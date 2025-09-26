package com.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

// import org.w3c.dom.events.MouseEvent; // Removed incorrect import

public class FilePanel extends JPanel {
	public FileItem fileItem;
	private Color gray = new Color(128, 128, 128);
	private CardLayout cardLayout = new CardLayout();
	private JPanel cards = new JPanel(cardLayout);
	private JLabel imgLabel = new JLabel();
	private Image image;
	private JPanel namePanel;

	private int borderSize = 3;

	public void setSelected(boolean state) {
		fileItem.isSelected = state;
		setBorder(BorderFactory.createMatteBorder(borderSize, borderSize, borderSize, borderSize,
				fileItem.isSelected ? Color.green : gray));
		ListingPanel.changeSelectedFiles(fileItem, state ? 1 : -1);
	}

	public FilePanel(FileItem fileItem) {

		this.fileItem = fileItem;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(borderSize, borderSize, borderSize, borderSize,
				fileItem.isSelected ? Color.green : gray));

		JLabel nameLabel = new JLabel(fileItem.name);
		namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		namePanel.add(nameLabel, BorderLayout.CENTER);
		add(namePanel, BorderLayout.SOUTH);

		JPanel imageView = createImageView();
		JPanel detailView = createDetailView();
		cards.add(imageView, "image");
		cards.add(detailView, "detail");
		add(cards, BorderLayout.CENTER);
		showImageView();
	}

	private JPanel createImageView() {
		JPanel panel = new JPanel(new BorderLayout());
		imgLabel.setHorizontalAlignment(JLabel.CENTER);
		imgLabel.setVerticalAlignment(JLabel.CENTER);
		panel.add(imgLabel, BorderLayout.CENTER);

		if (isImageFile(fileItem.file)) {
			// show placeholder immediately
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon")); // or custom "loading" icon

			// Load in background
			SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
				@Override
				protected ImageIcon doInBackground() throws Exception {
					BufferedImage img = ImageIO.read(fileItem.file);
					if (img == null)
						return null;

					// scale it for the panel
					int panelWidth = ListingPanel.panelWidth;
					int panelHeight = ListingPanel.panelHeight - namePanel.getHeight() * 2;

					double scaleX = (double) panelWidth / img.getWidth();
					double scaleY = (double) panelHeight / img.getHeight();
					double scale = Math.min(scaleX, scaleY);

					int newW = (int) (img.getWidth() * scale);
					int newH = (int) (img.getHeight() * scale);

					BufferedImage scaled = getScaledImage(img, newW, newH);
					return new ImageIcon(scaled);
				}

				@Override
				protected void done() {
					try {
						ImageIcon icon = get();
						if (icon != null) {
							imgLabel.setIcon(icon);
						} else {
							imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			worker.execute();
		} else {
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
		}

		// fix not registering when mouse moves a pixel
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						Desktop.getDesktop().open(fileItem.file);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		return panel;
	}

	private JPanel createDetailView() {
		// list of tags center, bottom title
		// hover tags shows connected tags?
		// use jtree?
		JPanel panel = new JPanel();
		JList<String> tagsList = new JList<>(fileItem.tagsList.toArray(new String[0]));
		JScrollPane scrollPane = new JScrollPane(tagsList);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		// to make it take as much space
		panel.setLayout(new GridLayout());
		panel.add(scrollPane);
		return panel;
	}

	static boolean isImageFile(File file) {
		String name = file.getName().toLowerCase();
		String[] imageExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp" };
		for (String ext : imageExtensions) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public void showImageView() {
		cardLayout.show(cards, "image");
	}

	public void showDetailView() {
		cardLayout.show(cards, "detail");
	}

	public void drawImage() {
		if (image == null) {
			return;
		}

		int imgWidth = image.getWidth(null);
		int imgHeight = image.getHeight(null);
		int panelWidth = ListingPanel.panelWidth;
		int panelHeight = ListingPanel.panelHeight;
		panelHeight -= namePanel.getHeight() * 2;

		double scaleX = (double) panelWidth / imgWidth;
		double scaleY = (double) panelHeight / imgHeight;
		double scale = Math.min(scaleX, scaleY); // fit inside panel

		int newW = (int) (imgWidth * scale);
		int newH = (int) (imgHeight * scale);

		BufferedImage buffered = toBufferedImage(image); // ✅ convert first
		BufferedImage scaled = getScaledImage(buffered, newW, newH);

		imgLabel.setIcon(new ImageIcon(scaled));
	}

	private static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bimage.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return bimage;
	}

	private static BufferedImage getScaledImage(BufferedImage src, int w, int h) {
		BufferedImage resized = new BufferedImage(w, h,
				src.getType() == 0 ? BufferedImage.TYPE_INT_RGB : src.getType());
		Graphics2D g2 = resized.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(src, 0, 0, w, h, null);
		g2.dispose();
		return resized;
	}

}
