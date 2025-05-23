package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class GUI {
	static Color black = new Color(0, 0, 0);
	static Color gray = new Color(128, 128, 128);

	private JFrame frame = new JFrame();
	// private JPanel interfacePanel = new JPanel(); // search, listings
	private JPanel listingPanel = new JPanel(); // listings
	private JPanel tagsPanel = new JPanel();

	GUI() {
		SetUpGUI();
		List<FileItem> fileItems = DbManager.getFileItems();
		for (FileItem fileItem : fileItems) {
			try {
				FilePanel filePanel = new FilePanel(fileItem);
				listingPanel.add(filePanel);
				System.out.println("Added file panel for " + fileItem.name);
			} catch (IOException ex) {
			}
		}
		frame.pack();
	}

	void SetUpGUI() {
		System.out.println("Setting up GUI");

		frame.pack();
		// frame.add(panel);
		frame.setVisible(true);
		frame.setTitle("MediaSort");
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("MediaSort");
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.add(listingPanel, BorderLayout.CENTER);
		frame.add(tagsPanel, BorderLayout.WEST);

		// min size frame
		frame.setMinimumSize(new Dimension(600, 340));
		// frame.setPreferredSize(new Dimension(1000, 600));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JList<HashMap<String, Integer>> tagsList = new JList<>();
		scrollPane.setViewportView(tagsList);

		JScrollPane appliedTagsPane = new JScrollPane();
		appliedTagsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JList<HashMap<String, Integer>> selectedTags = new JList<>();
		appliedTagsPane.setViewportView(selectedTags);
		ArrayList<HashMap<String, Integer>> selectedTagsArray = new ArrayList<>();
		JTextField tagSearchField = new JTextField();

		listingPanel.setBorder(BorderFactory.createMatteBorder(30, 30, 30, 30, black));
		listingPanel.setLayout(new GridLayout(0, 3));

		tagsPanel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
		tagsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;

		tagsPanel.add(new JLabel("Applied tags"), c);
		// c.gridy = 1;
		c.weighty = 6;
		tagsPanel.add(appliedTagsPane, c);
		c.weighty = 1;
		tagsPanel.add(new JLabel("Search tags"), c);
		c.weighty = 1;
		tagsPanel.add(tagSearchField, c);
		// tagsPanel.setPreferredSize(new Dimension(0, 15));
		c.weighty = 6;
		tagsPanel.add(scrollPane, c);
	}
}