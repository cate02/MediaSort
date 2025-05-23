package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

// import org.w3c.dom.events.MouseEvent; // Removed incorrect import

public class FilePanel extends JPanel {
	private FileItem fileItem;
	private Color gray = new Color(128, 128, 128);

	public FilePanel(FileItem fileItem) throws IOException {
		this.fileItem = fileItem;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
		JLabel nameLabel = new JLabel(fileItem.name);
		JLabel imgLabel = new JLabel();

		if (isImageFile(fileItem.file)) {
			Image image = ImageIO.read(fileItem.file);
			Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			imgLabel.setIcon(new ImageIcon(scaledImage));
		} else {
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
		}

		JButton tagsButton = new JButton("Tags");
		tagsButton.addActionListener(e -> {
			// openTagGUI(fileItem.id);

		});

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		namePanel.add(nameLabel, BorderLayout.CENTER);
		namePanel.add(tagsButton, BorderLayout.EAST);
		add(imgLabel, BorderLayout.CENTER);
		add(namePanel, BorderLayout.SOUTH);

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
}
