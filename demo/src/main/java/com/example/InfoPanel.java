package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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

public class InfoPanel extends JPanel {

	static Color gray = new Color(128, 128, 128);

	static JList<String> searchedTagsList = new JList<>();
	static JList<String> appliedTagsList = new JList<>();
	static JList<String> fileTagsList = new JList<>();
	static JTextField tagSearchField = new JTextField();

	public static JTabbedPane tabbedPane = new JTabbedPane();

	public static ListingPanel listingPanel;
	public static JFrame frame;
	static boolean isDetailView = false;
	public GUI gui;

	public Preferences preferences;

	public InfoPanel() {
		preferences = Preferences.userNodeForPackage(MediaSort.class);
		// pref size
		// setPreferredSize(new java.awt.Dimension(300, 600));

		SetUpPanels();

	}

	private void SetUpPanels() {
		SetUpTags();
		JPanel topPanel = SetUpTopPanel();
		JScrollPane searchPanel = SetUpSearch();
		JPanel controlPanel = SetUpControlPanel();

		// setup panels//
		// setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
		// setBorder(BorderFactory.createMatteBorder(0, 0, 0, 5, gray));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;

		// tagsPanel.add(new JLabel("Applied tags"), c);
		// c.gridy = 1;
		c.weighty = 1;
		add(topPanel, c);
		c.weighty = 6;
		add(tabbedPane, c);
		c.weighty = 1;
		add(new JLabel("Search tags"), c);
		c.weighty = 1;
		add(tagSearchField, c);
		// tagsPanel.setPreferredSize(new Dimension(0, 15));
		c.weighty = 6;
		add(searchPanel, c);
		add(controlPanel, c);
	}

	private JPanel SetUpTopPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		JLabel label = new JLabel("Current db: " + DbManager.contentPath);
		JButton switchDirButton = new JButton("Change db");
		switchDirButton.addActionListener(e -> {
			// System.out.println("Changing directory");
			DbManager.changeDirectory();
			updateResults();
			updateSearchTags();
		});

		// c.fill = GridBagConstraints.HORIZONTAL;
		// c.weighty = 2;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 0;
		c.gridwidth = 2;
		panel.add(label, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.weightx = .5;
		panel.add(switchDirButton, c);

		return panel;
	}

	private void SetUpTags() {
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
					DbManager.deleteTagFromFiles(fileTagsList.getSelectedValue(), ListingPanel.selectedFiles);
					updateFileTags();
					updateResults();
				}
			}
		});
		// jtabbedpane holding apply and file panes
		tabbedPane.addTab("Searching for tags", appliedTagsPane);
		tabbedPane.addTab("Selected files tags", fileTagsPane);
	}

	private JScrollPane SetUpSearch() {
		JPanel panel = new JPanel();
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
					} else {
						DbManager.addTagToFiles(ListingPanel.selectedFiles, searchedTagsList.getSelectedValue());
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
		panel.add(scrollPane);
		return scrollPane;
	}

	boolean isManagingTags = false;

	private JPanel SetUpControlPanel() {
		// control//
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(1, 2));
		JButton manageTagsButton = new JButton("Manage tags");
		manageTagsButton.addActionListener(e -> {
			isManagingTags = !isManagingTags;
			if (isManagingTags) {
				manageTagsButton.setText("View files");
				gui.changeRightView(1);
			} else {
				manageTagsButton.setText("Manage tags");
				gui.changeRightView(0);
			}
		});

		JButton detailToggleButton = new JButton("Detail view");
		detailToggleButton.addActionListener(e -> {

			isDetailView = !isDetailView;
			for (FilePanel filePanel : listingPanel.activeFilePanels) {
				if (isDetailView)
					filePanel.showDetailView();
				else
					filePanel.showImageView();
			}

			if (isDetailView) {
				detailToggleButton.setText("Image view");
			} else {
				detailToggleButton.setText("Detail view");
			}
		});

		controlPanel.add(manageTagsButton);
		controlPanel.add(detailToggleButton);

		return controlPanel;
	}

	public static void updateFileTags() {
		Map<String, Integer> sharedTagsCounted = new HashMap<>();
		sharedTagsCounted = DbManager.findTagsWithCount(ListingPanel.selectedFiles);
		List<String> selectedFilesTags = new ArrayList<>();
		for (FileItem fileItem : ListingPanel.selectedFiles) {
			for (String tag : fileItem.tagsList) {
				if (!selectedFilesTags.contains(tag)) {
					selectedFilesTags.add(tag);
				}
			}
		}
		// in selectedfilestags, remove its entries which include a tag from shared tags
		// selectedFilesTags.removeAll(sharedTags);
		UpdateSelectedHighlight(sharedTagsCounted);

		fileTagsList.setListData(sharedTagsCounted.keySet().toArray(new String[0]));
	}

	private static void UpdateSelectedHighlight(Map<String, Integer> sharedTagsCounted) {
		System.out.println(sharedTagsCounted.toString());
		for (FilePanel filePanel : listingPanel.activeFilePanels) {
			HashMap<String, TagMatchState> highlightMap = new HashMap<>();
			List<String> fileTags = filePanel.fileItem.tagsList;
			for (String tag : fileTags) {
				if (sharedTagsCounted.containsKey(tag)) {
					if (sharedTagsCounted.get(tag) == ListingPanel.selectedFiles.size()) {
						highlightMap.put(tag, TagMatchState.Full);
					} else {
						// if shared>1 partial
						if (sharedTagsCounted.get(tag) > 1) {
							highlightMap.put(tag, TagMatchState.Partial);
						} else {
							// if fileitems selected
							if (filePanel.fileItem.isSelected)
								highlightMap.put(tag, TagMatchState.Unique);
							else {
								highlightMap.put(tag, TagMatchState.Relative);
							}
						}
					}
				} else {
					highlightMap.put(tag, TagMatchState.None);
				}
			}

			filePanel.setHighlight(highlightMap);
		}
		appliedTagsList.repaint();
		fileTagsList.repaint();
	}

	private static void updateResults() {
		List<FileItem> fileItems;
		List<String> tags = new ArrayList<>();
		for (int i = 0; i < appliedTagsList.getModel().getSize(); i++) {
			tags.add(appliedTagsList.getModel().getElementAt(i));
		}

		if (appliedTagsList.getModel().getSize() == 0) {
			fileItems = DbManager.getFileItems();
		} else {
			fileItems = DbManager.findFiles(tags);
		}

		// Build a lookup of previously selected items by id
		Map<Integer, FileItem> previouslySelectedById = ListingPanel.selectedFiles.stream()
				.collect(Collectors.toMap(f -> f.id, f -> f));

		// Reset the selectedFiles list before repopulating
		ListingPanel.selectedFiles.clear();

		listingPanel.contentPanel.removeAll();
		for (FileItem fileItem : fileItems) {
			FileItem old = previouslySelectedById.get(fileItem.id);
			if (old != null) {
				fileItem.isSelected = old.isSelected;
				ListingPanel.changeSelectedFiles(fileItem, 1);
			}

			FilePanel tempFilePanel = new FilePanel(fileItem);
			if (isDetailView)
				tempFilePanel.showDetailView();
			else
				tempFilePanel.showImageView();
			listingPanel.addPanel(tempFilePanel);
		}

		// listingPanel.layoutPanels();
		// assign listingpanels activefilepanels to correct this.fileitems.filepanels
		listingPanel.activeFilePanels.clear();
		for (FileItem fileItem : fileItems) {
			listingPanel.activeFilePanels.add(fileItem.filePanel);
		}

		listingPanel.updateImageScales();
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

	public static void updateSearchTags() {
		String search = tagSearchField.getText();
		List<String> foundTags = DbManager.findTags(search);
		if (foundTags == null) {
			System.out.println("No tag found for search: " + search);
		}
		searchedTagsList.setListData(foundTags.toArray(new String[0]));
		CleanFrame(frame);
		// fsd -> xx
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
		frame.pack();
		frame.requestFocus();
	}

	static void CleanFrame(JFrame frame) {
		// frame.getContentPane().removeAll();
		// frame.pack();
		frame.repaint();
		frame.revalidate();
		// System.out.println("Cleaned frame");
	}

	static void CleanPanel(JPanel panel) {
		panel.removeAll();
		panel.repaint();
		panel.revalidate();
		// System.out.println("Cleaned panel");
	}

}
