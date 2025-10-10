package com.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
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
	private static JSplitPane splitPane;

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

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		// setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, InfoPanel.gray));
		splitPane.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, InfoPanel.gray));

		splitPane.setContinuousLayout(true); // live dragging
		splitPane.setOneTouchExpandable(true); // little arrows to collapse/expand

		splitPane.setLeftComponent(infoPanel);
		splitPane.setRightComponent(listingPanel);
		frame.add(splitPane, BorderLayout.CENTER);

		int dividerLocation = preferences.getInt("splitPaneDividerLocation", frameWidth / 4);
		splitPane.setDividerLocation(dividerLocation);

		// when divider loc changed update pref
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
			int newLocation = (int) evt.getNewValue();
			preferences.putInt("splitPaneDividerLocation", newLocation);
		});

		// for some reason this sacred combination gets a good result
		splitPane.setBorder(null);
		splitPane.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, InfoPanel.gray));
		BasicSplitPaneUI ui = (BasicSplitPaneUI) splitPane.getUI();
		ui.getDivider().setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, InfoPanel.gray));

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

	}

	public void ChangeRightView(int i) {
		if (i == 0) {
			splitPane.setRightComponent(listingPanel);
		}
		if (i == 1) {
			splitPane.setRightComponent(manageTagsPanel);
		}
		splitPane.setDividerLocation(preferences.getInt("splitPaneDividerLocation", frame.getWidth() / 4));
		frame.revalidate();
		frame.repaint();
	}

}