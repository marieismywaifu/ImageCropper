package imagecropper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
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

import javax.swing.filechooser.FileFilter;

public class ImageCropper extends JFrame {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ImageCropper cropper = new ImageCropper();
			cropper.setLocationRelativeTo(null);
			cropper.setVisible(true);
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
	private BufferedImage sourceImage, previewImage;
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
				builder.append("all supported files (*.").append(suffixes[0]);
				for (int i = 1; i < suffixes.length - 1; i++) builder.append(", *.").append(suffixes[i]);
				if (suffixes.length > 1) builder.append(" and *.").append(suffixes[suffixes.length - 1]);
				description = builder.append(")").toString();
			}
			
			@Override public boolean accept(File file) {
				final String name = file.getName();
				final int i = name.lastIndexOf('.');
				return file.isDirectory() || (i > 0 && Arrays.binarySearch(suffixes, name.substring(i + 1)) >= 0);
			}
			
			@Override public String getDescription() {
				return description;
			}
		});
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="bar">
		// <editor-fold defaultstate="collapsed" desc="mnu_file">
		// <editor-fold defaultstate="collapsed" desc="itm_open">
		final JMenuItem itm_open = new JMenuItem("open image(s) \u2026");
		itm_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		itm_open.addActionListener(event -> cmd_open());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_close">
		itm_close = new JMenuItem("close image(s)");
		itm_close.setEnabled(false);
		itm_close.addActionListener(event -> cmd_close());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_save">
		itm_save = new JMenuItem("save image(s)");
		itm_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		itm_save.setEnabled(false);
		itm_save.addActionListener(event -> cmd_save());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="itm_exit">
		final JMenuItem itm_exit = new JMenuItem("exit");
		itm_exit.addActionListener(event -> itm_exit());
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
		// <editor-fold defaultstate="collapsed" desc="itm_mirror">
		final JRadioButtonMenuItem itm_mirror = new JRadioButtonMenuItem("mirror");
		itm_mirror.setSelected(true);
		itm_mirror.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_mirror();
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_smear">
		final JRadioButtonMenuItem itm_smear = new JRadioButtonMenuItem("smear");
		itm_smear.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_smear();
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_loop">
		final JRadioButtonMenuItem itm_loop = new JRadioButtonMenuItem("loop");
		itm_loop.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_loop();
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_transparency">
		final JRadioButtonMenuItem itm_transparency = new JRadioButtonMenuItem("transparency");
		itm_transparency.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_transparency();
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="grp_edges">
		final ButtonGroup grp_edges = new ButtonGroup();
		grp_edges.add(itm_mirror);
		grp_edges.add(itm_smear);
		grp_edges.add(itm_loop);
		grp_edges.add(itm_transparency);
		// </editor-fold>
		
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
		itm_nightmode.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_nightmode();
			else if (event.getStateChange() == ItemEvent.DESELECTED) cmd_daymode();
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
		nbr_width.addChangeListener(event -> cmd_width());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_height">
		nbr_height = new JSpinner(new SpinnerNumberModel(targetHeight, 1, null, 1));
		nbr_height.setEditor(new JSpinner.NumberEditor(nbr_height, "#"));
		nbr_height.addChangeListener(event -> cmd_height());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="lbl_zoom">
		final JLabel lbl_zoom = new JLabel("zoom:");
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_zoom">
		nbr_zoom = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		nbr_zoom.setEditor(new JSpinner.NumberEditor(nbr_zoom, "#"));
		nbr_zoom.setEnabled(false);
		nbr_zoom.addChangeListener(event -> cmd_zoom());
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
				final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
				if (previewImage != null) graphics.drawImage(previewImage,

					// destination upper left corner
					(this.getWidth() - targetWidth / previewFactor) / 2, (this.getHeight() - targetHeight / previewFactor) / 2,

					// destination lower right corner
					(this.getWidth() + targetWidth / previewFactor) / 2, (this.getHeight() + targetHeight / previewFactor) / 2,

					// source upper left corner
					x + addLeftRight, y + addUpDown,

					// source lower right corner
					x + addLeftRight + targetWidth * zoomFactor,  y + addUpDown + targetHeight * zoomFactor,
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
		final BufferedImage sourceImageTemp;
		final String[] namesTemp = new String[filesTemp.length];
		final int sourceWidthTemp, sourceHeightTemp;
		final StringBuilder builder = new StringBuilder();
		try {
			sourceImageTemp = ImageIO.read(filesTemp[0]);
			String name = filesTemp[0].getName();
			namesTemp[0] = name;
			int j = name.lastIndexOf('.');
			builder.append('"').append(j > 0 ? name.substring(0, j) : name).append('"');
			sourceWidthTemp = sourceImageTemp.getWidth();
			sourceHeightTemp = sourceImageTemp.getHeight();
			for (int i = 1; i < filesTemp.length; i++) {
				BufferedImage imageTemp = ImageIO.read(filesTemp[i]);
				if (imageTemp.getWidth() != sourceWidthTemp && imageTemp.getHeight() != sourceHeightTemp) {
					JOptionPane.showMessageDialog(this, "the images don't all have the same resolution", null, JOptionPane.ERROR_MESSAGE);
					return;
				}
				name = filesTemp[i].getName();
				namesTemp[i] = name;
				j = name.lastIndexOf('.');
				builder.append(i < filesTemp.length - 1 ? ", " : " and ").append('"').append(j > 0 ? name.substring(0, j) : name).append('"');
			}
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this, exception, null, JOptionPane.ERROR_MESSAGE);
			return;
		}
		builder.append(" opened");
		
		// update UI
		itm_close.setEnabled(true);
		itm_close.setText("close image" + (filesTemp.length > 1 ? "s" : ""));
		itm_save.setEnabled(true);
		itm_save.setText("save image" + (filesTemp.length > 1 ? "s" : ""));
		for (zoomFactor = 1; ((targetWidth * (zoomFactor + 1)) <= sourceWidthTemp) && ((targetHeight * (zoomFactor + 1)) <= sourceHeightTemp); zoomFactor++);
		nbr_zoom.setValue(zoomFactor);
		nbr_zoom.setEnabled(true);
		lbl_names.setText(builder.toString());
		
		// update other variables
		unsavedChanges = true;
		currentDirectory = fch_open.getCurrentDirectory();
		sourceImage = sourceImageTemp;
		names = namesTemp;
		sourceWidth = sourceWidthTemp;
		sourceHeight = sourceHeightTemp;
		x = (sourceWidth - zoomFactor * targetWidth) / 2;
		y = (sourceHeight - zoomFactor * targetHeight) / 2;
		
		// continue execution
		reRenderPreviewImage();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_close">
	private void cmd_close() {
		// show confirm dialog in case of unsaved changes
		if (unsavedChanges && JOptionPane.showConfirmDialog(
			this,
			"Closing th" + (names.length > 1 ? "ese images" : "is image") + " will make you lose unsaved changes. Do you want to proceed?",
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
		sourceImage = null;
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
		try {
			for (int i = 0; i < names.length; i++) {
				final BufferedImage targetImage = new BufferedImage(zoomFactor * targetWidth, zoomFactor * targetHeight, BufferedImage.TYPE_INT_ARGB);
				if (edgeMode == EdgeMode.TRANSPARENCY) for (int smartX = Math.max(0, x); smartX < Math.min(zoomFactor * targetWidth, sourceWidth); smartX++) for (int smartY = Math.max(0, y); smartY < Math.min(zoomFactor * targetHeight, sourceHeight); smartY++) targetImage.setRGB(smartX - x, smartY - y, sourceImage.getRGB(smartX, smartY));
				else for (int getX = x; getX < x + zoomFactor * targetWidth; getX++) for (int getY = y; getY < y + zoomFactor * targetHeight; getY++) targetImage.setRGB(getX - x, getY - y, switch (edgeMode) {
					case MIRROR -> sourceImage.getRGB(
						// f(x) = |((x + w - 2) mod (2w - 2)) - w + 2|
						Math.abs(((((getX + sourceWidth - 2) % (sourceWidth * 2 - 2)) + (sourceWidth * 2 - 2)) % (sourceWidth * 2 - 2)) - sourceWidth + 2),
						Math.abs(((((getY + sourceHeight - 2) % (sourceHeight * 2 - 2)) + (sourceHeight * 2 - 2)) % (sourceHeight * 2 - 2)) - sourceHeight + 2)
					);
					case SMEAR -> sourceImage.getRGB(Math.max(Math.min(getX, sourceWidth - 1), 0), Math.max(Math.min(getY, sourceHeight - 1), 0));
					case LOOP -> sourceImage.getRGB(((getX % sourceWidth) + sourceWidth) % sourceWidth, ((getY % sourceHeight) + sourceHeight) % sourceHeight);
					default -> throw new Error("unsupported edge mode");
				});
				final int j = names[i].lastIndexOf('.');
				final String name = j > 0 ? names[i].substring(0, j) : names[i];
				ImageIO.write(targetImage, "PNG", new File(currentDirectory, name + " (" + zoomFactor * targetWidth + " \u00D7 " + zoomFactor * targetHeight + ").png"));
				if (names.length > 1) sourceImage = ImageIO.read(new File(currentDirectory, names[(i + 1) % names.length]));
			}
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this, exception, null, JOptionPane.ERROR_MESSAGE);
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
		final int zoomChange = ((Integer) nbr_zoom.getValue()) - zoomFactor;
		if (zoomChange == 0) return;
		zoomFactor += zoomChange;
		x -= zoomChange * targetWidth / 2;
		y -= zoomChange * targetHeight / 2;
		final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
		if (x < -addLeftRight) x = -addLeftRight;
		else if (x + zoomFactor * targetWidth > sourceWidth + addLeftRight) x = sourceWidth - zoomFactor * targetWidth + addLeftRight;
		if (y < -addUpDown) y = -addUpDown;
		else if (y + zoomFactor * targetHeight > sourceHeight + addUpDown) y = sourceHeight - zoomFactor * targetHeight + addUpDown;
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
			final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
			if (x < -addLeftRight) x = -addLeftRight;
			else if (x + zoomFactor * targetWidth > sourceWidth + addLeftRight) x = sourceWidth - zoomFactor * targetWidth + addLeftRight;
			if (y < -addUpDown) y = -addUpDown;
			else if (y + zoomFactor * targetHeight > sourceHeight + addUpDown) y = sourceHeight - zoomFactor * targetHeight + addUpDown;
			cmd_paint();
		}
		mouseX = currX;
		mouseY = currY;
	}
	// </editor-fold>
	
	private void reRenderPreviewImage() {
		if (sourceImage == null) return;
		
		final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
		previewImage = new BufferedImage(sourceWidth + 2 * addLeftRight, sourceHeight + 2 * addUpDown, BufferedImage.TYPE_INT_ARGB);
		new Thread(() -> {
			if (edgeMode == EdgeMode.TRANSPARENCY) for (int smartX = 0; smartX < sourceWidth; smartX++) for (int smartY = 0; smartY < sourceHeight; smartY++) previewImage.setRGB(smartX + addLeftRight, smartY + addUpDown, sourceImage.getRGB(smartX, smartY));
			else for (int getX = -addLeftRight; getX < sourceWidth + addLeftRight; getX++) for (int getY = -addUpDown; getY < sourceHeight + addUpDown; getY++) previewImage.setRGB(getX + addLeftRight, getY + addUpDown, switch (edgeMode) {
				case MIRROR -> sourceImage.getRGB(
					Math.abs(((((getX + sourceWidth - 2) % (sourceWidth * 2 - 2)) + (sourceWidth * 2 - 2)) % (sourceWidth * 2 - 2)) - sourceWidth + 2),
					Math.abs(((((getY + sourceHeight - 2) % (sourceHeight * 2 - 2)) + (sourceHeight * 2 - 2)) % (sourceHeight * 2 - 2)) - sourceHeight + 2)
				);
				case SMEAR -> sourceImage.getRGB(Math.max(Math.min(getX, sourceWidth - 1), 0), Math.max(Math.min(getY, sourceHeight - 1), 0));
				case LOOP -> sourceImage.getRGB(((getX % sourceWidth) + sourceWidth) % sourceWidth, ((getY % sourceHeight) + sourceHeight) % sourceHeight);
				default -> throw new Error("unsupported edge mode");
			});
			cmd_paint();
		}).start();
	}
	
	// <editor-fold defaultstate="collapsed" desc="cmd_paint">
	private void cmd_paint() {
		pnl_center.repaint();
		if (previewImage != null) lbl_offsets.setText(x + " " + y + " | " + (sourceWidth - zoomFactor * targetWidth - x) + " " + (sourceHeight - zoomFactor * targetHeight - y));
	}
	// </editor-fold>
}
