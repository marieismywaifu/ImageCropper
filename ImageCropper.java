package imagecropper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.filechooser.FileFilter;

public class ImageCropper extends JFrame {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				ImageCropper cropper = new ImageCropper();
				cropper.setLocationRelativeTo(null);
				cropper.setVisible(true);
			}
		});
	}
	
	private final JFileChooser fch_open;
	private final JMenuBar bar;
	private final JMenuItem itm_close, itm_save;
	private final JPanel pnl_north;
	private final JSpinner nbr_width, nbr_height, nbr_zoom;
	private final JPanel pnl_center, pnl_south;
	private final JLabel lbl_offsets, lbl_names;
	
	private enum EdgeMode {
		MIRROR, SMEAR, LOOP, TRANSPARENCY
	}
	
	private boolean nightMode = true;
	private boolean unsavedChanges;
	private File currentDirectory;
	private EdgeMode edgeMode = EdgeMode.MIRROR;
	private boolean mouseDown;
	private int sourceWidth, sourceHeight, targetWidth, targetHeight, zoomFactor = 1, previewFactor = 1, x, y, mouseX, mouseY;
	private BufferedImage[] images;
	private BufferedImage previewImage;
	private String[] names;
	
	public ImageCropper() {
		// <editor-fold defaultstate="collapsed" desc="fch_open">
		fch_open = new JFileChooser();
		fch_open.setMultiSelectionEnabled(true);
		fch_open.setFileFilter(new FileFilter() {
			private final String[] suffixes = ImageIO.getReaderFileSuffixes();
			private final String description;
			
			{
				Arrays.parallelSort(suffixes);
				final StringBuilder builder = new StringBuilder();
				builder.append("all supported files (");
				if (suffixes.length > 0) builder.append("*.").append(suffixes[0]);
				for (int i = 1; i < suffixes.length - 1; i++) builder.append(", *.").append(suffixes[i]);
				if (suffixes.length > 1) builder.append(" and *.").append(suffixes[suffixes.length - 1]);
				description = builder.append(")").toString();
			}
			
			@Override public boolean accept(File file) {
				final String name = file.getName();
				final int i = name.lastIndexOf('.');
				return file.isDirectory() || (i >= 0 ? Arrays.binarySearch(suffixes, name.substring(i + 1)) >= 0 : false);
			}
			
			@Override public String getDescription() {
				return description;
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="bar">
		// <editor-fold defaultstate="collapsed" desc="mnu_file">
		// <editor-fold defaultstate="collapsed" desc="itm_open">
		final JMenuItem itm_open = new JMenuItem("open image(s) ...");
		itm_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		itm_open.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				cmd_open();
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_close">
		itm_close = new JMenuItem("close image(s)");
		itm_close.setEnabled(false);
		itm_close.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				cmd_close();
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_save">
		itm_save = new JMenuItem("save image(s)");
		itm_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		itm_save.setEnabled(false);
		itm_save.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				cmd_save();
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_exit">
		final JMenuItem itm_exit = new JMenuItem("exit");
		itm_exit.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				cmd_exit();
			}
		});
		// </editor-fold>
		
		final JMenu mnu_file = new JMenu("file");
		mnu_file.setMnemonic(KeyEvent.VK_F);
		mnu_file.add(itm_open);
		mnu_file.add(itm_close);
		mnu_file.add(itm_save);
		mnu_file.addSeparator();
		mnu_file.add(itm_exit);
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="mnu_edges">
		//<editor-fold defaultstate="collapsed" desc="itm_mirror">
		final JRadioButtonMenuItem itm_mirror = new JRadioButtonMenuItem("mirror");
		itm_mirror.setSelected(true);
		itm_mirror.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) cmd_mirror();
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="itm_smear">
		final JRadioButtonMenuItem itm_smear = new JRadioButtonMenuItem("smear");
		itm_smear.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) cmd_smear();
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="itm_loop">
		final JRadioButtonMenuItem itm_loop = new JRadioButtonMenuItem("loop");
		itm_loop.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) cmd_loop();
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="itm_transparency">
		final JRadioButtonMenuItem itm_transparency = new JRadioButtonMenuItem("transparency");
		itm_transparency.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) cmd_transparency();
			}
		});
		//</editor-fold>

		// <editor-fold defaultstate="collapsed" desc="grp_edges">
		final ButtonGroup grp_edges = new ButtonGroup();
		grp_edges.add(itm_mirror);
		grp_edges.add(itm_smear);
		grp_edges.add(itm_loop);
		grp_edges.add(itm_transparency);
		//</editor-fold>
		
		final JMenu mnu_edges = new JMenu("edges");
		mnu_edges.setMnemonic(KeyEvent.VK_E);
		mnu_edges.add(itm_mirror);
		mnu_edges.add(itm_smear);
		mnu_edges.add(itm_loop);
		mnu_edges.add(itm_transparency);
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="mnu_preview">
		// <editor-fold defaultstate="collapsed" desc="itm_nightmode">
		final JCheckBoxMenuItem itm_nightmode = new JCheckBoxMenuItem("night mode");
		itm_nightmode.setSelected(true);
		itm_nightmode.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) cmd_nightmode();
				else if (event.getStateChange() == ItemEvent.DESELECTED) cmd_daymode();
			}
		});
		// </editor-fold>
		
		final JMenu mnu_preview = new JMenu("preview");
		mnu_preview.setMnemonic(KeyEvent.VK_P);
		mnu_preview.add(itm_nightmode);
		// </editor-fold>
		
		bar = new JMenuBar();
		bar.add(mnu_file);
		bar.add(mnu_edges);
		bar.add(mnu_preview);
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="pnl_north">
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		targetWidth = screenSize.width;
		targetHeight = screenSize.height;
		if (targetWidth <= 0 || targetHeight <= 0) throw new Error("at least one screen resolution is negative or zero");
		
		// <editor-fold defaultstate="collapsed" desc="lbl_resolution">
		final JLabel lbl_resolution = new JLabel("resulution:");
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_width">
		nbr_width = new JSpinner(new SpinnerNumberModel(targetWidth, 1, null, 1));
		nbr_width.setEditor(new JSpinner.NumberEditor(nbr_width, "#"));
		nbr_width.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent event) {
				cmd_width();
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_height">
		nbr_height = new JSpinner(new SpinnerNumberModel(targetHeight, 1, null, 1));
		nbr_height.setEditor(new JSpinner.NumberEditor(nbr_height, "#"));
		nbr_height.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent event) {
				cmd_height();
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="lbl_zoom">
		final JLabel lbl_zoom = new JLabel("zoom:");
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_zoom">
		nbr_zoom = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		nbr_zoom.setEditor(new JSpinner.NumberEditor(nbr_zoom, "#"));
		nbr_zoom.setEnabled(false);
		nbr_zoom.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent event) {
				cmd_zoom();
			}
		});
		// </editor-fold>
		
		pnl_north = new JPanel();
		pnl_north.setLayout(new BoxLayout(pnl_north, BoxLayout.X_AXIS));
		pnl_north.add(Box.createHorizontalGlue());
		pnl_north.add(lbl_resolution);
		pnl_north.add(nbr_width);
		pnl_north.add(nbr_height);
		pnl_north.add(Box.createHorizontalGlue());
		pnl_north.add(lbl_zoom);
		pnl_north.add(nbr_zoom);
		pnl_north.add(Box.createHorizontalGlue());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="pnl_center">
		pnl_center = new JPanel() {
			@Override public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				graphics.setColor(nightMode ? new Color(0, 0, 0) : new Color(255, 255, 255));
				graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
				graphics.setColor(nightMode ? new Color(255, 255, 255) : new Color(0, 0, 0));
				graphics.fillRect(
					(this.getWidth() - targetWidth / previewFactor) / 2,
					(this.getHeight() - targetHeight / previewFactor) / 2,
					targetWidth / previewFactor,
					targetHeight / previewFactor
				);
				if (previewImage != null) graphics.drawImage(previewImage,

					// destination upper left corner
					(this.getWidth() - targetWidth / previewFactor) / 2, (this.getHeight() - targetHeight / previewFactor) / 2,

					// destination lower right corner
					(this.getWidth() + targetWidth / previewFactor) / 2, (this.getHeight() + targetHeight / previewFactor) / 2,

					// source upper left corner
					x + zoomFactor * targetWidth - 1, y + zoomFactor * targetHeight - 1,

					// source lower right corner
					x + 2 * targetWidth * zoomFactor - 1,  y + 2 * targetHeight * zoomFactor - 1,
				null);
			}
		};
		pnl_center.setPreferredSize(new Dimension(2 * targetWidth / 3, 2 * targetHeight / 3));
		pnl_center.addComponentListener(new ComponentListener() {
			@Override public void componentResized(ComponentEvent event) {
				if (event.getComponent() == pnl_center) cmd_resize();
			}
			
			@Override public void componentMoved(ComponentEvent event) { }
			@Override public void componentShown(ComponentEvent event) { }
			@Override public void componentHidden(ComponentEvent event) { }
		});
		pnl_center.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent event) { }

			@Override public void mousePressed(MouseEvent event) {
				if (event.getButton() == 1) cmd_mousedown();
			}

			@Override public void mouseReleased(MouseEvent event) {
				if (event.getButton() == 1) cmd_mouseUp();
			}

			@Override public void mouseEntered(MouseEvent event) { }
			@Override public void mouseExited(MouseEvent event) { }
		});
		pnl_center.addMouseMotionListener(new MouseMotionListener() {
			@Override public void mouseDragged(MouseEvent event) {
				cmd_mousemove(event);
			}

			@Override public void mouseMoved(MouseEvent event) {
				cmd_mousemove(event);
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="pnl_south">
		// <editor-fold defaultstate="collapsed" desc="lbl_offsets">
		lbl_offsets = new JLabel();
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="lbl_names">
		lbl_names = new JLabel("no images opened");
		// </editor-fold>
		
		pnl_south = new JPanel();
		pnl_south.setLayout(new BoxLayout(pnl_south, BoxLayout.X_AXIS));
		pnl_south.add(lbl_offsets);
		pnl_south.add(Box.createHorizontalGlue());
		pnl_south.add(lbl_names);
		// </editor-fold>
		
		initComponents();
	}
	
	private void initComponents() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setExtendedState(MAXIMIZED_BOTH);
		setJMenuBar(bar);
		setTitle("Image Cropper 2.0");
		
		addWindowListener(new WindowListener() {
			@Override public void windowOpened(WindowEvent e) { }
			
			@Override public void windowClosing(WindowEvent e) {
				cmd_exit();
			}
			
			@Override public void windowClosed(WindowEvent e) { }
			@Override public void windowIconified(WindowEvent e) { }
			@Override public void windowDeiconified(WindowEvent e) { }
			@Override public void windowActivated(WindowEvent e) { }
			@Override public void windowDeactivated(WindowEvent e) { }
		});
		
		add(pnl_north, BorderLayout.NORTH);
		add(pnl_center, BorderLayout.CENTER);
		add(pnl_south, BorderLayout.SOUTH);
		
		pack();
	}
	
	// <editor-fold defaultstate="collapsed" desc="cmd_open">
	private void cmd_open() {
		// show confirm dialog in case of unsaved changes
		if (unsavedChanges && JOptionPane.showConfirmDialog(
			this,
			"Opening another (set of) image(s) makes you lose unsaved changes. Do you want to proceed?",
			null,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		) != JOptionPane.OK_OPTION) return;
		
		// show file chooser
		if (fch_open.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
		
		// read files
		final File[] filesTemp = fch_open.getSelectedFiles();
		final BufferedImage[] imagesTemp = new BufferedImage[filesTemp.length];
		final String[] namesTemp = new String[filesTemp.length];
		for (int i = 0; i < filesTemp.length; i++) {
			final String name = filesTemp[i].getName();
			final int j = name.lastIndexOf('.');
			namesTemp[i] = j < 0 ? name : name.substring(0, j);
			try {
				imagesTemp[i] = ImageIO.read(filesTemp[i]);
			} catch (IOException exception) {
				JOptionPane.showMessageDialog(
					this,
					exception,
					"while reading \"" + name + "\" (" + (i + 1) + " / " + filesTemp.length + ")",
					JOptionPane.ERROR_MESSAGE
				);
				return;
			}
		}
		
		// analyze files
		final int sourceWidthTemp = imagesTemp[0].getWidth();
		final int sourceHeightTemp = imagesTemp[0].getHeight();
		final StringBuilder builder = new StringBuilder();
		builder.append(namesTemp[0]);
		for (int i = 1; i < filesTemp.length; i++) {
			if (imagesTemp[i].getWidth() != sourceWidthTemp || imagesTemp[i].getHeight() != sourceHeightTemp) {
				JOptionPane.showMessageDialog(this, "the images don't all have the same resolution", null, JOptionPane.ERROR_MESSAGE);
				return;
			}
			builder.append(i < filesTemp.length - 1 ? ", " : " and ").append(namesTemp[i]);
		}
		builder.append(" opened");
		
		// update UI
		itm_close.setEnabled(true);
		itm_close.setText("close image" + (imagesTemp.length > 1 ? "s" : ""));
		itm_save.setEnabled(true);
		itm_save.setText("save image" + (imagesTemp.length > 1 ? "s" : ""));
		for (zoomFactor = 1; ((targetWidth * (zoomFactor + 1)) <= sourceWidthTemp) && ((targetHeight * (zoomFactor + 1)) <= sourceHeightTemp); zoomFactor++);
		nbr_zoom.setValue(zoomFactor);
		nbr_zoom.setEnabled(true);
		lbl_names.setText(builder.toString());
		
		// update other variables
		unsavedChanges = true;
		currentDirectory = fch_open.getCurrentDirectory();
		images = imagesTemp;
		names = namesTemp;
		sourceWidth = sourceWidthTemp;
		sourceHeight = sourceHeightTemp;
		x = 0;
		y = 0;
		
		// continue execution
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_close">
	private void cmd_close() {
		// show confirm dialog in case of unsaved changes
		if (unsavedChanges && JOptionPane.showConfirmDialog(
			this,
			"Closing th" + (images.length > 1 ? "ese images" : "is image") + " will make you lose unsaved changes. Do you want to proceed?",
			null,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		) != JOptionPane.OK_OPTION) return;
		
		// update UI
		itm_close.setEnabled(false);
		itm_close.setText("close image(s)");
		itm_save.setEnabled(false);
		itm_save.setText("save image(s)");
		nbr_zoom.setEnabled(false);
		nbr_zoom.setValue(1);
		lbl_offsets.setText(null);
		lbl_names.setText("no image(s) opened");
		
		// update other variables
		unsavedChanges = false;
		currentDirectory = null;
		images = null;
		previewImage = null;
		names = null;
		sourceWidth = 0;
		sourceHeight = 0;
		x = 0;
		y = 0;
		
		// continue execution
		cmd_paint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_save">
	private void cmd_save() {
		for (int i = 0; i < images.length; i++) {
			BufferedImage image = new BufferedImage(zoomFactor * targetWidth, zoomFactor * targetHeight, BufferedImage.TYPE_INT_ARGB);
			for (int setX = 0; setX < image.getWidth(); setX++) for (int setY = 0; setY < image.getHeight(); setY++) {
				final int getX = x + setX, getY = y + setY, rgb;
				if (getX >= 0 && getX < sourceWidth && getY >= 0 && getY < sourceHeight) rgb = images[i].getRGB(getX, getY); else switch (edgeMode) {
					case MIRROR:
						rgb = images[i].getRGB(
							// f(x) = |((x + w - 2) mod (2w - 2)) - w + 2|
							Math.abs(((((getX + sourceWidth - 2) % (sourceWidth * 2 - 2)) + (sourceWidth * 2 - 2)) % (sourceWidth * 2 - 2)) - sourceWidth + 2),
							Math.abs(((((getY + sourceHeight - 2) % (sourceHeight * 2 - 2)) + (sourceHeight * 2 - 2)) % (sourceHeight * 2 - 2)) - sourceHeight + 2)
						);
						break;
					case SMEAR:
						rgb = images[i].getRGB(Math.max(Math.min(getX, sourceWidth - 1), 0), Math.max(Math.min(getY, sourceHeight - 1), 0));
						break;
					case LOOP:
						rgb = images[i].getRGB(((getX % sourceWidth) + sourceWidth) % sourceWidth, ((getY % sourceHeight) + sourceHeight) % sourceHeight);
						break;
					case TRANSPARENCY:
						rgb = 0x00_00_00_00; // AA RR GG BB
						break;
					default:
						throw new Error("unsupported edge mode");
				}
				image.setRGB(setX, setY, rgb);
			}
			String name = names[i] + " (" + zoomFactor * targetWidth + " × " + zoomFactor * targetHeight + ").png";
			try {
				ImageIO.write(image, "PNG", new File(currentDirectory, name));
			} catch (IOException exception) {
				JOptionPane.showMessageDialog(
					this,
					exception,
					"while writing \"" + name + "\" (" + (i + 1) + " / " + images.length + ")",
					JOptionPane.ERROR_MESSAGE
				);
			}
		}
		unsavedChanges = false;
		JOptionPane.showMessageDialog(this, "done", null, JOptionPane.INFORMATION_MESSAGE);
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_exit">
	private void cmd_exit() {
		if (unsavedChanges && JOptionPane.showConfirmDialog(
			this,
			"Exiting Image Cropper 2.0 will make you lose unsaved changes. Do you want to proceed?",
			null,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		) != JOptionPane.OK_OPTION) return;
		dispose();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_mirror">
	private void cmd_mirror() {
		if (edgeMode == EdgeMode.MIRROR) return;
		edgeMode = EdgeMode.MIRROR;
		if (previewImage != null) unsavedChanges = true;
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_smear">
	private void cmd_smear() {
		if (edgeMode == EdgeMode.SMEAR) return;
		edgeMode = EdgeMode.SMEAR;
		if (previewImage != null) unsavedChanges = true;
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_loop">
	private void cmd_loop() {
		if (edgeMode == EdgeMode.LOOP) return;
		edgeMode = EdgeMode.LOOP;
		if (previewImage != null) unsavedChanges = true;
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_transparency">
	private void cmd_transparency() {
		if (edgeMode == EdgeMode.TRANSPARENCY) return;
		edgeMode = EdgeMode.TRANSPARENCY;
		if (previewImage != null) unsavedChanges = true;
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_nightmode">
	private void cmd_nightmode() {
		nightMode = true;
		cmd_paint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_daymode">
	private void cmd_daymode() {
		nightMode = false;
		cmd_paint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_width">
	private void cmd_width() {
		final int targetWidthTemp = (Integer) nbr_width.getValue();
		if (targetWidth == targetWidthTemp) return;
		targetWidth = targetWidthTemp;
		if (previewImage != null) unsavedChanges = true;
		cmd_resize();
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_height">
	private void cmd_height() {
		final int targetHeightTemp = (Integer) nbr_height.getValue();
		if (targetHeight == targetHeightTemp) return;
		targetHeight = (Integer) nbr_height.getValue();
		if (previewImage != null) unsavedChanges = true;
		cmd_resize();
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_zoom">
	private void cmd_zoom() {
		final int zoomFactorTemp = (Integer) nbr_zoom.getValue();
		if (zoomFactor == zoomFactorTemp) return;
		zoomFactor = zoomFactorTemp;
		if (previewImage != null) unsavedChanges = true;
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_resize">
	private void cmd_resize() {
		for (previewFactor = 1; ((targetWidth / previewFactor) > pnl_center.getWidth()) || ((targetHeight / previewFactor) > pnl_center.getHeight()); previewFactor++);
		cmd_paint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_mousedown">
	private void cmd_mousedown() {
		mouseDown = true;
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_mouseup">
	private void cmd_mouseUp() {
		mouseDown = false;
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_mousemove">
	private void cmd_mousemove(MouseEvent event) {
		int currX = event.getX(), currY = event.getY();
		if (mouseDown) {
			x += mouseX - currX;
			y += mouseY - currY;
			if (x < 0 - zoomFactor * targetWidth + 1) x = 0 - zoomFactor * targetWidth + 1;
			else if (x >= sourceWidth) x = sourceWidth - 1;
			if (y < 0 - zoomFactor * targetHeight + 1) y = 0 - zoomFactor * targetHeight + 1;
			else if (y >= sourceHeight) y = sourceHeight - 1;
			cmd_paint();
		}
		mouseX = currX;
		mouseY = currY;
	}
	// </editor-fold>
	
	private void reRenderPreviewImage() {
		if (images == null) return;
		
		// draw the source image directly
		previewImage = new BufferedImage(2 * (zoomFactor * targetWidth - 1) + sourceWidth, 2 * (zoomFactor * targetHeight - 1) + sourceHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = previewImage.createGraphics();
		graphics.drawImage(images[0], zoomFactor * targetWidth - 1, zoomFactor * targetHeight - 1, null);
		graphics.dispose();
		
		// draw the edges in background
		new Thread() {
			@Override public void run() {
				for (int previewX = 0 - zoomFactor * targetWidth + 1; previewX < sourceWidth + zoomFactor * targetWidth - 1; previewX++) {
					for (int previewY = 0 - zoomFactor * targetHeight + 1; previewY < sourceHeight + zoomFactor * targetHeight - 1; previewY++) {
						if (previewX <= 0 || previewX > sourceWidth || previewY <= 0 || previewY > sourceHeight) {
							final int rgb;
							switch (edgeMode) {
								case MIRROR:
									rgb = images[0].getRGB(
										Math.abs(((((previewX + sourceWidth - 2) % (sourceWidth * 2 - 2)) + (sourceWidth * 2 - 2)) % (sourceWidth * 2 - 2)) - sourceWidth + 2),
										Math.abs(((((previewY + sourceHeight - 2) % (sourceHeight * 2 - 2)) + (sourceHeight * 2 - 2)) % (sourceHeight * 2 - 2)) - sourceHeight + 2)
									);
									break;
								case SMEAR:
									rgb = images[0].getRGB(Math.max(Math.min(previewX, sourceWidth - 1), 0), Math.max(Math.min(previewY, sourceHeight - 1), 0));
									break;
								case LOOP:
									rgb = images[0].getRGB(((previewX % sourceWidth) + sourceWidth) % sourceWidth, ((previewY % sourceHeight) + sourceHeight) % sourceHeight);
									break;
								case TRANSPARENCY:
									rgb = 0x00_00_00_00;
									break;
								default:
									throw new Error("unsupported edge mode");
							}
							previewImage.setRGB(previewX + zoomFactor * targetWidth - 1, previewY + zoomFactor * targetHeight - 1, rgb);
						}
					}
				}
				cmd_paint();
			}
		}.start();
		
		cmd_paint();
	}
	
	// <editor-fold defaultstate="collapsed" desc="cmd_paint">
	private void cmd_paint() {
		pnl_center.repaint();
		if (previewImage != null) lbl_offsets.setText(x + " " + y + " | " + (sourceWidth - zoomFactor * targetWidth - x) + " " + (sourceHeight - zoomFactor * targetHeight - y));
	}
	// </editor-fold>
}
