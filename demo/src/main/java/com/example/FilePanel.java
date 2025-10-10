package com.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
	private JPanel namePanel;

	public JPanel detailView;

	private int borderSize = 3;

	public void setSelected(boolean state) {
		// for stacking select all click
		if (state == fileItem.isSelected)
			return;
		if (!ListingPanel.canSelect)
			return;
		fileItem.isSelected = state;
		setBorder(BorderFactory.createMatteBorder(borderSize, borderSize, borderSize, borderSize,
				fileItem.isSelected ? Color.green : gray));
		ListingPanel.changeSelectedFiles(fileItem, state ? 1 : -1);

	}

	public FilePanel(FileItem fileItem) {

		this.fileItem = fileItem;
		fileItem.filePanel = this;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(borderSize, borderSize, borderSize, borderSize,
				fileItem.isSelected ? Color.green : gray));

		JLabel nameLabel = new JLabel(fileItem.name);
		namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		namePanel.add(nameLabel, BorderLayout.CENTER);
		add(namePanel, BorderLayout.SOUTH);

		JPanel imageView = createImageView();
		detailView = createDetailView();
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
			// set the fucking image to the fucking files image
			try {
				// image = ImageIO.read(fileItem.file);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// show placeholder immediately
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon")); // or custom "loading" icon

			// Load in background

		} else {
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
		}

		// fix not registering when mouse moves a pixel
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// if left click
				if (e.getButton() == MouseEvent.BUTTON1) {

					if (e.getClickCount() == 2) {
						try {
							Desktop.getDesktop().open(fileItem.file);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
				if (e.getButton() == MouseEvent.BUTTON3) {
					try {
						Desktop.getDesktop().open(fileItem.file.getParentFile()); // fallback for non-Windows

						// On Windows, open Explorer with the file pre-selected
						if (System.getProperty("os.name").toLowerCase().contains("win")) {
							String path = fileItem.file.getAbsolutePath();
							// explorer expects backslashes
							path = path.replace('/', '\\');
							new ProcessBuilder("explorer.exe", "/select,", path).start();
						} else {
							// rip bozo
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					setSelected(!fileItem.isSelected);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				System.err.println("entered " + fileItem.name);
				if (e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
					System.err.println("drag select " + fileItem.name);
					setSelected(!fileItem.isSelected);
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
		tagsList.setEnabled(false);
		tagsList.setFocusable(false);
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

	private SwingWorker<ImageIcon, Void> imageWorker;

	public void drawImage() {
		System.out.println("Drawing image for " + fileItem.name);
		// Cancel previous worker if still running
		if (imageWorker != null && !imageWorker.isDone()) {
			imageWorker.cancel(true);
		}

		imageWorker = new SwingWorker<>() {

			@Override

			protected ImageIcon doInBackground() throws Exception {

				if (isCancelled())
					return null;
				BufferedImage img = ImageIO.read(fileItem.file);

				if (img == null || isCancelled())
					return null;
				if (img.getWidth() <= 0 || img.getHeight() <= 0)
					return null;

				int panelWidth = ListingPanel.panelWidth;
				int panelHeight = ListingPanel.panelHeight - namePanel.getHeight() * 2;

				double scaleX = (double) panelWidth / img.getWidth();
				double scaleY = (double) panelHeight / img.getHeight();
				double scale = Math.min(scaleX, scaleY);

				// if scale <=0 return null
				if (scale <= 0)
					return null;

				int newW = (int) (img.getWidth() * scale);
				int newH = (int) (img.getHeight() * scale);

				BufferedImage scaled = getScaledImage(img, newW, newH);
				return new ImageIcon(scaled);
			}

			@Override
			protected void done() {
				if (isCancelled())
					return;
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
		imageWorker.execute();
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

	/*
	 * private void addSelectionListeners(JPanel filePanel, FileItem fileItem) {
	 * MouseAdapter listener = new MouseAdapter() {
	 *
	 * @Override public void mousePressed(MouseEvent e) { mouseDown = true;
	 * fileItem.filePanel.setSelected(!fileItem.isSelected); }
	 *
	 * @Override public void mouseReleased(MouseEvent e) { mouseDown = false; }
	 *
	 * @Override public void mouseEntered(MouseEvent e) { if (mouseDown) {
	 * fileItem.filePanel.setSelected(!fileItem.isSelected); } } };
	 * //filePanel.addMouseListener(listener); }
	 *
	 * int iii = 0;
	 *
	 * private void addMouseListenerRecursively(Component comp, FileItem fileItem) {
	 * MouseAdapter listener = new MouseAdapter() {
	 *
	 * @Override public void mousePressed(MouseEvent e) { mouseDown = true; //
	 * fileItem.filePanel.setSelected(!fileItem.isSelected);
	 * fileItem.selectControlCount++; iii++; System.out.println(fileItem.name + " "
	 * + iii + " press"); //
	 * fileItem.filePanel.setSelected(fileItem.selectControlCount > 0); if
	 * (fileItem.selectControlCount > 0) {
	 * fileItem.filePanel.setSelected(!fileItem.isSelected);
	 * System.out.println("toggled"); } else System.out.println("not toggled");
	 *
	 * }
	 *
	 * @Override public void mouseReleased(MouseEvent e) { mouseDown = false;
	 * fileItem.selectControlCount--; iii--; System.out.println(fileItem.name + " "
	 * + iii + " release"); //
	 * fileItem.filePanel.setSelected(fileItem.selectControlCount > 0); // iii++; }
	 *
	 * @Override public void mouseEntered(MouseEvent e) { if (mouseDown) { //
	 * fileItem.filePanel.setSelected(!fileItem.isSelected); iii++;
	 * System.out.println(fileItem.name + " " + iii + " enter");
	 *
	 * // fileItem.filePanel.setSelected(fileItem.selectControlCount > 0); if
	 * (fileItem.selectControlCount == 0) {
	 * fileItem.filePanel.setSelected(!fileItem.isSelected); }
	 * fileItem.selectControlCount++;
	 *
	 * } }
	 *
	 * @Override public void mouseExited(MouseEvent e) { if (mouseDown) { iii--;
	 * System.out.println(fileItem.name + " " + iii + " exit");
	 * fileItem.selectControlCount--; } //
	 * fileItem.filePanel.setSelected(fileItem.selectControlCount > 0); } };
	 *
	 * comp.addMouseListener(listener); if (comp instanceof Container container) {
	 * for (Component child : container.getComponents()) {
	 * addMouseListenerRecursively(child, fileItem); } } }
	 */

}
