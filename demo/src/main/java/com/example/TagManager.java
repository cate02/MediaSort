package com.example;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class TagManager extends JPanel {
	JTextField tagSearchField = new JTextField(20);
	JTable tagTable = new JTable();
	DefaultTableModel tagTableModel;
	String selectedTag = null;

	JTextField aliasSearchField = new JTextField(20);
	JTable aliasTable = new JTable();
	DefaultTableModel aliasTableModel;
	String selectedAlias = null;

	JLabel selectedTagLabel = new JLabel("Tag selected: ");
	JLabel selectedAliasLabel = new JLabel("Alias selected: ");

	TagManager() {
		try {
			String tempConnDir = DbManager.tempConnDir;
			Connection conn = DriverManager.getConnection(tempConnDir);

		} catch (Exception e) {
			e.printStackTrace();
		}
		DbManager.tagManager = this;
		setUpGUI();
		updateTagResults();
		updateAliasResults();
		updateConnectionResults();

	}

	public void refreshTagManager() {
		removeAll();
		revalidate();
		repaint();
		setUpGUI();
		updateTagResults();
		updateAliasResults();
		updateConnectionResults();
	}

	void setUpGUI() {
		JPanel tagPanel = createTagPanel();
		JPanel aliasPanel = createAliasPanel();
		JPanel connectionPanel = createConnectionPanel();

		// tagpanel above other 2
		setLayout(new GridLayout(2, 1));
		add(tagPanel);
		JPanel lowerPanel = new JPanel(new GridLayout(1, 2));
		lowerPanel.add(aliasPanel);
		lowerPanel.add(connectionPanel);
		add(lowerPanel);
		// pack();
		// setSize(700, 500);
		setVisible(true);
	}

	JPanel createTagPanel() {
		JPanel tagPanel = new JPanel(new BorderLayout());
		tagPanel.setBorder(BorderFactory.createTitledBorder("Tags"));

		tagTableModel = new DefaultTableModel(new Object[] { "Tag", "Files", "ID" }, 0) {
			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		tagTable.setModel(tagTableModel);
		tagTable.setRowHeight(24);
		tagTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tagTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && tagTable.getSelectedRow() >= 0) {
				String tag = (String) tagTable.getValueAt(tagTable.getSelectedRow(), 0);
				setSelectedTag(tag);
				// System.out.println("Selected tag: " + tag);
			}
		});

		JPanel tagSearchPanel = new JPanel(new BorderLayout(5, 5));
		tagSearchPanel.add(new JLabel("Search tag:"), BorderLayout.WEST);
		tagSearchPanel.add(tagSearchField, BorderLayout.CENTER);
		tagPanel.add(tagSearchPanel, BorderLayout.NORTH);

		JScrollPane tagScroll = setUpTagScroll();
		tagPanel.add(tagScroll, BorderLayout.CENTER);

		JPanel tagActions = new JPanel();
		tagActions.add(selectedTagLabel);

		JButton createTagBtn = new JButton("Create new tag");
		createTagBtn.addActionListener(e -> {
			String newTag = tagSearchField.getText();
			if (!isValidName(newTag))
				return;
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(this, "Create new tag: " + newTag + "?", "Confirm Tag Creation",
					JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.createTag(newTag) == 1) {
					JOptionPane.showMessageDialog(this, "Tag created: " + newTag);
					tagSearchField.setText("");
					updateTagResults();
					InfoPanel.updateSearchTags();
				} else {
					JOptionPane.showMessageDialog(this, "Tag already exists: " + newTag);
				}
			}
		});
		tagActions.add(createTagBtn);
		JButton deleteTagBtn = new JButton("Delete selected tag");
		deleteTagBtn.addActionListener(e -> {
			if (selectedTag == null) {
				JOptionPane.showMessageDialog(this, "No tag selected to delete.");
				return;
			}
			confirmDeleteTag();
			/*
			 * // confirm dialog int confirm = JOptionPane.showConfirmDialog(this,
			 * "Delete tag: " + selectedTag + "?", "Confirm Tag Deletion",
			 * JOptionPane.YES_NO_OPTION); if (confirm == JOptionPane.YES_OPTION) { if
			 * (DbManager.deleteTag(selectedTag) == 1) { JOptionPane.showMessageDialog(this,
			 * "Tag deleted: " + selectedTag); setSelectedTag(null); updateTagResults(); }
			 * else { JOptionPane.showMessageDialog(this, "Error deleting tag: " +
			 * selectedTag); } }
			 */
		});
		tagActions.add(deleteTagBtn);
		tagPanel.add(tagActions, BorderLayout.SOUTH);
		return tagPanel;
	}

	JScrollPane setUpTagScroll() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(tagTable);

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
		if (foundTags == null)
			foundTags = java.util.Collections.emptyList();
		tagTableModel.setRowCount(0);
		for (String tag : foundTags) {
			tagTableModel.addRow(new Object[] { tag, DbManager.getFilesForTag(tag).size(), DbManager.findTagId(tag) });
		}

	}

	JPanel createAliasPanel() {
		JPanel aliasPanel = new JPanel(new BorderLayout());
		aliasPanel.setBorder(BorderFactory.createTitledBorder("Aliases"));

		aliasTableModel = new DefaultTableModel(new Object[] { "Applied", "Alias", "ID" }, 0) {
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
		aliasTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

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
				JOptionPane.showMessageDialog(this, "No tag selected to add/remove alias to/from.");
				return;
			}

			// Perform DB updates
			if (checked) {
				DbManager.addAliasToTag(selectedTag, alias);
				// System.out.println("Added alias: " + alias + " to tag: " + selectedTag);
			} else {
				DbManager.removeAliasFromTag(selectedTag, alias);
				// System.out.println("Removed alias: " + alias + " from tag: " + selectedTag);
			}
		});

		aliasTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && aliasTable.getSelectedRow() >= 0) {
				String alias = (String) aliasTable.getValueAt(aliasTable.getSelectedRow(), 1);
				setSelectedAlias(alias);
				// System.out.println("Selected alias: " + alias);
			}
		});

		aliasPanel.add(new JScrollPane(aliasTable), BorderLayout.CENTER);

		JPanel aliasSearchPanel = new JPanel(new BorderLayout(5, 5));
		aliasSearchPanel.add(new JLabel("Search alias:"), BorderLayout.WEST);
		aliasSearchPanel.add(aliasSearchField, BorderLayout.CENTER);

		aliasPanel.add(aliasSearchPanel, BorderLayout.NORTH);

		JScrollPane aliasScroll = setUpAliasScroll();
		aliasPanel.add(aliasScroll, BorderLayout.CENTER);

		JPanel aliasActions = new JPanel();
		// let panel components resize to fit space
		aliasActions.setLayout(new GridLayout(0, 1));
		aliasActions.add(selectedAliasLabel);
		JButton createAliasBtn = new JButton("Create new alias");
		createAliasBtn.addActionListener(e -> {
			String newAlias = aliasSearchField.getText();
			if (!isValidName(newAlias))
				return;
			// confirm dialog
			int confirm = JOptionPane.showConfirmDialog(this, "Create new alias: " + newAlias, "Confirm alias creation",
					JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				if (DbManager.createAlias(newAlias) == 1) {
					JOptionPane.showMessageDialog(this, "Alias created: " + newAlias);
					aliasSearchField.setText("");
					updateAliasResults();
				} else {
					JOptionPane.showMessageDialog(this, "Alias already exists: " + newAlias);
				}
			}
		});
		aliasActions.add(createAliasBtn);
		JButton deleteAliasBtn = new JButton("Delete selected alias");
		deleteAliasBtn.addActionListener(e -> {
			if (selectedAlias == null) {
				JOptionPane.showMessageDialog(this, "No alias selected to delete.");
				return;
			}
			confirmDeleteAlias();
			/*
			 * // confirm dialog int confirm = JOptionPane.showConfirmDialog(this,
			 * "Delete alias: " + selectedAlias + "?", "Confirm Alias Deletion",
			 * JOptionPane.YES_NO_OPTION); if (confirm == JOptionPane.YES_OPTION) { if
			 * (DbManager.deleteAlias(selectedAlias) == 1) {
			 * JOptionPane.showMessageDialog(this, "Alias deleted: " + selectedAlias);
			 * setSelectedAlias(null); updateAliasResults(); } else {
			 * JOptionPane.showMessageDialog(this, "Error deleting alias: " +
			 * selectedAlias); } }
			 */
		});
		aliasActions.add(deleteAliasBtn);
		aliasPanel.add(aliasActions, BorderLayout.SOUTH);
		return aliasPanel;
	}

	void confirmDeleteAlias() {
		// create panel with yes no buttons, that shows a scrollable list of all tags
		// this alias is connected to
		List<String> connectedTags = DbManager.getTagsForAlias(selectedAlias);
		JPanel panel = new JPanel(new BorderLayout());
		JTextField infoField = new JTextField(
				"Alias: " + selectedAlias + " connected to " + connectedTags.size() + " tags. Delete anyway?");
		infoField.setEditable(false);
		panel.add(infoField, BorderLayout.NORTH);
		JList<String> tagList = new JList<>(connectedTags.toArray(new String[0]));
		JScrollPane scrollPane = new JScrollPane(tagList);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		panel.add(scrollPane, BorderLayout.CENTER);
		int confirm = JOptionPane.showConfirmDialog(this, panel, "Confirm Alias Deletion", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			if (DbManager.deleteAlias(selectedAlias) == 1) {
				JOptionPane.showMessageDialog(this, "Alias deleted: " + selectedAlias);
				setSelectedAlias(null);
				updateAliasResults();
			} else {
				JOptionPane.showMessageDialog(this, "Error deleting alias: " + selectedAlias);
			}
		}

	}

	void confirmDeleteTag() {
		// create panel with yes no buttons, that shows a scrollable list of all files
		// this tag is connected to
		List<String> connectedFiles = DbManager.getFilesForTag(selectedTag);
		JPanel panel = new JPanel(new BorderLayout());
		JTextField infoField = new JTextField(
				"Tag: " + selectedTag + " connected to " + connectedFiles.size() + " files. Delete anyway?");
		infoField.setEditable(false);
		panel.add(infoField, BorderLayout.NORTH);
		JList<String> fileList = new JList<>(connectedFiles.toArray(new String[0]));
		JScrollPane scrollPane = new JScrollPane(fileList);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		panel.add(scrollPane, BorderLayout.CENTER);
		int confirm = JOptionPane.showConfirmDialog(this, panel, "Confirm Tag Deletion", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			if (DbManager.deleteTag(selectedTag) == 1) {
				JOptionPane.showMessageDialog(this, "Tag deleted: " + selectedTag);
				setSelectedTag(null);
				updateTagResults();
				InfoPanel.updateSearchTags();
			} else {
				JOptionPane.showMessageDialog(this, "Error deleting tag: " + selectedTag);
			}
		}
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

		if (aliasesMap == null)
			aliasesMap = java.util.Collections.emptyMap();

		// clear and repopulate rows (keeps listeners intact)
		aliasTableModel.setRowCount(0);
		for (Map.Entry<String, Boolean> entry : aliasesMap.entrySet()) {
			aliasTableModel
					.addRow(new Object[] { entry.getValue(), entry.getKey(), DbManager.findAliasId(entry.getKey()) });
		}

		// rows with true boolean first
		aliasTableModel.getDataVector().sort((a, b) -> {
			Boolean aChecked = (Boolean) ((java.util.Vector<?>) a).get(0);
			Boolean bChecked = (Boolean) ((java.util.Vector<?>) b).get(0);
			return bChecked.compareTo(aChecked); // true first
		});
		// CleanFrame(frame);
	}

	// fields you need in your class
	private JTable connTable = new JTable();
	private DefaultTableModel connTableModel;
	private JTextField connSearchField = new JTextField(15);
	private JLabel selectedConnLabel = new JLabel("Connection selected: ");
	private String selectedConn = null;

	JPanel createConnectionPanel() {
		JPanel connPanel = new JPanel(new BorderLayout());
		connPanel.setBorder(BorderFactory.createTitledBorder("Connections"));

		// table model
		connTableModel = new DefaultTableModel(new Object[] { "Applied", "Connection", "ID" }, 0) {
			@Override
			public Class<?> getColumnClass(int c) {
				return c == 0 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int r, int c) {
				// checkboxes only editable if a tag is selected
				return c == 0 && selectedTag != null;
			}
		};

		// table setup
		connTable.setModel(connTableModel);
		connTable.setRowHeight(24);
		connTable.putClientProperty("terminateEditOnFocusLost", true);
		connTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// listen for checkbox toggles
		connTableModel.addTableModelListener(e -> {
			if (e.getColumn() != 0 || e.getFirstRow() < 0)
				return;

			int row = e.getFirstRow();
			Boolean checked = (Boolean) connTableModel.getValueAt(row, 0);
			String conn = (String) connTableModel.getValueAt(row, 1);

			setSelectedConn(conn);

			if (selectedTag == null) {
				// just ignore if no tag selected (checkbox editing disabled anyway)
				return;
			}

			if (checked) {
				DbManager.addConnToTag(selectedTag, conn);
				// System.out.println("Added connection: " + conn + " to tag: " + selectedTag);
			} else {
				DbManager.removeConnFromTag(selectedTag, conn);
				// System.out.println("Removed connection: " + conn + " from tag: " +
				// selectedTag);
			}
		});

		// listen for row selection (column 1 clicks)
		connTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && connTable.getSelectedRow() >= 0) {
				String conn = (String) connTable.getValueAt(connTable.getSelectedRow(), 1);
				setSelectedConn(conn);
				// System.out.println("Selected connection: " + conn);
			}
		});

		// search bar
		JPanel connSearchPanel = new JPanel(new BorderLayout(5, 5));
		connSearchPanel.add(new JLabel("Search connection:"), BorderLayout.WEST);
		connSearchPanel.add(connSearchField, BorderLayout.CENTER);
		connPanel.add(connSearchPanel, BorderLayout.NORTH);

		// table scroll
		JScrollPane connScroll = setUpConnectionScroll();
		connPanel.add(connScroll, BorderLayout.CENTER);

		// bottom actions / status
		JPanel connActions = new JPanel(new BorderLayout(5, 5));
		connActions.add(selectedConnLabel);
		connPanel.add(connActions, BorderLayout.SOUTH);

		return connPanel;
	}

	JScrollPane setUpConnectionScroll() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(connTable);

		connSearchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateConnectionResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateConnectionResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateConnectionResults();
			}
		});

		return scrollPane;
	}

	void updateConnectionResults() {
		// System.out.println("Updating connection results for tag: " + selectedTag);
		String search = connSearchField.getText();
		Map<String, Boolean> connsMap = DbManager.findConnections(selectedTag, search);

		if (connsMap == null)
			connsMap = java.util.Collections.emptyMap();

		// clear + repopulate rows
		connTableModel.setRowCount(0);
		for (Map.Entry<String, Boolean> entry : connsMap.entrySet()) {
			connTableModel.addRow(
					new Object[] { entry.getValue(), entry.getKey(), DbManager.findConnectionId(entry.getKey()) });
		}

		// rows with true boolean first
		connTableModel.getDataVector().sort((a, b) -> {
			Boolean aChecked = (Boolean) ((java.util.Vector<?>) a).get(0);
			Boolean bChecked = (Boolean) ((java.util.Vector<?>) b).get(0);
			return bChecked.compareTo(aChecked); // true first
		});

		// CleanFrame(frame);
	}

	// helper
	private void setSelectedConn(String conn) {
		this.selectedConn = conn;
		selectedConnLabel.setText("Connection selected: " + (conn != null ? conn : ""));
	}

	void setSelectedTag(String selectedTag) {
		this.selectedTag = selectedTag;
		selectedTagLabel.setText("Tag selected: " + selectedTag);
		updateAliasResults();
		updateConnectionResults();

	}

	void setSelectedAlias(String selectedAlias) {
		this.selectedAlias = selectedAlias;
		selectedAliasLabel.setText("Alias selected: " + selectedAlias);
	}

	static void CleanFrame(JFrame frame) {
		// getContentPane().removeAll();
		// repaint();
		// revalidate();
		// System.out.println("Cleaned frame");
	}

	boolean isValidName(String name) {
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Name can not be empty");
			return false;
		}
		if (!DbManager.isUniqueTagAlias(name)) {
			JOptionPane.showMessageDialog(this, "Name already exists as tag or alias");
			return false;
		}
		return true;
	}

}
