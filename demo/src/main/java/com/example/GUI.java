package com.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class GUI {
	private static Preferences preferences;

	// static List<FileItem> selectedFileItems = new ArrayList<>();

	static JFrame frame = new JFrame();
	ListingPanel listingPanel = new ListingPanel();
	InfoPanel infoPanel = new InfoPanel();

	JList<String> searchedTagsList = new JList<>();
	JList<String> appliedTagsList = new JList<>();
	static JList<String> fileTagsList = new JList<>();
	JTextField tagSearchField = new JTextField();

	public static JTabbedPane tabbedPane = new JTabbedPane();

	static List<FilePanel> activeFiles = new ArrayList<>();
	boolean isDetailView = false;

	GUI() {
		infoPanel.listingPanel = listingPanel;
		infoPanel.frame = frame;

		preferences = Preferences.userNodeForPackage(MediaSort.class);
		setUpGUI();
		activeFiles.clear();
		List<FileItem> fileItems = DbManager.getFileItems();
		for (FileItem fileItem : fileItems) {
			FilePanel filePanel = new FilePanel(fileItem);
			listingPanel.addPanel(filePanel);
			activeFiles.add(filePanel);
			// System.out.println("Added file panel for " + fileItem.name);
		}

		// RefreshListing();
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

		infoPanel.setMinimumSize(new Dimension(150, 0));
		listingPanel.setMinimumSize(new Dimension(150, 0));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		splitPane.setContinuousLayout(true); // live dragging
		splitPane.setOneTouchExpandable(true); // little arrows to collapse/expand

		splitPane.setLeftComponent(infoPanel);
		splitPane.setRightComponent(listingPanel);
		frame.add(splitPane, BorderLayout.CENTER);

		int dividerLocation = preferences.getInt("splitPaneDividerLocation", frameWidth / 4);
		splitPane.setDividerLocation(dividerLocation);

		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				preferences.putInt("splitPaneDividerLocation", splitPane.getDividerLocation());
			}
		});
		splitPane.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, InfoPanel.gray));

		// frame.add(listingPanel, BorderLayout.CENTER);
		// frame.add(infoPanel, BorderLayout.WEST);

		// min size frame
		// frame.setMinimumSize(new Dimension(600, 340));
		// frame.setPreferredSize(new Dimension(1000, 600));

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

}