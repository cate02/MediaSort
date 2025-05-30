package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class GUI {
	static Color black = new Color(0, 0, 0);
	static Color gray = new Color(128, 128, 128);

	private JFrame frame = new JFrame();
	// private JPanel interfacePanel = new JPanel(); // search, listings
	private JPanel listingPanel = new JPanel(); // listings
	private JPanel tagsPanel = new JPanel();

	private JList<String> searchedTagsList = new JList<>();
	private JList<String> appliedTagsList = new JList<>();
	private JList<String> fileTagsList = new JList<>();
	private JTextField tagSearchField = new JTextField();

	GUI() {
		updateSearchTags();
		setUpGUI();

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

	void setUpGUI() {
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

		// search tags//
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(searchedTagsList);
		searchedTagsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					System.out.println("Double clicked on tag: " + searchedTagsList.getSelectedValue());
					// append to applied tags list
					// Get current applied tags as a List
					List<String> temp = new ArrayList<>();
					for (int i = 0; i < appliedTagsList.getModel().getSize(); i++) {
						temp.add(appliedTagsList.getModel().getElementAt(i));
					}
					temp.add(searchedTagsList.getSelectedValue());
					appliedTagsList.setListData(temp.toArray(new String[0]));
					updateResults();
				}
			}
		});
		tagSearchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSearchTags();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSearchTags();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSearchTags();
			}
		});

		// apply tags//
		JScrollPane appliedTagsPane = new JScrollPane();
		appliedTagsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		appliedTagsPane.setViewportView(appliedTagsList);

		// file tags //
		JScrollPane fileTagsPane = new JScrollPane();
		fileTagsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		fileTagsPane.setViewportView(fileTagsList);

		// jtabbedpane holding apply and file panes
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Applied Tags", appliedTagsPane);
		tabbedPane.addTab("File Tags", fileTagsPane);
		// tabbedPane.setPreferredSize(new Dimension(300, 300));

		// control//
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(1, 3));
		JButton createTagButton = new JButton("Create new tag");
		createTagButton.addActionListener(e -> {
			createTagGUI();
		});
		JButton editTagButton = new JButton("Edit tag");
		editTagButton.addActionListener(e -> {
			// edit tag
		});
		JButton deleteTagButton = new JButton("Delete tag");
		deleteTagButton.addActionListener(e -> {
			// DbManager.DeleteTag(existingTagsTagMap.get(existingTagsList.getSelectedValue());
		});
		controlPanel.add(createTagButton);
		controlPanel.add(editTagButton);
		controlPanel.add(deleteTagButton);

		// setup panels//
		listingPanel.setBorder(BorderFactory.createMatteBorder(30, 30, 30, 30, black));
		listingPanel.setLayout(new GridLayout(0, 3));

		tagsPanel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
		tagsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;

		// tagsPanel.add(new JLabel("Applied tags"), c);
		// c.gridy = 1;
		c.weighty = 6;
		tagsPanel.add(tabbedPane, c);
		c.weighty = 1;
		tagsPanel.add(new JLabel("Search tags"), c);
		c.weighty = 1;
		tagsPanel.add(tagSearchField, c);
		// tagsPanel.setPreferredSize(new Dimension(0, 15));
		c.weighty = 6;
		tagsPanel.add(scrollPane, c);
		tagsPanel.add(controlPanel, c);
	}

	private void updateSearchTags() {
		String search = tagSearchField.getText();
		List<String> foundTags = DbManager.findTags(search);
		if (foundTags == null) {
			System.out.println("No tag found for search: " + search);
		}
		searchedTagsList.setListData(foundTags.toArray(new String[0]));
		frame.pack();
		// fsd -> xx
	}

	private void updateResults() {
		List<String> tags = new ArrayList<>();
		for (int i = 0; i < appliedTagsList.getModel().getSize(); i++) {
			tags.add(appliedTagsList.getModel().getElementAt(i));
		}
		List<FileItem> fileItems = DbManager.findFiles(tags);
		// empty listingpanel
		listingPanel.removeAll();
		for (FileItem fileItem : fileItems) {
			try {
				listingPanel.add(new FilePanel(fileItem));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		frame.pack();
	}

	static void createTagGUI() {
		JFrame frame = new JFrame();
		JPanel createTagPanel = new JPanel();
		createTagPanel.setLayout(new BorderLayout());
		JLabel panelTitle = new JLabel("Create Tag");
		createTagPanel.add(panelTitle, BorderLayout.NORTH);
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridLayout(0, 1));

		JPanel createControlPanel = new JPanel();
		createControlPanel.setLayout(new GridLayout(2, 2));
		JLabel mainTagLabel = new JLabel("Main Tag:");
		JTextField mainTagField = new JTextField();
		JLabel aliasLabel = new JLabel("Alias:");
		JTextField aliasField = new JTextField();

		createControlPanel.add(mainTagLabel);
		createControlPanel.add(mainTagField);
		createControlPanel.add(aliasLabel);
		createControlPanel.add(aliasField);
		contentPanel.add(createControlPanel);
		JButton addAliasButton = new JButton("Add alias");
		List<String> aliasTemp = new ArrayList<>();
		contentPanel.add(addAliasButton);

		JList<String> existingAliasesList = new JList<>(aliasTemp.toArray(new String[0]));
		// horizontal wrap, scrollable
		existingAliasesList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		JScrollPane scrollPane = new JScrollPane(existingAliasesList);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		addAliasButton.addActionListener(e -> {
			String alias = aliasField.getText();
			if (alias.isEmpty()) {
				return;
			} // if already exists
			if (aliasTemp.contains(alias)) {
				JOptionPane.showMessageDialog(frame, "Alias already in list", "Error", JOptionPane.ERROR_MESSAGE);
				aliasField.setText(alias);
				return;
			}

			aliasTemp.add(alias);
			aliasField.setText("");
			// refresh jlist
			existingAliasesList.setListData(aliasTemp.toArray(new String[0]));
		});

		JLabel existingSubTagsLabel = new JLabel("Existing aliases:");

		contentPanel.add(existingSubTagsLabel);
		contentPanel.add(scrollPane);

		// when double clicked, remove from list
		existingAliasesList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					int index = existingAliasesList.locationToIndex(evt.getPoint());
					if (index >= 0 && index < aliasTemp.size()) {
						aliasTemp.remove(index);
						existingAliasesList.setListData(aliasTemp.toArray(new String[0]));
					}
				}
			}
		});

		JPanel tagControlPanel = new JPanel();
		tagControlPanel.setLayout(new GridLayout(1, 2));
		JButton createTagButton = new JButton("Create Tag");
		createTagButton.addActionListener(e -> {
			if (mainTagField.getText().isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Main Tag cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (DbManager.doesTagExist(mainTagField.getText())) {
				JOptionPane.showMessageDialog(frame, "Main Tag already exists", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (DbManager.createTag(mainTagField.getText()) == 1) {
				// for all sub tags
				for (String subTag : aliasTemp) {
					if (!subTag.isEmpty()) {
						if (DbManager.createTagAlias(mainTagField.getText(), subTag) == 1) {
							// Successfully created sub tag
						} else {
							JOptionPane.showMessageDialog(frame, "Failed to create sub tag: " + subTag, "Error",
									JOptionPane.ERROR_MESSAGE);
						}
					}
				}
				JOptionPane.showMessageDialog(frame, "Tag created", "Success", JOptionPane.INFORMATION_MESSAGE);
				frame.dispose();
			} else {
				JOptionPane.showMessageDialog(frame, "Failed to create new tag", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		tagControlPanel.add(createTagButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			frame.dispose();
		});
		tagControlPanel.add(cancelButton);
		createTagPanel.add(tagControlPanel, BorderLayout.SOUTH);

		createTagPanel.add(contentPanel, BorderLayout.CENTER);
		frame.add(createTagPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.requestFocus();
	}
}