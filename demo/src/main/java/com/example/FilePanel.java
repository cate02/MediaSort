package com.example;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;

// import org.w3c.dom.events.MouseEvent; // Removed incorrect import

enum TagMatchState {
	Full, Partial, None
}

public class FilePanel extends JPanel {
	private boolean mouseInside = false;
	private boolean oldMouseInside = false;

	public FileItem fileItem;
	private Color gray = new Color(128, 128, 128);
	private CardLayout cardLayout = new CardLayout();
	private JPanel cards = new JPanel(cardLayout);
	private JLabel imgLabel = new JLabel();
	private JPanel namePanel;

	private JList<String> tagsList;
	private JScrollPane tagsScrollPane;

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
		setupListeners();

		showImageView();
	}

	int i = 0;

	void setupListeners() {
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
					i++;
					mouseInside = true;
					oldMouseInside = mouseInside;
					setSelected(!fileItem.isSelected);
					// System.out.println("Pressed " + i);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				i++;
				// System.out.println("Released " + i);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				i++;
				if (e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
					// System.err.println("Entered " + i);
					mouseInside = true;
					if (oldMouseInside == false) {
						setSelected(!fileItem.isSelected);
					}
					oldMouseInside = mouseInside;
					// is mouse

				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// find whether mouse still inside filepanel
				Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), FilePanel.this);
				boolean actuallyInside = new Rectangle(0, 0, getWidth(), getHeight()).contains(p);

				// debugRect(getBounds(), Color.RED);
				// debugRect(new java.awt.Rectangle(e.getX(), e.getY(), 1, 1), Color.BLUE);

				if (actuallyInside) {
					// System.err.println("Exited but still inside " + i + " " +
					// FilePanel.this.getClass().getName());
					return;
				}
				i++;
				// System.out.println("Exited " + i);
				mouseInside = false;
				oldMouseInside = mouseInside;
			}
		});
	}

	void debugRect(Rectangle rect, Color color) {
		JRootPane root = SwingUtilities.getRootPane(FilePanel.this);
		if (root == null)
			return;

		JComponent glass = (JComponent) root.getGlassPane();
		glass.setVisible(true);
		glass.setLayout(null);

		// Convert correctly: if rect is from getBounds(), convert from parent, not
		// FilePanel
		Rectangle rOnGlass = SwingUtilities.convertRectangle(FilePanel.this.getParent(), // convert from parent
																							// coordinates
				rect, glass);

		JComponent overlay = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setColor(color);
				g2.setStroke(new BasicStroke(2));
				g2.drawRect(rOnGlass.x, rOnGlass.y, rOnGlass.width, rOnGlass.height);
			}
		};
		overlay.setOpaque(false);
		overlay.setBounds(0, 0, glass.getWidth(), glass.getHeight());
		glass.add(overlay, 0);
		glass.repaint();

		new Timer(500, e -> {
			glass.remove(overlay);
			glass.repaint();
		}).start();
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

		return panel;
	}

	private JPanel createDetailView() {
		// list of tags center, bottom title
		// hover tags shows connected tags?
		// use jtree?
		JPanel panel = new JPanel();
		tagsList = new JList<>(fileItem.tagsList.toArray(new String[0]));
		tagsScrollPane = new JScrollPane(tagsList);
		tagsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		tagsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		tagsList.setEnabled(false);
		tagsList.setFocusable(false);
		// to make it take as much space
		panel.setLayout(new GridLayout());
		panel.add(tagsScrollPane);

		propagateMouse(panel);
		propagateMouse(tagsScrollPane);
		propagateMouse(tagsList);
		setupTagListRenderer();

		return panel;
	}

	private void propagateMouse(JComponent comp) {
		comp.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				FilePanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, FilePanel.this));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				FilePanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, FilePanel.this));
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				FilePanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, FilePanel.this));
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				FilePanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, FilePanel.this));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				FilePanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, FilePanel.this));
			}

		});
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
		// System.out.println("Drawing image for " + fileItem.name);
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

	Map<String, TagMatchState> highlightMap = new HashMap<>();

	public void setHighlight(HashMap<String, TagMatchState> highlightMap) {
		this.highlightMap = highlightMap;
		tagsList.repaint(); // triggers re-rendering of cells with new colors
	}

	private void setupTagListRenderer() {
		tagsList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				String tag = (String) value;
				TagMatchState state = highlightMap.getOrDefault(tag, TagMatchState.None);

				// Apply color depending on tag's state
				if (state == TagMatchState.Full) {
					// make it ligher blue
					c.setBackground(new Color(204, 229, 255)); // light blue
				} else if (state == TagMatchState.Partial) {
					// lighter purple
					c.setBackground(new Color(255, 204, 255)); // light purple

				} else if (!isSelected) {
					c.setBackground(Color.WHITE); // reset
				}

				return c;
			}
		});
	}

}
