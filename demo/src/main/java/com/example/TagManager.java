package com.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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
	JTable aliasTable = new JTable();
	DefaultTableModel aliasTableModel;
	String selectedAlias = null;

	JLabel selectedTagLabel = new JLabel("Tag selected: ");
	JLabel selectedAliasLabel = new JLabel("Alias selected: ");

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
		tagActions.add(selectedTagLabel);

		// Dynamically update label when selectedTag changes
		searchedTagsList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				setSelectedTag(searchedTagsList.getSelectedValue());
			}
		});
		searchedTagsList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				if (value instanceof String str) {
					// if empty, replace with a space so it takes up visible height
					if (str.isEmpty()) {
						value = " ";
					}
				}

				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});

		JButton createTagBtn = new JButton("Create new tag");
		createTagBtn.addActionListener(e -> {
			String newTag = tagSearchField.getText();
			if (!isValidName(newTag))
				return;
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(frame, "Create new tag: " + newTag + "?",
					"Confirm Tag Creation", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.createTag(newTag) == 1) {
					JOptionPane.showMessageDialog(frame, "Tag created: " + newTag);
					updateTagResults();
				} else {
					JOptionPane.showMessageDialog(frame, "Tag already exists: " + newTag);
				}
			}
		});
		tagActions.add(createTagBtn);
		JButton deleteTagBtn = new JButton("Delete selected tag");
		deleteTagBtn.addActionListener(e -> {
			if (selectedTag == null) {
				JOptionPane.showMessageDialog(frame, "No tag selected to delete.");
				return;
			}
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(frame, "Delete tag: " + selectedTag + "?",
					"Confirm Tag Deletion", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.deleteTag(selectedTag) == 1) {
					JOptionPane.showMessageDialog(frame, "Tag deleted: " + selectedTag);
					setSelectedTag(null);
					updateTagResults();
				} else {
					JOptionPane.showMessageDialog(frame, "Error deleting tag: " + selectedTag);
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

		aliasTableModel = new DefaultTableModel(new Object[] { "Selected", "Alias" }, 0) {
			@Override
			public Class<?> getColumnClass(int c) {
				return c == 0 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int r, int c) {
				return c == 0 && selectedTag != null;
			}
		};

		aliasTable.setModel(aliasTableModel);
		aliasTable.setRowHeight(24);
		aliasTable.putClientProperty("terminateEditOnFocusLost", true); // commit edits reliably

		aliasTableModel.addTableModelListener(e -> {
			// Only respond to checkbox column changes
			if (e.getColumn() != 0 || e.getFirstRow() < 0)
				return;

			int row = e.getFirstRow();
			Boolean checked = (Boolean) aliasTableModel.getValueAt(row, 0);
			String alias = (String) aliasTableModel.getValueAt(row, 1);

			// Update selected alias for convenience
			setSelectedAlias(alias);

			if (selectedTag == null) {
				// revert without recursion: use invokeLater to defer
				SwingUtilities.invokeLater(() -> aliasTableModel.setValueAt(!checked, row, 0));
				JOptionPane.showMessageDialog(frame, "No tag selected to add/remove alias to/from.");
				return;
			}

			// Perform DB updates
			if (checked) {
				DbManager.addAliasToTag(selectedTag, alias);
				System.out.println("Added alias: " + alias + " to tag: " + selectedTag);
			} else {
				DbManager.removeAliasFromTag(selectedTag, alias);
				System.out.println("Removed alias: " + alias + " from tag: " + selectedTag);
			}
		});

		aliasTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && aliasTable.getSelectedRow() >= 0) {
				String alias = (String) aliasTable.getValueAt(aliasTable.getSelectedRow(), 1);
				setSelectedAlias(alias);
				System.out.println("Selected alias: " + alias);
			}
		});

		aliasPanel.add(new JScrollPane(aliasTable), BorderLayout.CENTER);

		JPanel aliasSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		aliasSearchPanel.add(new JLabel("Search alias:"));
		aliasSearchPanel.add(aliasSearchField);
		aliasPanel.add(aliasSearchPanel, BorderLayout.NORTH);

		JScrollPane aliasScroll = setUpAliasScroll();
		aliasPanel.add(aliasScroll, BorderLayout.CENTER);

		JPanel aliasActions = new JPanel(new FlowLayout(FlowLayout.CENTER));
		// let panel components resize to fit space
		aliasActions.setLayout(new GridLayout(0, 1));
		aliasActions.add(selectedAliasLabel);
		JButton createAliasBtn = new JButton("Create new alias");
		createAliasBtn.addActionListener(e -> {
			String newAlias = aliasSearchField.getText();
			if (!isValidName(newAlias))
				return;
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(frame, "Create new alias: " + newAlias,
					"Confirm alias creation", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.createAlias(newAlias) == 1) {
					JOptionPane.showMessageDialog(frame, "Alias created: " + newAlias);
					updateAliasResults();
				} else {
					JOptionPane.showMessageDialog(frame, "Alias already exists: " + newAlias);
				}
			}
		});
		aliasActions.add(createAliasBtn);
		JButton deleteAliasBtn = new JButton("Delete selected alias");
		deleteAliasBtn.addActionListener(e -> {
			if (selectedAlias == null) {
				JOptionPane.showMessageDialog(frame, "No alias selected to delete.");
				return;
			}
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(frame, "Delete alias: " + selectedAlias + "?",
					"Confirm Alias Deletion", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.deleteAlias(selectedAlias) == 1) {
					JOptionPane.showMessageDialog(frame, "Alias deleted: " + selectedAlias);
					setSelectedAlias(null);
					updateAliasResults();
				} else {
					JOptionPane.showMessageDialog(frame, "Error deleting alias: " + selectedAlias);
				}
			}
		});
		aliasActions.add(deleteAliasBtn);
		aliasPanel.add(aliasActions, BorderLayout.SOUTH);
		return aliasPanel;
	}

	JScrollPane setUpAliasScroll() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(aliasTable);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
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
		Map<String, Boolean> aliasesMap = DbManager.findAliases(selectedTag, search);

		if (selectedTag == null) {
			// optional: clear table if no tag
			// aliasModel.setRowCount(0);
			// CleanFrame(frame);
			// return;
		}
		if (aliasesMap == null)
			aliasesMap = java.util.Collections.emptyMap();

		// clear and repopulate rows (keeps listeners intact)
		aliasTableModel.setRowCount(0);
		for (Map.Entry<String, Boolean> entry : aliasesMap.entrySet()) {
			aliasTableModel.addRow(new Object[] { entry.getValue(), entry.getKey() });
		}
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
		// TODO : scrap
		DefaultTableModel model = new DefaultTableModel(new Object[] { "", "Alias" }, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				return col == 0 && selectedTag != null;
			}
		};
		for (String item : items) {
			model.addRow(new Object[] { false, item });
		}
		JTable table = new JTable(model);
		table.setRowHeight(25);
		return table;
	}

	void setSelectedTag(String selectedTag) {
		this.selectedTag = selectedTag;
		selectedTagLabel.setText("Tag selected: " + selectedTag);
		updateAliasResults();

	}

	void setSelectedAlias(String selectedAlias) {
		this.selectedAlias = selectedAlias;
		selectedAliasLabel.setText("Alias selected: " + selectedAlias);
	}

	static void CleanFrame(JFrame frame) {
		// frame.getContentPane().removeAll();
		frame.repaint();
		frame.revalidate();
		// System.out.println("Cleaned frame");
	}

	boolean isValidName(String name) {
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Name can not be empty");
			return false;
		}
		return true;
	}

}
