package com.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public final class GUI {
	private static Preferences preferences;

	// static List<FileItem> selectedFileItems = new ArrayList<>();

	static JFrame frame = new JFrame();
	ListingPanel listingPanel = new ListingPanel();
	InfoPanel infoPanel = new InfoPanel();
	TagManager manageTagsPanel = new TagManager();
	private static JSplitPane leftMiddleSplit;
	private static JSplitPane middleRightSplit;

	public static JTabbedPane tabbedPane = new JTabbedPane();

	GUI() {
		infoPanel.gui = this;
		infoPanel.frame = frame;
		infoPanel.listingPanel = listingPanel;

		List<FileItem> fileItems = DbManager.getFileItems();
		for (FileItem fileItem : fileItems) {
			FilePanel filePanel = new FilePanel(fileItem);
			listingPanel.addPanel(filePanel);
		}
		listingPanel.updateImageScales();

		preferences = Preferences.userNodeForPackage(MediaSort.class);
		setUpGUI();
		frame.repaint();
		frame.revalidate();

	}

	void setUpGUI() {

		System.out.println("Setting up GUI");

		frame.setVisible(true);
		frame.setTitle("MediaSort");
		// frame.setLocationRelativeTo(null);
		int frameWidth = preferences.getInt("frameWidth", 1000);
		int frameHeight = preferences.getInt("frameHeight", 600);
		frame.setSize(frameWidth, frameHeight);
		int frameX = preferences.getInt("frameX", 100);
		int frameY = preferences.getInt("frameY", 100);
		frame.setLocation(frameX, frameY);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		listingPanel.setMinimumSize(new Dimension(100, 100));
		infoPanel.setMinimumSize(new Dimension(100, 100));
		manageTagsPanel.setMinimumSize(new Dimension(100, 100));
		frame.setMinimumSize(new Dimension(400, 250));

		leftMiddleSplit = new JSplitPane(setupSplitPane());
		middleRightSplit = setupSplitPane();

		leftMiddleSplit.setLeftComponent(infoPanel);
		leftMiddleSplit.setRightComponent(listingPanel);

		middleRightSplit.setLeftComponent(leftMiddleSplit);
		middleRightSplit.setRightComponent(manageTagsPanel);

		frame.add(middleRightSplit, BorderLayout.CENTER);
		frame.add(leftMiddleSplit, BorderLayout.CENTER);

		int dividerLocation = preferences.getInt("splitPaneDividerLocation", frameWidth / 4);
		leftMiddleSplit.setDividerLocation(dividerLocation);
		int secondDividerLocation = preferences.getInt("secondSplitPaneDividerLocation", (frameWidth / 4) * 3);
		middleRightSplit.setDividerLocation(secondDividerLocation);

		// when window resized, update divider location to keep same percent
		frame.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt) {
				int dividerValue = preferences.getInt("splitPaneDividerLocation", 25);
				if ((preferences.getInt("resizeSaveType", 0)) == 0) {
					int newDividerLocation = (int) (((double) dividerValue / 100.0) * (double) frame.getWidth());
					leftMiddleSplit.setDividerLocation(newDividerLocation);
				} else {
					leftMiddleSplit.setDividerLocation(dividerValue);
				}
				frame.repaint();
				frame.revalidate();
			}
		});

		// when program closed
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				preferences.putInt("frameWidth", frame.getWidth());
				preferences.putInt("frameHeight", frame.getHeight());
				preferences.putInt("frameX", frame.getX());
				preferences.putInt("frameY", frame.getY());
			}
		});

		JPanel footerPanel = footerPanel();
		frame.add(footerPanel, BorderLayout.SOUTH);
	}

	JSplitPane setupSplitPane() {
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.5); // equal resize
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(true);
		attachUserDragListener(splitPane);
		// leftMiddleSplit.setBorder(null);
		leftMiddleSplit.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, InfoPanel.gray));
		// ui.getDivider().setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0,
		// InfoPanel.gray));

		return splitPane;
	}

	private void attachUserDragListener(JSplitPane splitPane) {
		final boolean[] userDragging = { false };
		BasicSplitPaneUI ui = (BasicSplitPaneUI) splitPane.getUI();
		ui.getDivider().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				userDragging[0] = true;
			}

			public void mouseReleased(MouseEvent e) {
				userDragging[0] = false;
			}
		});

		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
			if (userDragging[0]) {
				System.out.println("Divider moved manually: " + splitPane.getDividerLocation());
			}
		});
	}

	void saveSplitPaneDividerLocation() {
		if ((preferences.getInt("resizeSaveType", 0)) == 0) {
			int newDividerPercent = (int) (((double) leftMiddleSplit.getDividerLocation()
					/ (double) leftMiddleSplit.getWidth()) * 100);
			preferences.putInt("splitPaneDividerLocation", newDividerPercent);
		} else {
			int newDividerLocation = leftMiddleSplit.getDividerLocation();
			preferences.putInt("splitPaneDividerLocation", newDividerLocation);
		}
	}

	JPanel footerPanel() {
		JPanel panel = new JPanel(new java.awt.GridBagLayout());
		panel.setBorder(BorderFactory.createMatteBorder(0, 5, 5, 5, InfoPanel.gray));

		JLabel label = new JLabel("Resize save type:");
		JComboBox<String> saveTypeComboBox = new JComboBox<>(new String[] { "Pixels", "Percent" });

		// --- Fix 1: Correct layout constraints ---
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.insets = new java.awt.Insets(5, 5, 5, 5);
		gbc.anchor = java.awt.GridBagConstraints.WEST;

		// Label in column 0
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(label, gbc);

		// Combo box in column 1, stretches horizontally
		gbc.gridx = 1;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		panel.add(saveTypeComboBox, gbc);

		// --- Fix 2: Logic correction ---
		int resizeSaveType = preferences.getInt("resizeSaveType", 0);
		if (resizeSaveType == 1) {
			saveTypeComboBox.setSelectedItem("Pixels");
		} else {
			saveTypeComboBox.setSelectedItem("Percent");
		}

		saveTypeComboBox.addActionListener(e -> {
			String selected = (String) saveTypeComboBox.getSelectedItem();
			if ("Pixels".equals(selected)) {
				preferences.putInt("resizeSaveType", 1);
			} else {
				preferences.putInt("resizeSaveType", 0);
			}
			saveSplitPaneDividerLocation();
		});

		return panel;
	}

	public void changeRightView(int i) {
		if (i == 0) {
			leftMiddleSplit.setRightComponent(listingPanel);
		}
		if (i == 1) {
			leftMiddleSplit.setRightComponent(manageTagsPanel);
		}
		leftMiddleSplit.setDividerLocation(preferences.getInt("splitPaneDividerLocation", frame.getWidth() / 4));
		frame.revalidate();
		frame.repaint();
	}

}