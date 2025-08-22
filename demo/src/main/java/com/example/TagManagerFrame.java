package com.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class TagManagerFrame extends JFrame {
	private Connection conn;

	public TagManagerFrame(Connection conn) {
		this.conn = conn;
		setTitle("Tag Manager");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.add("Tags", createTagPanel());
		tabbedPane.add("Aliases", createAliasPanel());
		tabbedPane.add("Connections", createConnectionPanel());
		tabbedPane.add("Overview", createOverviewPanel());
		tabbedPane.add("Unified Tags", createUnifiedTagPanel());

		add(tabbedPane);
		setLocationRelativeTo(null);
	}

	// ------------------ TAG PANEL ------------------
	private JPanel createTagPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		DefaultTableModel model = new DefaultTableModel(new String[] { "ID", "Tag" }, 0);
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		loadTags(model);

		JPanel buttons = new JPanel();
		JButton addBtn = new JButton("Add");
		JButton renameBtn = new JButton("Rename");
		JButton deleteBtn = new JButton("Delete");

		buttons.add(addBtn);
		buttons.add(renameBtn);
		buttons.add(deleteBtn);

		addBtn.addActionListener(e -> {
			String name = JOptionPane.showInputDialog(this, "Enter new tag:");
			if (name != null && !name.isBlank()) {
				try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tags(tag) VALUES(?)")) {
					ps.setString(1, name.trim());
					ps.executeUpdate();
					loadTags(model);
				} catch (SQLException ex) {
					showError(ex);
				}
			}
		});

		renameBtn.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				int id = (int) model.getValueAt(row, 0);
				String oldName = (String) model.getValueAt(row, 1);
				String newName = JOptionPane.showInputDialog(this, "Rename tag:", oldName);
				if (newName != null && !newName.isBlank()) {
					try (PreparedStatement ps = conn.prepareStatement("UPDATE tags SET tag=? WHERE id=?")) {
						ps.setString(1, newName.trim());
						ps.setInt(2, id);
						ps.executeUpdate();
						loadTags(model);
					} catch (SQLException ex) {
						showError(ex);
					}
				}
			}
		});

		deleteBtn.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				int id = (int) model.getValueAt(row, 0);
				String tagName = (String) model.getValueAt(row, 1);

				StringBuilder msg = new StringBuilder("Deleting tag \"" + tagName + "\" will also delete:\n\n");

				try (PreparedStatement ps1 = conn.prepareStatement("SELECT alias FROM tag_aliases WHERE tag_id=?");
						PreparedStatement ps2 = conn.prepareStatement(
								"SELECT t2.tag FROM tag_connections tc JOIN tags t2 ON tc.connection_tag_id = t2.id WHERE tc.tag_id=? "
										+ "UNION SELECT t1.tag FROM tag_connections tc JOIN tags t1 ON tc.tag_id = t1.id WHERE tc.connection_tag_id=?");
						PreparedStatement ps3 = conn.prepareStatement(
								"SELECT f.path FROM file_tags ft JOIN files f ON ft.file_id = f.id WHERE ft.tag_id=?")) {
					// Aliases
					ps1.setInt(1, id);
					ResultSet rs1 = ps1.executeQuery();
					while (rs1.next()) {
						msg.append(" - Alias: ").append(rs1.getString("alias")).append("\n");
					}

					// Connections
					ps2.setInt(1, id);
					ps2.setInt(2, id);
					ResultSet rs2 = ps2.executeQuery();
					while (rs2.next()) {
						msg.append(" - Connection with tag: ").append(rs2.getString(1)).append("\n");
					}

					// File mappings
					ps3.setInt(1, id);
					ResultSet rs3 = ps3.executeQuery();
					while (rs3.next()) {
						msg.append(" - File mapping: ").append(rs3.getString("path")).append("\n");
					}

				} catch (SQLException ex) {
					showError(ex);
				}

				// Put the message into a scrollable text area
				JTextArea textArea = new JTextArea(msg.toString());
				textArea.setEditable(false);
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setPreferredSize(new Dimension(400, 300));

				int confirm = JOptionPane.showConfirmDialog(this, scrollPane, "Confirm Delete",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

				if (confirm == JOptionPane.YES_OPTION) {
					try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tags WHERE id=?")) {
						ps.setInt(1, id);
						ps.executeUpdate();
						loadTags(model);
					} catch (SQLException ex) {
						showError(ex);
					}
				}
			}
		});

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private void loadTags(DefaultTableModel model) {
		model.setRowCount(0);
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT id, tag FROM tags ORDER BY tag")) {
			while (rs.next()) {
				model.addRow(new Object[] { rs.getInt("id"), rs.getString("tag") });
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// ------------------ ALIASES PANEL ------------------
	private JPanel createAliasPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		DefaultTableModel model = new DefaultTableModel(new String[] { "ID", "Tag ID", "Alias" }, 0);
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		loadAliases(model);

		JPanel buttons = new JPanel();
		JButton addBtn = new JButton("Add");
		JButton renameBtn = new JButton("Rename");
		JButton deleteBtn = new JButton("Delete");
		buttons.add(addBtn);
		buttons.add(renameBtn);
		buttons.add(deleteBtn);

		// similar CRUD logic like in tags...
		// (add asks for alias + tag id, rename updates alias, delete confirms cascade)

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private void loadAliases(DefaultTableModel model) {
		model.setRowCount(0);
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT id, tag_id, alias FROM tag_aliases ORDER BY alias")) {
			while (rs.next()) {
				model.addRow(new Object[] { rs.getInt("id"), rs.getInt("tag_id"), rs.getString("alias") });
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// ------------------ CONNECTIONS PANEL ------------------
	private JPanel createConnectionPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		DefaultTableModel model = new DefaultTableModel(new String[] { "ID", "Tag", "Connected Tag" }, 0);
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		loadConnections(model);

		JPanel buttons = new JPanel();
		JButton addBtn = new JButton("Add");
		JButton deleteBtn = new JButton("Delete");
		buttons.add(addBtn);
		buttons.add(deleteBtn);

		// similar CRUD logic here: pick tag_id and connection_tag_id

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private void loadConnections(DefaultTableModel model) {
		model.setRowCount(0);
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT tc.id, t1.tag, t2.tag FROM tag_connections tc "
						+ "JOIN tags t1 ON tc.tag_id=t1.id " + "JOIN tags t2 ON tc.connection_tag_id=t2.id")) {
			while (rs.next()) {
				model.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3) });
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// ------------------ UTILITY ------------------
	private void showError(Exception ex) {
		JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	// ------------------OVERVIEW------------------
	private JPanel createOverviewPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		DefaultTableModel model = new DefaultTableModel(
				new String[] { "File ID", "File Path", "Tag", "Tag Count", "Aliases", "Connections" }, 0);

		JTable table = new JTable(model);

		// Enable sorting on column headers
		table.setAutoCreateRowSorter(true);

		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> loadOverview(model));

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(refreshBtn, BorderLayout.SOUTH);

		// Initial load
		loadOverview(model);
		resizeTableColumns(table);

		return panel;
	}

	private void loadOverview(DefaultTableModel model) {
		model.setRowCount(0);

		String sql = """
				    SELECT f.id AS file_id,
				           f.path AS file_path,
				           t.tag AS tag,
				           COUNT(ft.tag_id) AS tag_count,
				           GROUP_CONCAT(DISTINCT ta.alias) AS aliases,
				           GROUP_CONCAT(DISTINCT t2.tag) AS connections
				    FROM files f
				    JOIN file_tags ft ON f.id = ft.file_id
				    JOIN tags t ON ft.tag_id = t.id
				    LEFT JOIN tag_aliases ta ON t.id = ta.tag_id
				    LEFT JOIN tag_connections tc ON t.id = tc.tag_id OR t.id = tc.connection_tag_id
				    LEFT JOIN tags t2 ON (t2.id = tc.tag_id OR t2.id = tc.connection_tag_id) AND t2.id != t.id
				    GROUP BY f.id, t.id
				    ORDER BY f.id
				""";

		try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				model.addRow(new Object[] { rs.getInt("file_id"), rs.getString("file_path"), rs.getString("tag"),
						rs.getInt("tag_count"), rs.getString("aliases"), rs.getString("connections") });
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// -----------------HELPER METHODS-----------------
	private void resizeTableColumns(JTable table) {
		final int margin = 6; // padding space
		for (int col = 0; col < table.getColumnCount(); col++) {
			int maxWidth = 50; // minimum reasonable width
			int preferredWidth;

			// Check header width
			TableColumn column = table.getColumnModel().getColumn(col);
			TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
			Component headerComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false,
					false, 0, col);
			maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);

			// Check cell contents width
			for (int row = 0; row < table.getRowCount(); row++) {
				TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
				Component comp = table.prepareRenderer(cellRenderer, row, col);
				maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
			}

			// Add margin
			preferredWidth = maxWidth + margin;

			// Limit huge columns (like file paths)
			if (preferredWidth > 400) {
				preferredWidth = 400; // max column width
			}

			column.setPreferredWidth(preferredWidth);
		}
	}

	// ------------BIG------------------
	private JPanel createUnifiedTagPanel() {

		JPanel panel = new JPanel(new BorderLayout());

		// Tree for tags + aliases + connections
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tags");
		JTree tree = new JTree(root);
		loadTagTree(root);
		JScrollPane treeScroll = new JScrollPane(tree);

		// Details panel (container, not just text area)
		JPanel detailsPanel = new JPanel(new BorderLayout());
		JScrollPane detailsScroll = new JScrollPane(detailsPanel);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailsScroll);
		split.setDividerLocation(250);

		// Buttons
		JPanel buttons = new JPanel();
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		JButton refreshBtn = new JButton("Refresh");
		buttons.add(addBtn);
		buttons.add(editBtn);
		buttons.add(deleteBtn);
		buttons.add(refreshBtn);

		// Tree click -> load details
		tree.addTreeSelectionListener(e -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			if (node == null)
				return;
			Object data = node.getUserObject();

			detailsPanel.removeAll(); // clear old content

			if (data instanceof TagInfo tag) {
				JScrollPane newDetails = loadTagDetailsPanel(tag.id);
				detailsPanel.add(newDetails, BorderLayout.CENTER);

			} else if (data instanceof AliasInfo alias) {
				JTextArea aliasDetails = new JTextArea("Alias: " + alias.alias + "\nTag ID: " + alias.tagId);
				aliasDetails.setEditable(false);
				detailsPanel.add(new JScrollPane(aliasDetails), BorderLayout.CENTER);

			} else if (data instanceof ConnectionInfo conn) {
				JTextArea connDetails = new JTextArea("Connection between: " + conn.tagName1 + " and " + conn.tagName2);
				connDetails.setEditable(false);
				detailsPanel.add(new JScrollPane(connDetails), BorderLayout.CENTER);
			}

			detailsPanel.revalidate();
			detailsPanel.repaint();
		});

		// Add/edit/delete button handlers would open dialogs
		addBtn.addActionListener(e -> openTagEditorDialog(null));
		editBtn.addActionListener(e -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			if (node != null && node.getUserObject() instanceof TagInfo tag) {
				openTagEditorDialog(tag);
			}
		});
		deleteBtn.addActionListener(e -> {
			/* confirm + cascade delete */});
		refreshBtn.addActionListener(e -> {
			root.removeAllChildren();
			loadTagTree(root);
			((DefaultTreeModel) tree.getModel()).reload();
		});

		panel.add(split, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private void loadTagTree(DefaultMutableTreeNode root) {
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT id, tag FROM tags ORDER BY tag")) {
			while (rs.next()) {
				TagInfo tag = new TagInfo(rs.getInt("id"), rs.getString("tag"));
				DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(tag);

				// Aliases
				try (PreparedStatement ps = conn.prepareStatement("SELECT id, alias FROM tag_aliases WHERE tag_id=?")) {
					ps.setInt(1, tag.id);
					ResultSet rsa = ps.executeQuery();
					while (rsa.next()) {
						AliasInfo alias = new AliasInfo(rsa.getInt("id"), tag.id, rsa.getString("alias"));
						tagNode.add(new DefaultMutableTreeNode(alias));
					}
				}

				// Connections
				try (PreparedStatement ps = conn.prepareStatement("SELECT tc.id, t2.tag FROM tag_connections tc "
						+ "JOIN tags t2 ON (t2.id = tc.connection_tag_id) " + "WHERE tc.tag_id=?")) {
					ps.setInt(1, tag.id);
					ResultSet rsc = ps.executeQuery();
					while (rsc.next()) {
						ConnectionInfo connInfo = new ConnectionInfo(rsc.getInt("id"), tag.name, rsc.getString("tag"));
						tagNode.add(new DefaultMutableTreeNode(connInfo));
					}
				}

				root.add(tagNode);
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// Small helper record-like classes
	class TagInfo {
		int id;
		String name;

		TagInfo(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public String toString() {
			return "Tag: " + name;
		}
	}

	class AliasInfo {
		int id, tagId;
		String alias;

		AliasInfo(int id, int tagId, String alias) {
			this.id = id;
			this.tagId = tagId;
			this.alias = alias;
		}

		public String toString() {
			return "Alias: " + alias;
		}
	}

	class ConnectionInfo {
		int id;
		String tagName1, tagName2;

		ConnectionInfo(int id, String t1, String t2) {
			this.id = id;
			this.tagName1 = t1;
			this.tagName2 = t2;
		}

		public String toString() {
			return "Conn: " + tagName2;
		}
	}

	// === Load details for right-hand side panel ===
	private JScrollPane loadTagDetailsPanel(int tagId) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		try {
			// Tag name
			String tagName = "";
			try (PreparedStatement ps = conn.prepareStatement("SELECT tag FROM tags WHERE id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					tagName = rs.getString("tag");
			}
			panel.add(new JLabel("Tag: " + tagName));

			// Files
			DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
			int fileCount = 0;
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT f.id, f.path FROM file_tags ft " + "JOIN files f ON f.id = ft.file_id WHERE ft.tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					int fid = rs.getInt("id");
					String path = rs.getString("path");
					fileModel.addElement(fid + " : " + path);
					fileCount++;
				}
			}

			JLabel fileLabel = new JLabel("Used in " + fileCount + " file(s)");
			JComboBox<String> fileDropdown = new JComboBox<>(fileModel);
			fileDropdown.setMaximumSize(new Dimension(600, 30)); // prevents giant expansion
			panel.add(fileLabel);
			panel.add(fileDropdown);

			// Aliases
			panel.add(new JLabel("Aliases:"));
			try (PreparedStatement ps = conn.prepareStatement("SELECT alias FROM tag_aliases WHERE tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					panel.add(new JLabel(" - " + rs.getString(1)));
				}
			}

			// Connections
			panel.add(new JLabel("Connections:"));
			try (PreparedStatement ps = conn.prepareStatement("SELECT t2.tag FROM tag_connections tc "
					+ "JOIN tags t2 ON t2.id = tc.connection_tag_id WHERE tc.tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					panel.add(new JLabel(" - " + rs.getString(1)));
				}
			}

		} catch (SQLException ex) {
			showError(ex);
		}

		return new JScrollPane(panel); // so long lists scroll nicely
	}

	private void openTagEditorDialog(TagInfo tag) {
		boolean editing = (tag != null);

		JDialog dialog = new JDialog((Frame) null, editing ? "Edit Tag" : "New Tag", true);
		dialog.setSize(400, 400);
		dialog.setLayout(new BorderLayout());

		JPanel form = new JPanel(new BorderLayout());

		// Tag name
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Tag:"));
		JTextField tagField = new JTextField(editing ? tag.name : "", 20);
		top.add(tagField);
		form.add(top, BorderLayout.NORTH);

		// Aliases
		DefaultListModel<String> aliasModel = new DefaultListModel<>();
		JList<String> aliasList = new JList<>(aliasModel);
		if (editing) {
			try (PreparedStatement ps = conn.prepareStatement("SELECT alias FROM tag_aliases WHERE tag_id=?")) {
				ps.setInt(1, tag.id);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					aliasModel.addElement(rs.getString(1));
			} catch (SQLException ex) {
				showError(ex);
			}
		}
		JButton addAlias = new JButton("Add Alias");
		JButton removeAlias = new JButton("Remove Alias");
		addAlias.addActionListener(e -> {
			String a = JOptionPane.showInputDialog(dialog, "Alias:");
			if (a != null && !a.isBlank())
				aliasModel.addElement(a.trim());
		});
		removeAlias.addActionListener(e -> {
			int idx = aliasList.getSelectedIndex();
			if (idx >= 0)
				aliasModel.remove(idx);
		});
		JPanel aliasBtns = new JPanel();
		aliasBtns.add(addAlias);
		aliasBtns.add(removeAlias);
		JPanel aliasPanel = new JPanel(new BorderLayout());
		aliasPanel.add(new JLabel("Aliases:"), BorderLayout.NORTH);
		aliasPanel.add(new JScrollPane(aliasList), BorderLayout.CENTER);
		aliasPanel.add(aliasBtns, BorderLayout.SOUTH);

		// Connections
		DefaultListModel<String> connModel = new DefaultListModel<>();
		JList<String> connList = new JList<>(connModel);
		if (editing) {
			try (PreparedStatement ps = conn.prepareStatement("SELECT t2.tag FROM tag_connections tc "
					+ "JOIN tags t2 ON t2.id = tc.connection_tag_id WHERE tc.tag_id=?")) {
				ps.setInt(1, tag.id);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					connModel.addElement(rs.getString(1));
			} catch (SQLException ex) {
				showError(ex);
			}
		}
		JButton addConn = new JButton("Add Connection");
		JButton removeConn = new JButton("Remove Connection");
		addConn.addActionListener(e -> {
			String c = JOptionPane.showInputDialog(dialog, "Connect to tag:");
			if (c != null && !c.isBlank())
				connModel.addElement(c.trim());
		});
		removeConn.addActionListener(e -> {
			int idx = connList.getSelectedIndex();
			if (idx >= 0)
				connModel.remove(idx);
		});
		JPanel connBtns = new JPanel();
		connBtns.add(addConn);
		connBtns.add(removeConn);
		JPanel connPanel = new JPanel(new BorderLayout());
		connPanel.add(new JLabel("Connections:"), BorderLayout.NORTH);
		connPanel.add(new JScrollPane(connList), BorderLayout.CENTER);
		connPanel.add(connBtns, BorderLayout.SOUTH);

		// Put aliases + connections into tabs
		JTabbedPane tabs = new JTabbedPane();
		tabs.add("Aliases", aliasPanel);
		tabs.add("Connections", connPanel);

		form.add(tabs, BorderLayout.CENTER);

		// Save/Cancel
		JPanel bottom = new JPanel();
		JButton saveBtn = new JButton("Save");
		JButton cancelBtn = new JButton("Cancel");
		bottom.add(saveBtn);
		bottom.add(cancelBtn);

		saveBtn.addActionListener(e -> {
			try {
				conn.setAutoCommit(false);

				int tagId = editing ? tag.id : -1;

				// Insert or update tag
				if (!editing) {
					try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tags(tag) VALUES(?)",
							Statement.RETURN_GENERATED_KEYS)) {
						ps.setString(1, tagField.getText().trim());
						ps.executeUpdate();
						ResultSet keys = ps.getGeneratedKeys();
						if (keys.next())
							tagId = keys.getInt(1);
					}
				} else {
					try (PreparedStatement ps = conn.prepareStatement("UPDATE tags SET tag=? WHERE id=?")) {
						ps.setString(1, tagField.getText().trim());
						ps.setInt(2, tagId);
						ps.executeUpdate();
					}
					// clear old aliases/connections
					try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tag_aliases WHERE tag_id=?")) {
						ps.setInt(1, tagId);
						ps.executeUpdate();
					}
					try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tag_connections WHERE tag_id=?")) {
						ps.setInt(1, tagId);
						ps.executeUpdate();
					}
				}

				// Insert aliases
				try (PreparedStatement ps = conn
						.prepareStatement("INSERT INTO tag_aliases(tag_id, alias) VALUES(?, ?)")) {
					for (int i = 0; i < aliasModel.size(); i++) {
						ps.setInt(1, tagId);
						ps.setString(2, aliasModel.get(i));
						ps.addBatch();
					}
					ps.executeBatch();
				}

				// Insert connections (create tags if not exist)
				for (int i = 0; i < connModel.size(); i++) {
					String connTag = connModel.get(i);
					int connTagId = ensureTagExists(connTag);
					try (PreparedStatement ps = conn
							.prepareStatement("INSERT INTO tag_connections(tag_id, connection_tag_id) VALUES(?, ?)")) {
						ps.setInt(1, tagId);
						ps.setInt(2, connTagId);
						ps.executeUpdate();
					}
				}

				conn.commit();
				dialog.dispose();
			} catch (SQLException ex) {
				try {
					conn.rollback();
				} catch (SQLException ignore) {
				}
				showError(ex);
			} finally {
				try {
					conn.setAutoCommit(true);
				} catch (SQLException ignore) {
				}
			}
		});

		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.add(form, BorderLayout.CENTER);
		dialog.add(bottom, BorderLayout.SOUTH);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	// helper: ensures tag exists, returns id
	private int ensureTagExists(String tagName) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tags WHERE tag=?")) {
			ps.setString(1, tagName);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1);
		}
		try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tags(tag) VALUES(?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, tagName);
			ps.executeUpdate();
			ResultSet keys = ps.getGeneratedKeys();
			if (keys.next())
				return keys.getInt(1);
		}
		throw new SQLException("Could not create tag: " + tagName);
	}

	// ------------------ RUN TEST ------------------
	public static void main(String[] args) throws Exception {

		System.out.println("Connection to SQLite has been established.");

		DbManager dbManager = new DbManager();
		dbManager.createDatabase(); // ensure DB and tables exist
		String tempConnDir = DbManager.tempConnDir;
		Connection conn = DriverManager.getConnection(tempConnDir);
		SwingUtilities.invokeLater(() -> new TagManagerFrame(conn).setVisible(true));
	}
}
