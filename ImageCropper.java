package imagecropper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public class ImageCropper {
	private static final Dimension MAXIMUM_SIZE = new Dimension(1600, 900);
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				ImageCropper cropper = new ImageCropper();
			}
		});
	}
	
	private final JFileChooser jfc;
	private final JFrame window;
	private final JPanel preview, options;
	private final JButton open, save;
	private final JTextField width, height;
	private final JLabel status;
	private final int maxPreviewWidth, maxPreviewHeight;
	
	private boolean mouseDown;
	private int imageWidth, imageHeight, inputWidth = 1920, inputHeight = 1080, cropFactor, previewFactor, x, y, mouseX, mouseY;
	private BufferedImage[] images;
	private String[] names;
	
	public ImageCropper() {
		jfc = new JFileChooser();
		window = new JFrame();
		options = new JPanel();
		open = new JButton();
		width = new JTextField();
		height = new JTextField();
		save = new JButton();
		preview = new JPanel() {
			@Override public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				if (images != null) {
					graphics.drawImage(images[0],
						0, 0, // destination (x1, y1)
						inputWidth / previewFactor, inputHeight / previewFactor, // destination (x2, y2)
						x, y, // source (x1, y1)
						inputWidth * cropFactor + x, inputHeight * cropFactor + y, // source (x2, y2)
					null);
					status.setText(x + " " + y + " | " + (imageWidth - x - inputWidth * cropFactor) + " " + (imageHeight - y - inputHeight * cropFactor));
				}
			}
		};
		status = new JLabel();
		
		initComponents();
		
		Dimension maxPreviewSize = preview.getMaximumSize();
		maxPreviewWidth = maxPreviewSize.width;
		maxPreviewHeight = maxPreviewSize.height;
	}
	
	// <editor-fold defaultstate="collapsed" desc="initComponents()">
	private void initComponents() {
		jfc.setMultiSelectionEnabled(true);
		
		open.setText("open image(s)");
		open.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				handleOpen(event);
			}
		});
		options.add(open);
		
		width.setText("1920");
		width.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				window.requestFocus();
			}
		});
		width.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void changedUpdate(DocumentEvent event) {
				handleWidth(event);
			}
			
			@Override public void removeUpdate(DocumentEvent event) {
				handleWidth(event);
			}
			
			@Override public void insertUpdate(DocumentEvent event) {
				handleWidth(event);
			}
		});
		options.add(width);
		
		height.setText("1080");
		height.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				window.requestFocus();
			}
		});
		height.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void changedUpdate(DocumentEvent event) {
				handleHeight(event);
			}
			
			@Override public void removeUpdate(DocumentEvent event) {
				handleHeight(event);
			}
			
			@Override public void insertUpdate(DocumentEvent event) {
				handleHeight(event);
			}
		});
		options.add(height);
		
		save.setText("save image(s)");
		save.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				handleSave(event);
			}
		});
		options.add(save);
		
		window.add(options, BorderLayout.NORTH);
		
		preview.setMinimumSize(MAXIMUM_SIZE);
		preview.setPreferredSize(MAXIMUM_SIZE);
		preview.setMaximumSize(MAXIMUM_SIZE);
		preview.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent event) { }

			@Override public void mousePressed(MouseEvent event) {
				if (event.getButton() == 1) {
					mouseDown = true;
				}
			}

			@Override public void mouseReleased(MouseEvent event) {
				if (event.getButton() == 1) {
					mouseDown = false;
				}
			}

			@Override public void mouseEntered(MouseEvent event) { }
			@Override public void mouseExited(MouseEvent event) { }
		});
		preview.addMouseMotionListener(new MouseMotionListener() {
			@Override public void mouseDragged(MouseEvent event) {
				handleMovement(event);
			}

			@Override public void mouseMoved(MouseEvent event) {
				handleMovement(event);
			}
		});
		window.add(preview, BorderLayout.CENTER);
		
		status.setText("please open an image");
		window.add(status, BorderLayout.SOUTH);
		
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
	// </editor-fold>
	
	private void handleOpen(ActionEvent event) {
		if (jfc.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) return;
		File[] files = jfc.getSelectedFiles();
		images = new BufferedImage[files.length];
		names = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			try {
				names[i] = files[i].getName().substring(0, files[i].getName().lastIndexOf("."));
				status.setText("opening " + names[i] + " (" + (i + 1) + " / " + files.length + ") ...");
				images[i] = ImageIO.read(files[i]);
			} catch (IOException exception) {
				status.setText("while opening " + names[i] + " (" + (i + 1) + " / " + files.length + "): " + exception.getMessage());
				images = null;
				window.setTitle(null);
				window.repaint();
				return;
			}
		}
		imageWidth = images[0].getWidth();
		imageHeight = images[0].getHeight();
		StringBuilder title = new StringBuilder();
		title.append("cropping ");
		title.append(names[0]);
		for (int i = 1; i < files.length; i++) {
			if (images[i].getWidth() != imageWidth || images[i].getHeight() != imageHeight) {
				status.setText("the images don't have the same dimensions");
				images = null;
				window.setTitle(null);
				window.repaint();
				return;
			}
			title.append(", ");
			title.append(names[i]);
		}
		window.setTitle(title.toString());
		handleSize();
	}
	
	private void handleWidth(DocumentEvent event) {
		int length = event.getDocument().getLength();
		if (length <= 0) {
			status.setText("please enter a width");
			return;
		}
		try {
			String string = event.getDocument().getText(0, length);
			if (string.startsWith("-")) {
				status.setText("width can't be negative");
				return;
			}
			inputWidth = Integer.parseInt(string);
			if (inputWidth > imageWidth) {
				status.setText("width is wider than image(s)");
				return;
			}
			handleSize();
		} catch (BadLocationException exception) {
			throw new Error(exception);
		} catch (NumberFormatException exception) {
			status.setText("width: " + exception.getMessage());
		}
	}
	
	private void handleHeight(DocumentEvent event) {
		int length = event.getDocument().getLength();
		if (length <= 0) {
			status.setText("please enter a height");
			return;
		}
		try {
			String string = event.getDocument().getText(0, length);
			if (string.startsWith("-")) {
				status.setText("height can't be negative");
				return;
			}
			inputHeight = Integer.parseInt(string);
			if (inputHeight > imageHeight) {
				status.setText("height is higher than image(s)");
				return;
			}
			handleSize();
		} catch (BadLocationException exception) {
			throw new Error(exception);
		} catch (NumberFormatException exception) {
			status.setText("height: " + exception.getMessage());
		}
	}
	
	private void handleSize() {
		if (inputWidth > imageWidth) {
			status.setText("width is wider than image(s)");
			return;
		}
		if (inputHeight > imageHeight) {
			status.setText("height is higher than image(s)");
			return;
		}
		for (cropFactor = 1; ((inputWidth * cropFactor * 2) <= imageWidth) && ((inputHeight * cropFactor * 2) <= imageHeight); cropFactor *= 2);
		for (previewFactor = 1; ((inputWidth / previewFactor) > maxPreviewWidth) || ((inputHeight / previewFactor) > maxPreviewHeight); previewFactor *= 2);
		x = 0;
		y = 0;
		window.repaint();
	}
	
	private void handleSave(ActionEvent event) {
		for (int i = 0; i < images.length; i++) {
			status.setText("saving " + names[i] + " (" + (i + 1) + " / " + images.length + ") ...");
			BufferedImage image = new BufferedImage(cropFactor * inputWidth, cropFactor * inputHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics graphics = image.createGraphics();
			graphics.drawImage(images[i],
				0, 0,
				image.getWidth(), image.getHeight(),
				x, y,
				x + cropFactor * inputWidth, y + cropFactor * inputHeight,
			null);
			try {
				ImageIO.write(image, "PNG", new File(
					jfc.getCurrentDirectory().getAbsolutePath() + "/" + names[i] + " (" + cropFactor * inputWidth + " × " + cropFactor * inputHeight + ").png"
				));
			} catch (IOException exception) {
				status.setText("while saving " + names[i] + " (" + (i + 1) + " / " + images.length + "): " + exception.getMessage());
				return;
			}
		}
		window.repaint();
	}
	
	private void handleMovement(MouseEvent event) {
		int currX = event.getX(), currY = event.getY();
		if (mouseDown) {
			x += mouseX - currX;
			y += mouseY - currY;
			if (x < 0) x = 0;
			else if (x + cropFactor * inputWidth > imageWidth) x = imageWidth - cropFactor * inputWidth;
			if (y < 0) y = 0;
			else if (y + cropFactor * inputHeight > imageHeight) y = imageHeight - cropFactor * inputHeight;
			window.repaint();
		}
		mouseX = currX;
		mouseY = currY;
	}
}