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
import java.util.Locale;

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
			final File[] files = new File[args.length];
			for (int i = 0; i < args.length; i++) files[i] = new File(args[i]);
			
			ImageCropper cropper = new ImageCropper(files);
			cropper.setLocationRelativeTo(null);
			cropper.setVisible(true);
		});
	}
	
	private static String removeFileExtension(String fileName) {
		int i = fileName.lastIndexOf('.');
		return i > 0 ? fileName.substring(0, i) : fileName;
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
	private EdgeMode edgeMode = EdgeMode.MIRROR;
	private boolean mouseDown;
	private int sourceWidth, sourceHeight, targetWidth, targetHeight, zoomFactor = 1, previewFactor = 1, x, y, mouseX, mouseY;
	private BufferedImage sourceImage, previewImage;
	private File[] files;
	
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
				final String name = file.getName().toLowerCase(Locele.ROOT);
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
		itm_open.addActionListener(event -> cmd_choose());
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
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_edge_mode(EdgeMode.MIRROR);
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_smear">
		final JRadioButtonMenuItem itm_smear = new JRadioButtonMenuItem("smear");
		itm_smear.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_edge_mode(EdgeMode.SMEAR);
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_loop">
		final JRadioButtonMenuItem itm_loop = new JRadioButtonMenuItem("loop");
		itm_loop.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_edge_mode(EdgeMode.LOOP);
		});
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="itm_transparency">
		final JRadioButtonMenuItem itm_transparency = new JRadioButtonMenuItem("transparency");
		itm_transparency.addItemListener((event) -> {
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_edge_mode(EdgeMode.TRANSPARENCY);
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
			if (event.getStateChange() == ItemEvent.SELECTED) cmd_nightmode(true);
			else if (event.getStateChange() == ItemEvent.DESELECTED) cmd_nightmode(false);
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
		nbr_width.addChangeListener(event -> cmd_target_size());
		// </editor-fold>
		
		// <editor-fold defaultstate="collapsed" desc="nbr_height">
		nbr_height = new JSpinner(new SpinnerNumberModel(targetHeight, 1, null, 1));
		nbr_height.setEditor(new JSpinner.NumberEditor(nbr_height, "#"));
		nbr_height.addChangeListener(event -> cmd_target_size());
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
				if (event.getComponent() == pnl_center) cmd_preview_size();
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
	
	public ImageCropper(File[] files) {
		this ();
		
		open(files);
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
	
	private void updateOffsets() {
		lbl_offsets.setText(x + " " + y + " | " + (sourceWidth - zoomFactor * targetWidth - x) + " " + (sourceHeight - zoomFactor * targetHeight - y));
	}
	
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
			pnl_center.repaint();
		}).start();
	}
	
	private void open(File[] files) {
		if (files == null || files.length <= 0) return;
		
		final BufferedImage sourceImageTemp;
		final int sourceWidthTemp, sourceHeightTemp;
		final StringBuilder namesTemp = new StringBuilder();
		
		try {
			sourceImageTemp = ImageIO.read(files[0]);
			namesTemp.append('"').append(removeFileExtension(files[0].getName())).append('"');
			sourceWidthTemp = sourceImageTemp.getWidth();
			sourceHeightTemp = sourceImageTemp.getHeight();
			
			for (int i = 1; i < files.length; i++) {
				final BufferedImage imageTemp = ImageIO.read(files[i]);
				if (imageTemp.getWidth() != sourceWidthTemp || imageTemp.getHeight() != sourceHeightTemp) {
					JOptionPane.showMessageDialog(this, "the images don't all have the same resolution", null, JOptionPane.ERROR_MESSAGE);
					return;
				}
				namesTemp.append(i < files.length - 1 ? ", \"" : " and \"").append(removeFileExtension(files[i].getName())).append('"');
			}
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this, exception, null, JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		setTitle("cropping " + namesTemp + " \u2013 ImageCropper 2.0");
		itm_close.setEnabled(true);
		itm_close.setText("close image" + (files.length > 1 ? "s" : ""));
		itm_save.setEnabled(true);
		itm_save.setText("save image" + (files.length > 1 ? "s" : ""));
		for (zoomFactor = 1; ((targetWidth * (zoomFactor + 1)) <= sourceWidthTemp) && ((targetHeight * (zoomFactor + 1)) <= sourceHeightTemp); zoomFactor++);
		nbr_zoom.setValue(zoomFactor);
		nbr_zoom.setEnabled(true);
		lbl_names.setText(namesTemp + " opened");
		
		unsavedChanges = true;
		sourceImage = sourceImageTemp;
		this.files = files;
		sourceWidth = sourceWidthTemp;
		sourceHeight = sourceHeightTemp;
		x = (sourceWidth - zoomFactor * targetWidth) / 2;
		y = (sourceHeight - zoomFactor * targetHeight) / 2;
		
		updateOffsets();
		reRenderPreviewImage();
	}
	
	// <editor-fold defaultstate="collapsed" desc="cmd_choose">
	private void cmd_choose() {
		// show confirm dialog in case of unsaved changes
		if (!unsavedChanges || JOptionPane.showConfirmDialog(
			this,
			"Opening another (set of) image(s) makes you lose unsaved changes. Do you want to proceed?",
			null,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		) == JOptionPane.OK_OPTION) if (fch_open.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) open(fch_open.getSelectedFiles());
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_close">
	private void cmd_close() {
		// show confirm dialog in case of unsaved changes
		if (unsavedChanges && JOptionPane.showConfirmDialog(
			this,
			"Closing th" + (paths.length > 1 ? "ese images" : "is image") + " will make you lose unsaved changes. Do you want to proceed?",
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
		sourceImage = null;
		previewImage = null;
		files = null;
		sourceWidth = 0;
		sourceHeight = 0;
		x = 0;
		y = 0;
		
		// continue execution
		updateOffsets();
		pnl_center.repaint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_save">
	private void cmd_save() {
		try {
			for (int i = 0; i < files.length; i++) {
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
				ImageIO.write(targetImage, "PNG", new File(files[i].getParentFile(); removeFileExtension(files[i].getName()) + " (" + zoomFactor * targetWidth + " \u00D7 " + zoomFactor * targetHeight + ").png"));
				if (i < files.length - 1) sourceImage = ImageIO.read(files[(i + 1) % files.length]);
			}
			unsavedChanges = false;
			JOptionPane.showMessageDialog(this, "done", null, JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this, exception, null, JOptionPane.ERROR_MESSAGE);
		} finally {
			if (files.length > 1) try {
				sourceImage = ImageIO.read(files[0]);
			} catch (IOException exception) {
				JOptionPane.showMessageDialog(this, exception, null, JOptionPane.ERROR_MESSAGE);
				cmd_close();
			}
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_exit">
	private void cmd_exit() {
		if (!unsavedChanges || JOptionPane.showConfirmDialog(
			this,
			"Exiting Image Cropper 2.0 will make you lose unsaved changes. Do you want to proceed?",
			null,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		) == JOptionPane.OK_OPTION) dispose();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_edge_mode">
	private void cmd_edge_mode(EdgeMode edgeModeTemp) {
		if (edgeMode != EdgeMode.MIRROR) {
			edgeMode = edgeModeTemp;
			if (previewImage != null) {
				unsavedChanges = true;
				reRenderPreviewImage();
			}
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_nightmode">
	private void cmd_nightmode(boolean nightModeTemp) {
		if (nightMode != nightModeTemp) {
			nightMode = nightModeTemp;
			pnl_center.repaint();
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_preview_size">
	private void cmd_preview_size() {
		for (previewFactor = 1; ((targetWidth / previewFactor) > pnl_center.getWidth()) || ((targetHeight / previewFactor) > pnl_center.getHeight()); previewFactor++);
		pnl_center.repaint();
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_target_size">
	private void cmd_target_size() {
		final int changeHoriz = (Integer) nbr_width.getValue() - zoomFactor * targetWidth, changeVert = (Integer) nbr_height.getValue() - zoomFactor * targetHeight;
		if (changeHoriz != 0 || changeVert != 0) {
			targetWidth += changeHoriz;
			targetHeight += changeVert;
			cmd_preview_size();
			if (previewImage != null) {
				x -= zoomFactor * changeHoriz / 2 + (x % 2 != 0 ? (zoomFactor * changeHoriz) % 2 : 0);
				y -= zoomFactor * changeVert / 2 + (y % 2 != 0 ? (zoomFactor * changeVert) % 2 : 0);
				unsavedChanges = true;
				updateOffsets();
				reRenderPreviewImage();
			}
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="cmd_zoom">
	private void cmd_zoom() {
		final int zoomChange = ((Integer) nbr_zoom.getValue()) - zoomFactor;
		if (zoomChange != 0) {
			zoomFactor += zoomChange;
			if (previewImage != null) {
				x -= zoomChange * targetWidth / 2;
				y -= zoomChange * targetHeight / 2;
				final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
				if (x < -addLeftRight) x = -addLeftRight;
				else if (x + zoomFactor * targetWidth > sourceWidth + addLeftRight) x = sourceWidth - zoomFactor * targetWidth + addLeftRight;
				if (y < -addUpDown) y = -addUpDown;
				else if (y + zoomFactor * targetHeight > sourceHeight + addUpDown) y = sourceHeight - zoomFactor * targetHeight + addUpDown;
				updateOffsets();
				reRenderPreviewImage();
			}
		}
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
		if (mouseDown && previewImage != null) {
			x += mouseX - currX;
			y += mouseY - currY;
			final int addLeftRight = Math.max(0, zoomFactor * targetWidth - sourceWidth), addUpDown = Math.max(0, zoomFactor * targetHeight - sourceHeight);
			if (x < -addLeftRight) x = -addLeftRight;
			else if (x + zoomFactor * targetWidth > sourceWidth + addLeftRight) x = sourceWidth - zoomFactor * targetWidth + addLeftRight;
			if (y < -addUpDown) y = -addUpDown;
			else if (y + zoomFactor * targetHeight > sourceHeight + addUpDown) y = sourceHeight - zoomFactor * targetHeight + addUpDown;
			updateOffsets();
			pnl_center.repaint();
		}
		mouseX = currX;
		mouseY = currY;
	}
	// </editor-fold>
}
