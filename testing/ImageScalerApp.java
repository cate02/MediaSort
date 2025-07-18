import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public class ImageScalerApp extends JFrame {

	private BufferedImage originalImage;
	private Image scaledImage;
	private int scaledWidth = 200;
	private int scaledHeight = 200;
	private int scaleMethod = Image.SCALE_SMOOTH;

	private ImagePanel imagePanel;
	private JComboBox<String> scaleMethodBox;
	private JSlider widthSlider, heightSlider;

	public ImageScalerApp() {
		super("Image Scaling Demo");

		// UI setup
		setLayout(new BorderLayout());

		// Image panel
		imagePanel = new ImagePanel();
		add(imagePanel, BorderLayout.CENTER);

		// Controls
		JPanel controlPanel = new JPanel(new FlowLayout());

		JButton loadButton = new JButton("Load Image");
		loadButton.addActionListener(e -> loadImage());

		widthSlider = new JSlider(50, 800, scaledWidth);
		widthSlider.setMajorTickSpacing(100);
		widthSlider.setPaintTicks(true);
		widthSlider.setPaintLabels(true);
		widthSlider.addChangeListener(e -> updateScaledImage());

		heightSlider = new JSlider(50, 800, scaledHeight);
		heightSlider.setMajorTickSpacing(100);
		heightSlider.setPaintTicks(true);
		heightSlider.setPaintLabels(true);
		heightSlider.addChangeListener(e -> updateScaledImage());

		scaleMethodBox = new JComboBox<>(new String[] { "SCALE_DEFAULT", "SCALE_FAST", "SCALE_SMOOTH",
				"SCALE_REPLICATE", "SCALE_AREA_AVERAGING" });
		scaleMethodBox.setSelectedItem("SCALE_SMOOTH");
		scaleMethodBox.addActionListener(e -> updateScaleMethod());

		controlPanel.add(loadButton);
		controlPanel.add(new JLabel("Width:"));
		controlPanel.add(widthSlider);
		controlPanel.add(new JLabel("Height:"));
		controlPanel.add(heightSlider);
		controlPanel.add(new JLabel("Method:"));
		controlPanel.add(scaleMethodBox);

		add(controlPanel, BorderLayout.SOUTH);

		// Frame setup
		setSize(1000, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void loadImage() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				originalImage = ImageIO.read(chooser.getSelectedFile());
				updateScaledImage();
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Failed to load image");
			}
		}
	}

	private void updateScaleMethod() {
		String selected = (String) scaleMethodBox.getSelectedItem();
		switch (selected) {
		case "SCALE_FAST":
			scaleMethod = Image.SCALE_FAST;
			break;
		case "SCALE_SMOOTH":
			scaleMethod = Image.SCALE_SMOOTH;
			break;
		case "SCALE_REPLICATE":
			scaleMethod = Image.SCALE_REPLICATE;
			break;
		case "SCALE_AREA_AVERAGING":
			scaleMethod = Image.SCALE_AREA_AVERAGING;
			break;
		default:
			scaleMethod = Image.SCALE_DEFAULT;
		}
		updateScaledImage();
	}

	private void updateScaledImage() {
		if (originalImage != null) {
			scaledWidth = widthSlider.getValue();
			scaledHeight = heightSlider.getValue();
			scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, scaleMethod);
			imagePanel.repaint();
		}
	}

	class ImagePanel extends JPanel {
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (scaledImage != null) {
				int x = (getWidth() - scaledWidth) / 2;
				int y = (getHeight() - scaledHeight) / 2;
				g.drawImage(scaledImage, x, y, this);
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(ImageScalerApp::new);
	}
}
