package com.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

// import org.w3c.dom.events.MouseEvent; // Removed incorrect import

public class FilePanel extends JPanel {
	private FileItem fileItem;
	private Color gray = new Color(128, 128, 128);
	private CardLayout cardLayout = new CardLayout();
	private JPanel cards = new JPanel(cardLayout);

	public FilePanel(FileItem fileItem) {
		this.fileItem = fileItem;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));

		JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(e -> {
			if (checkBox.isSelected()) {
				setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.GREEN));
				GUI.changeSelectedFiles(fileItem, 1);
			} else {
				setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
				GUI.changeSelectedFiles(fileItem, -1);
			}
		});
		JLabel nameLabel = new JLabel(fileItem.name);
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		namePanel.add(nameLabel, BorderLayout.CENTER);
		namePanel.add(checkBox, BorderLayout.EAST);
		add(namePanel, BorderLayout.SOUTH);

		JPanel imageView = createImageView();
		JPanel detailView = createDetailView();
		cards.add(imageView, "image");
		cards.add(detailView, "detail");
		add(cards, BorderLayout.CENTER);
		showImageView();
	}

	private JPanel createImageView() {
		JPanel panel = new JPanel();
		JLabel imgLabel = new JLabel();
		panel.add(imgLabel, BorderLayout.CENTER);

		if (isImageFile(fileItem.file)) {
			try {
				Image image = ImageIO.read(fileItem.file);
				Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
				imgLabel.setIcon(new ImageIcon(scaledImage));
			} catch (IOException ex) {
			}
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
		List<String> list = fileItem.list;
		JList<String> tagsList = new JList<>(list.toArray(new String[0]));
		panel.add(tagsList);

		return panel;
	}

	static boolean isImageFile(File file) {
		String[] imageExtensions = new String[] { "jpg", "jpeg", "png", "gif", "bmp" };
		for (String extension : imageExtensions) {
			if (file.getName().endsWith(extension)) {
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
}
