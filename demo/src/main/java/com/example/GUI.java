package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
	static List<FileItem> selectedFileItems = new ArrayList<>();

	static Color black = new Color(0, 0, 0);
	static Color gray = new Color(128, 128, 128);

	static JFrame frame = new JFrame();
	JPanel interfacePanel = new JPanel(); // search, listings
	JPanel listingPanel = new JPanel(); // listings
	JPanel tagsPanel = new JPanel();

	JList<String> searchedTagsList = new JList<>();
	JList<String> appliedTagsList = new JList<>();
	static JList<String> fileTagsList = new JList<>();
	JTextField tagSearchField = new JTextField();

	static JTabbedPane tabbedPane = new JTabbedPane();

	GUI() {
		updateSearchTags();
		setUpGUI();

		List<FileItem> fileItems = DbManager.getFileItems();
		for (FileItem fileItem : fileItems) {
			FilePanel filePanel = new FilePanel(fileItem);
			listingPanel.add(filePanel);
			System.out.println("Added file panel for " + fileItem.name);
		}
		CleanFrame(frame);
	}

	void setUpGUI() {
		System.out.println("Setting up GUI");

		frame.setVisible(true);
		frame.setTitle("MediaSort");
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
					if (tabbedPane.getSelectedIndex() == 0) {
						changeJList(appliedTagsList, searchedTagsList.getSelectedValue(), 1);
						changeSelectedFiles(null, -1);
					} else {
						DbManager.addTagToFiles(selectedFileItems, searchedTagsList.getSelectedValue());
						updateFileTags();
						// add tag to selected files
					}
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
		appliedTagsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					changeJList(appliedTagsList, appliedTagsList.getSelectedValue(), -1);
					updateResults();
				}
			}
		});

		// file tags //
		JScrollPane fileTagsPane = new JScrollPane();
		fileTagsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		fileTagsPane.setViewportView(fileTagsList);
		fileTagsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					DbManager.DeleteTagFromFiles(fileTagsList.getSelectedValue(), selectedFileItems);
					updateFileTags();
					updateResults();
				}
			}
		});

		// jtabbedpane holding apply and file panes
		tabbedPane.addTab("Applied Tags", appliedTagsPane);
		tabbedPane.addTab("File Tags", fileTagsPane);

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
			// confirm dialog
			int response = JOptionPane.showConfirmDialog(frame,
					"Are you sure you want to delete tag: " + searchedTagsList.getSelectedValue() + "?",
					"Confirm Delete tag " + searchedTagsList.getSelectedValue(), JOptionPane.YES_NO_OPTION);
			if (response == JOptionPane.YES_OPTION) {
				DbManager.DeleteTag(searchedTagsList.getSelectedValue());
			}
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
		CleanFrame(frame);
		// fsd -> xx
	}

	private static void updateFileTags() {
		// find all tags that all selected files use
		// add tags to file tags list

		// when search tag clicked
		// add tag to all selected files
		// update file tags

		// when file tag clicked
		// remove tag from all
		// update tags and results

		List<String> foundTags = new ArrayList<>();
		List<String> tags = new ArrayList<>();
		foundTags = DbManager.findTags(selectedFileItems);
		fileTagsList.setListData(foundTags.toArray(new String[0]));
	}

	private void updateResults() {
		List<FileItem> fileItems;
		List<String> tags = new ArrayList<>();
		for (int i = 0; i < appliedTagsList.getModel().getSize(); i++) {
			tags.add(appliedTagsList.getModel().getElementAt(i));
		}

		if (appliedTagsList.getModel().getSize() == 0)
			fileItems = DbManager.getFileItems();
		else
			fileItems = DbManager.findFiles(tags);

		List<FileItem> oldSelectedFileItems = new ArrayList<>(selectedFileItems);
		selectedFileItems.clear();
		listingPanel.removeAll();
		for (FileItem fileItem : fileItems) {
			for (FileItem selectedFileItem : oldSelectedFileItems) {
				if (selectedFileItem.id == fileItem.id) {
					fileItem.isSelected = selectedFileItem.isSelected;
					break;
				}
			}
			listingPanel.add(new FilePanel(fileItem));
		}
		System.out.println("Updated results with " + fileItems.size() + " items");
		CleanFrame(frame);
	}

	private void changeJList(JList<String> list, String tag, int action) {
		List<String> temp = new ArrayList<>();
		for (int i = 0; i < list.getModel().getSize(); i++) {
			temp.add(list.getModel().getElementAt(i));
		}
		if (action == 1) {
			temp.add(tag);
		} else if (action == -1) {
			temp.remove(tag);
		}
		list.setListData(temp.toArray(new String[0]));
	}

	public static void changeSelectedFiles(FileItem fileItem, int action) {
		if (selectedFileItems.isEmpty()) {
			if (action == 1) {
				tabbedPane.setSelectedIndex(1);
			}
		}

		if (fileItem != null) {
			if (action == 1) {
				selectedFileItems.add(fileItem);
			} else if (action == -1) {
				selectedFileItems.remove(fileItem);
			}
		} else {
			if (action == -1) {
				selectedFileItems.clear();
			}
		}

		if (selectedFileItems.size() <= 0) {
			tabbedPane.setEnabledAt(1, false);
			// focus/select applied tags tabv
			tabbedPane.setSelectedIndex(0);
		} else {
			tabbedPane.setEnabledAt(1, true);
			updateFileTags();
		}

		CleanFrame(frame);

		System.out.println("Selected items: " + selectedFileItems.size() + " " + tabbedPane.getSelectedIndex());
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
		// contentPanel.add(scrollPane);

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
		CleanFrame(frame);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.requestFocus();
	}

	static void CleanFrame(JFrame frame) {
		// frame.getContentPane().removeAll();
		frame.pack();
		frame.repaint();
		frame.revalidate();
		// System.out.println("Cleaned frame");
	}
}