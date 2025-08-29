package com.example;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class TagManager {
	JFrame frame = new JFrame();
	JTextField tagSearchField = new JTextField(20);
	JList<String> searchedTagsList = new JList<>();
	String selectedTag = null;

	JTextField aliasSearchField = new JTextField(20);
	JList<String> searchedAliasesList = new JList<>();
	String selectedAlias = null;

	public static void main(String[] args) {
		TagManager tagManager = new TagManager();
	}

	TagManager() {
		try {
			DbManager dbManager = new DbManager();
			String tempConnDir = DbManager.tempConnDir;
			Connection conn = DriverManager.getConnection(tempConnDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		setUpGUI();
		updateTagResults();
		updateAliasResults();
	}

	void setUpGUI() {
		JPanel tagPanel = createTagPanel();
		JPanel aliasPanel = createAliasPanel();
		JPanel connectionPanel = createConnectionPanel();

		// tagpanel above other 2
		frame.setLayout(new BorderLayout());
		frame.add(tagPanel, BorderLayout.NORTH);
		JPanel lowerPanel = new JPanel(new GridLayout(1, 2));
		lowerPanel.add(aliasPanel);
		lowerPanel.add(connectionPanel);
		frame.add(lowerPanel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setSize(700, 500);
		frame.setVisible(true);
	}

	JPanel createTagPanel() {
		JPanel tagPanel = new JPanel(new BorderLayout());
		tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));

		JPanel tagSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tagSearchPanel.add(new JLabel("Search tag:"));
		tagSearchPanel.add(tagSearchField);
		tagPanel.add(tagSearchPanel, BorderLayout.NORTH);

		JScrollPane tagScroll = setUpTagScroll();
		tagPanel.add(tagScroll, BorderLayout.CENTER);

		JPanel tagActions = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel selectedTagLabel = new JLabel("Tag selected: ");
		tagActions.add(selectedTagLabel);

		// Dynamically update label when selectedTag changes
		searchedTagsList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				selectedTag = searchedTagsList.getSelectedValue();
				selectedTagLabel.setText("Tag selected: " + (selectedTag != null ? selectedTag : ""));
				updateAliasResults();
			}
		});
		JButton createTagBtn = new JButton("Create new tag");
		createTagBtn.addActionListener(e -> {
			String newTag = tagSearchField.getText();
			// confirm dialog
			int confirm = javax.swing.JOptionPane.showConfirmDialog(frame, "Create new tag: " + newTag + "?",
					"Confirm Tag Creation", javax.swing.JOptionPane.YES_NO_OPTION);
			if (confirm == javax.swing.JOptionPane.YES_OPTION) {
				if (DbManager.createTag(newTag) == 1) {
					javax.swing.JOptionPane.showMessageDialog(frame, "Tag created: " + newTag);
					updateTagResults();
				} else {
					javax.swing.JOptionPane.showMessageDialog(frame, "Tag already exists: " + newTag);
				}
			}
		});
		tagActions.add(createTagBtn);
		JButton deleteTagBtn = new JButton("Delete selected tag");
		deleteTagBtn.addActionListener(e -> {
			if (selectedTag == null) {
				javax.swing.JOptionPane.showMessageDialog(frame, "No tag selected to delete.");
				return;
			}
			// confirm dialog
			int confirm = javax.swing.JOptionPane.showConfirmDialog(frame, "Delete tag: " + selectedTag + "?",
					"Confirm Tag Deletion", javax.swing.JOptionPane.YES_NO_OPTION);
			if (confirm == javax.swing.JOptionPane.YES_OPTION) {
				if (DbManager.deleteTag(selectedTag) == 1) {
					javax.swing.JOptionPane.showMessageDialog(frame, "Tag deleted: " + selectedTag);
					selectedTag = null;
					selectedTagLabel.setText("Tag selected: ");
					updateTagResults();
				} else {
					javax.swing.JOptionPane.showMessageDialog(frame, "Error deleting tag: " + selectedTag);
				}
			}
		});
		tagActions.add(deleteTagBtn);
		tagPanel.add(tagActions, BorderLayout.SOUTH);
		return tagPanel;
	}

	JScrollPane setUpTagScroll() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(searchedTagsList);
		searchedTagsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		/*
		 * searchedTagsList.addMouseListener(new MouseAdapter() {
		 * 
		 * @Override public void mouseClicked(MouseEvent e) { selectedTag =
		 * searchedTagsList.getSelectedValue(); System.out.println("Selected tag: " +
		 * selectedTag); } });
		 */
		/*
		 * searchedTagsList.addMouseListener(new MouseAdapter() {
		 *
		 * @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() >=
		 * 2) { if (tabbedPane.getSelectedIndex() == 0) { changeJList(appliedTagsList,
		 * searchedTagsList.getSelectedValue(), 1); } else {
		 * DbManager.addTagToFiles(ListingPanel.selectedFiles,
		 * searchedTagsList.getSelectedValue()); updateFileTags(); // add tag to
		 * selected files } updateResults(); } } });
		 */
		tagSearchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateTagResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateTagResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateTagResults();
			}
		});
		return scrollPane;
	}

	void updateTagResults() {
		String search = tagSearchField.getText();
		List<String> foundTags = DbManager.findTags(search);
		if (foundTags == null) {
			System.out.println("No tag found for search: " + search);
		}
		searchedTagsList.setListData(foundTags.toArray(new String[0]));
		CleanFrame(frame);
	}

	JPanel createAliasPanel() {
		JPanel aliasPanel = new JPanel(new BorderLayout());
		aliasPanel.setBorder(BorderFactory.createTitledBorder("Aliases"));

		JPanel aliasSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		aliasSearchPanel.add(new JLabel("Search alias:"));
		aliasSearchPanel.add(aliasSearchField);
		aliasPanel.add(aliasSearchPanel, BorderLayout.NORTH);

		JScrollPane aliasScroll = setUpAliasScroll();
		aliasPanel.add(aliasScroll, BorderLayout.CENTER);

		// JTable aliasTable = makeCheckboxTable(new String[] { "Alias1", "Alias2",
		// "Alias3" });
		// aliasPanel.add(new JScrollPane(aliasTable), BorderLayout.CENTER);

		JPanel aliasActions = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel selectedAliasLabel = new JLabel("Alias selected: ");
		aliasActions.add(selectedAliasLabel);
		searchedAliasesList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				selectedAlias = searchedAliasesList.getSelectedValue();
				selectedAliasLabel.setText("Alias selected: " + (selectedAlias != null ? selectedAlias : ""));
			}
		});
		aliasActions.add(new JButton("Create new alias"));
		aliasActions.add(new JButton("Delete selected"));
		aliasPanel.add(aliasActions, BorderLayout.SOUTH);

		return aliasPanel;
	}

	JScrollPane setUpAliasScroll() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(searchedAliasesList);
		searchedAliasesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		searchedAliasesList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectedAlias = searchedAliasesList.getSelectedValue();
				System.out.println("Selected alias: " + selectedAlias);
			}
		});
		aliasSearchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateAliasResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateAliasResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateAliasResults();
			}
		});

		return scrollPane;
	}

	void updateAliasResults() {
		String search = aliasSearchField.getText();
		List<String> foundAliases = DbManager.findAliases(search);
		if (foundAliases == null) {
			System.out.println("No alias found for search: " + search);
		}
		searchedAliasesList.setListData(foundAliases.toArray(new String[0]));
		CleanFrame(frame);
	}

	JPanel createConnectionPanel() {
		JPanel connPanel = new JPanel(new BorderLayout());
		connPanel.setBorder(BorderFactory.createTitledBorder("Connections"));

		JPanel connSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
		connSearch.add(new JLabel("Search connection:"));
		connSearch.add(new JTextField(15));
		connPanel.add(connSearch, BorderLayout.NORTH);

		JTable connTable = makeCheckboxTable(new String[] { "Conn1", "Conn2", "Conn3" });
		connPanel.add(new JScrollPane(connTable), BorderLayout.CENTER);
		return connPanel;
	}

	private JTable makeCheckboxTable(String[] items) {

		DefaultTableModel model = new DefaultTableModel(new Object[] { "", "Name" }, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				return col == 0; // only checkbox editable
			}
		};
		for (String item : items) {
			model.addRow(new Object[] { false, item });
		}
		JTable table = new JTable(model);
		table.setRowHeight(25);
		return table;
	}

	static void CleanFrame(JFrame frame) {
		// frame.getContentPane().removeAll();
		// frame.pack();
		frame.repaint();
		frame.revalidate();
		// System.out.println("Cleaned frame");
	}

}
