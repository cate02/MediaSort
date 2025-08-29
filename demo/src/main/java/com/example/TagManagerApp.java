package com.example;

// TagManagerApp.java
// Java 11+ recommended. Requires sqlite-jdbc on classpath (e.g. org.xerial:sqlite-jdbc).
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class TagManagerApp extends JFrame {
	private final Connection conn;

	// UI components
	private final JTextField tagSearchField = new JTextField(20);
	private final DefaultListModel<TagRow> tagListModel = new DefaultListModel<>();
	private final JList<TagRow> tagList = new JList<>(tagListModel);

	private final JTextField aliasSearchField = new JTextField(20);
	private final AliasTableModel aliasTableModel = new AliasTableModel();
	private final JTable aliasTable = new JTable(aliasTableModel);

	private final JTextField connSearchField = new JTextField(20);
	private final ConnTableModel connTableModel = new ConnTableModel();
	private final JTable connTable = new JTable(connTableModel);

	// helper: last selected tag
	private TagRow selectedTag = null;

	public static void main(String[] args) throws Exception {
		// opens (or creates) mytags.db in current working dir

		DbManager dbManager = new DbManager();
		// dbManager.createDatabase(); // ensure DB and tables exist
		String tempConnDir = DbManager.tempConnDir;
		Connection conn = dbManager.getConnection(tempConnDir);

		// Connection conn = DriverManager.getConnection("jdbc:sqlite:mytags.db");
		SwingUtilities.invokeLater(() -> {
			try {
				new TagManagerApp(conn).setVisible(true);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "Failed: " + ex.getMessage());
			}
		});
	}

	public TagManagerApp(Connection conn) throws SQLException {
		super("Tag Manager");
		this.conn = conn;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(950, 700);
		setLocationRelativeTo(null);
		initSchemaIfMissing(); // optional, will create if not exist
		initUI();
	}

	// ---------------- UI ----------------
	private void initUI() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.add("Tags", createTagsPanel());
		tabs.add("Aliases", createAliasesPanel());
		tabs.add("Connections", createConnectionsPanel());
		add(tabs, BorderLayout.CENTER);
	}

	private JPanel createTagsPanel() {
		JPanel p = new JPanel(new BorderLayout(8, 8));
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Search tags:"));
		top.add(tagSearchField);
		JButton searchBtn = new JButton("Search");
		JButton addBtn = new JButton("New Tag");
		JButton deleteBtn = new JButton("Delete Tag");
		JButton refreshBtn = new JButton("Refresh");
		top.add(searchBtn);
		top.add(addBtn);
		top.add(deleteBtn);
		top.add(refreshBtn);

		p.add(top, BorderLayout.NORTH);

		tagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tagList.setCellRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList<?> list, Object value, int idx, boolean sel,
					boolean foc) {
				JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, foc);
				if (value instanceof TagRow tr)
					lbl.setText(tr.tag + "  (id=" + tr.id + ", files=" + tr.fileCount + ")");
				return lbl;
			}
		});

		JScrollPane leftScroll = new JScrollPane(tagList);
		leftScroll.setPreferredSize(new Dimension(350, 400));

		// details area on right
		JPanel right = new JPanel(new BorderLayout(6, 6));
		right.setBorder(BorderFactory.createTitledBorder("Tag Details"));
		JTextArea details = new JTextArea();
		details.setEditable(false);
		right.add(new JScrollPane(details), BorderLayout.CENTER);

		// when selecting a tag, load details and store selectedTag
		tagList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				selectedTag = tagList.getSelectedValue();
				if (selectedTag != null) {
					details.setText(buildTagDetails(selectedTag.id));
				} else {
					details.setText("");
				}
			}
		});

		JPanel center = new JPanel(new BorderLayout());
		center.add(leftScroll, BorderLayout.WEST);
		center.add(right, BorderLayout.CENTER);

		p.add(center, BorderLayout.CENTER);

		// handlers
		searchBtn.addActionListener(e -> loadTags(tagSearchField.getText().trim()));
		refreshBtn.addActionListener(e -> {
			tagSearchField.setText("");
			loadTags("");
		});
		addBtn.addActionListener(e -> openTagCreateDialog());
		deleteBtn.addActionListener(e -> {
			TagRow sel = tagList.getSelectedValue();
			if (sel != null)
				confirmAndDeleteTag(sel);
			else
				JOptionPane.showMessageDialog(this, "Select a tag first.");
		});

		loadTags(""); // initial load
		return p;
	}

	private JPanel createAliasesPanel() {
		JPanel p = new JPanel(new BorderLayout(6, 6));
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Search aliases:"));
		top.add(aliasSearchField);
		JButton search = new JButton("Search");
		JButton applyBtn = new JButton("Apply selected to chosen tag");
		JButton createBtn = new JButton("Create Alias");
		JButton deleteBtn = new JButton("Delete Selected");
		JButton sortSelectedTopBtn = new JButton("Sort selected to top");
		top.add(search);
		top.add(applyBtn);
		top.add(createBtn);
		top.add(deleteBtn);
		top.add(sortSelectedTopBtn);
		p.add(top, BorderLayout.NORTH);

		aliasTable.setFillsViewportHeight(true);
		aliasTable.getColumnModel().getColumn(0).setMaxWidth(40);
		aliasTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scroll = new JScrollPane(aliasTable);
		p.add(scroll, BorderLayout.CENTER);

		// handlers
		search.addActionListener(e -> loadAliases(aliasSearchField.getText().trim()));
		createBtn.addActionListener(e -> openAliasCreateDialog());
		deleteBtn.addActionListener(e -> deleteSelectedAliases());
		applyBtn.addActionListener(e -> applySelectedAliasesToTag());
		sortSelectedTopBtn.addActionListener(e -> aliasTableModel.moveSelectedToTop());

		loadAliases("");
		return p;
	}

	private JPanel createConnectionsPanel() {
		JPanel p = new JPanel(new BorderLayout(6, 6));
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Search connections (tag text):"));
		top.add(connSearchField);
		JButton search = new JButton("Search");
		JButton applyBtn = new JButton("Apply selected connection to chosen tag");
		JButton createBtn = new JButton("Create Connection");
		JButton deleteBtn = new JButton("Delete Selected");
		top.add(search);
		top.add(applyBtn);
		top.add(createBtn);
		top.add(deleteBtn);
		p.add(top, BorderLayout.NORTH);

		connTable.setFillsViewportHeight(true);
		connTable.getColumnModel().getColumn(0).setMaxWidth(40);
		connTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scroll = new JScrollPane(connTable);
		p.add(scroll, BorderLayout.CENTER);

		search.addActionListener(e -> loadConnections(connSearchField.getText().trim()));
		createBtn.addActionListener(e -> openConnectionCreateDialog());
		deleteBtn.addActionListener(e -> deleteSelectedConnections());
		applyBtn.addActionListener(e -> applySelectedConnectionsToTag());

		loadConnections("");
		return p;
	}

	// ---------------- Loading data ----------------

	private void loadTags(String text) {
		tagListModel.clear();
		String sql = "SELECT t.id, t.tag, COUNT(ft.file_id) as filecount "
				+ "FROM tags t LEFT JOIN file_tags ft ON t.id = ft.tag_id "
				+ "WHERE t.tag LIKE ? GROUP BY t.id ORDER BY t.tag";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, "%" + text + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					tagListModel.addElement(new TagRow(rs.getInt("id"), rs.getString("tag"), rs.getInt("filecount")));
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	private void loadAliases(String text) {
		aliasTableModel.clear();
		String sql = "SELECT id, tag_id, alias FROM tag_aliases WHERE alias LIKE ? ORDER BY alias";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, "%" + text + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					aliasTableModel.addRow(new AliasRow(rs.getInt("id"), rs.getInt("tag_id"), rs.getString("alias")));
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	private void loadConnections(String text) {
		connTableModel.clear();
		String sql = "SELECT tc.id, tc.tag_id, tc.connection_tag_id, t1.tag as t1, t2.tag as t2 "
				+ "FROM tag_connections tc " + "JOIN tags t1 ON tc.tag_id = t1.id "
				+ "JOIN tags t2 ON tc.connection_tag_id = t2.id "
				+ "WHERE t1.tag LIKE ? OR t2.tag LIKE ? ORDER BY t1.tag, t2.tag";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, "%" + text + "%");
			ps.setString(2, "%" + text + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					connTableModel.addRow(new ConnRow(rs.getInt("id"), rs.getInt("tag_id"),
							rs.getInt("connection_tag_id"), rs.getString("t1"), rs.getString("t2")));
			}
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	// ---------------- Actions: Tags ----------------

	private void openTagCreateDialog() {
		JTextField tagName = new JTextField(20);
		JTextField aliases = new JTextField(30); // comma separated
		JTextField conns = new JTextField(30); // comma separated tag names

		JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
		form.add(new JLabel("Tag name:"));
		form.add(tagName);
		form.add(new JLabel("Aliases (comma separated, optional):"));
		form.add(aliases);
		form.add(new JLabel("Connections (comma separated tag names, optional):"));
		form.add(conns);

		int ok = JOptionPane.showConfirmDialog(this, form, "Create new Tag", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ok == JOptionPane.OK_OPTION) {
			String name = tagName.getText().trim();
			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Name required");
				return;
			}
			try {
				conn.setAutoCommit(false);
				int tagId = ensureTagExists(name); // returns existing or created id (if exists it returns existing)
				// insert aliases if provided
				String[] as = splitComma(aliases.getText());
				try (PreparedStatement ps = conn
						.prepareStatement("INSERT OR IGNORE INTO tag_aliases(tag_id, alias) VALUES(?,?)")) {
					for (String a : as) {
						ps.setInt(1, tagId);
						ps.setString(2, a);
						ps.addBatch();
					}
					ps.executeBatch();
				}
				// connections - ensure tags exist then insert
				String[] cs = splitComma(conns.getText());
				for (String c : cs) {
					int cid = ensureTagExists(c);
					try (PreparedStatement ps = conn.prepareStatement(
							"INSERT OR IGNORE INTO tag_connections(tag_id, connection_tag_id) VALUES(?,?)")) {
						ps.setInt(1, tagId);
						ps.setInt(2, cid);
						ps.executeUpdate();
					}
				}
				conn.commit();
				loadTags("");
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
		}
	}

	private void confirmAndDeleteTag(TagRow tr) {
		// construct what will be deleted
		List<String> aliases = new ArrayList<>();
		List<String> connections = new ArrayList<>();
		List<String> fileMappings = new ArrayList<>();
		try {
			try (PreparedStatement ps = conn.prepareStatement("SELECT alias FROM tag_aliases WHERE tag_id=?")) {
				ps.setInt(1, tr.id);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					aliases.add(rs.getString(1));
			}
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT t2.tag FROM tag_connections tc JOIN tags t2 ON tc.connection_tag_id = t2.id WHERE tc.tag_id=?")) {
				ps.setInt(1, tr.id);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					connections.add(rs.getString(1));
			}
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT f.id, f.path FROM file_tags ft JOIN files f ON ft.file_id = f.id WHERE ft.tag_id=?")) {
				ps.setInt(1, tr.id);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					fileMappings.add(rs.getInt(1) + " : " + rs.getString(2));
			}
		} catch (SQLException ex) {
			showError(ex);
			return;
		}

		// build panel showing those lists
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.add(new JLabel("Delete tag: " + tr.tag + " (id=" + tr.id + ")"), BorderLayout.NORTH);
		JTabbedPane t = new JTabbedPane();

		t.add("Aliases (" + aliases.size() + ")", new JScrollPane(new JList<>(aliases.toArray(new String[0]))));
		t.add("Connections (" + connections.size() + ")",
				new JScrollPane(new JList<>(connections.toArray(new String[0]))));
		t.add("File mappings (" + fileMappings.size() + ")",
				new JScrollPane(new JList<>(fileMappings.toArray(new String[0]))));
		panel.add(t, BorderLayout.CENTER);

		int ok = JOptionPane.showConfirmDialog(this, panel, "Confirm delete", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (ok == JOptionPane.YES_OPTION) {
			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tags WHERE id=?")) {
				ps.setInt(1, tr.id);
				ps.executeUpdate();
				loadTags("");
			} catch (SQLException ex) {
				showError(ex);
			}
		}
	}

	// ---------------- Actions: Aliases ----------------

	private void openAliasCreateDialog() {
		String alias = JOptionPane.showInputDialog(this, "Alias text:");
		if (alias == null || alias.trim().isEmpty())
			return;
		// optionally choose tag to attach to
		String[] tags = loadTagNamesArray();
		String attach = (String) JOptionPane.showInputDialog(this, "Attach to tag (optional):", "Attach",
				JOptionPane.PLAIN_MESSAGE, null, tags, tags.length > 0 ? tags[0] : null);
		try {
			int tagId = -1;
			if (attach != null)
				tagId = ensureTagExists(attach);
			try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tag_aliases(tag_id, alias) VALUES(?, ?)")) {
				if (tagId == -1) {
					// create orphan alias? we will create tag with placeholder name if none
					// provided.
					JOptionPane.showMessageDialog(this, "Please choose or create a tag to attach alias to.");
				} else {
					ps.setInt(1, tagId);
					ps.setString(2, alias.trim());
					ps.executeUpdate();
				}
			}
			loadAliases("");
		} catch (SQLException ex) {
			showError(ex);
		}
	}

	private void deleteSelectedAliases() {
		int[] rows = aliasTable.getSelectedRows();
		if (rows.length == 0) {
			JOptionPane.showMessageDialog(this, "Select alias rows to delete.");
			return;
		}
		List<AliasRow> toDelete = new ArrayList<>();
		for (int r : rows)
			toDelete.add(aliasTableModel.getRowAt(aliasTable.convertRowIndexToModel(r)));
		// show cascade info for each alias
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		DefaultListModel<String> lm = new DefaultListModel<>();
		for (AliasRow ar : toDelete) {
			// what will be deleted: alias row only, but could show tag info and file
			// mappings using that tag
			lm.addElement("Alias: " + ar.alias + " (id=" + ar.id + ", tagId=" + ar.tagId + ")");
		}
		panel.add(new JScrollPane(new JList<>(lm)), BorderLayout.CENTER);
		int ok = JOptionPane.showConfirmDialog(this, panel, "Confirm delete aliases", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (ok == JOptionPane.YES_OPTION) {
			try {
				conn.setAutoCommit(false);
				try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tag_aliases WHERE id=?")) {
					for (AliasRow ar : toDelete) {
						ps.setInt(1, ar.id);
						ps.addBatch();
					}
					ps.executeBatch();
				}
				conn.commit();
				loadAliases("");
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
		}
	}

	private void applySelectedAliasesToTag() {
		if (selectedTag == null) {
			JOptionPane.showMessageDialog(this, "Select a tag in the Tags tab first.");
			return;
		}
		int[] rows = aliasTable.getSelectedRows();
		if (rows.length == 0) {
			JOptionPane.showMessageDialog(this, "Select alias rows to apply.");
			return;
		}
		try {
			conn.setAutoCommit(false);
			try (PreparedStatement ps = conn.prepareStatement("UPDATE tag_aliases SET tag_id=? WHERE id=?")) {
				for (int r : rows) {
					AliasRow ar = aliasTableModel.getRowAt(aliasTable.convertRowIndexToModel(r));
					ps.setInt(1, selectedTag.id);
					ps.setInt(2, ar.id);
					ps.addBatch();
				}
				ps.executeBatch();
			}
			conn.commit();
			loadAliases("");
			loadTags("");
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
	}

	// ---------------- Actions: Connections ----------------

	private void openConnectionCreateDialog() {
		// choose two tags to connect (by name)
		String[] tags = loadTagNamesArray();
		if (tags.length < 1) {
			JOptionPane.showMessageDialog(this, "Create tags first.");
			return;
		}
		JComboBox<String> a = new JComboBox<>(tags);
		JComboBox<String> b = new JComboBox<>(tags);
		JPanel form = new JPanel(new GridLayout(0, 1));
		form.add(new JLabel("Tag A:"));
		form.add(a);
		form.add(new JLabel("Tag B:"));
		form.add(b);
		int ok = JOptionPane.showConfirmDialog(this, form, "Create connection", JOptionPane.OK_CANCEL_OPTION);
		if (ok == JOptionPane.OK_OPTION) {
			String A = (String) a.getSelectedItem();
			String B = (String) b.getSelectedItem();
			if (A.equals(B)) {
				JOptionPane.showMessageDialog(this, "Choose different tags.");
				return;
			}
			try {
				int aid = ensureTagExists(A), bid = ensureTagExists(B);
				try (PreparedStatement ps = conn.prepareStatement(
						"INSERT OR IGNORE INTO tag_connections(tag_id, connection_tag_id) VALUES(?,?)")) {
					ps.setInt(1, aid);
					ps.setInt(2, bid);
					ps.executeUpdate();
				}
				loadConnections("");
			} catch (SQLException ex) {
				showError(ex);
			}
		}
	}

	private void deleteSelectedConnections() {
		int[] rows = connTable.getSelectedRows();
		if (rows.length == 0) {
			JOptionPane.showMessageDialog(this, "Select connection rows to delete.");
			return;
		}
		List<ConnRow> toDel = new ArrayList<>();
		for (int r : rows)
			toDel.add(connTableModel.getRowAt(connTable.convertRowIndexToModel(r)));
		JPanel panel = new JPanel(new GridLayout(0, 1));
		for (ConnRow cr : toDel)
			panel.add(new JLabel("Conn: " + cr.t1 + " <-> " + cr.t2 + " (id=" + cr.id + ")"));
		int ok = JOptionPane.showConfirmDialog(this, panel, "Confirm delete connections", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (ok == JOptionPane.YES_OPTION) {
			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tag_connections WHERE id=?")) {
				for (ConnRow cr : toDel) {
					ps.setInt(1, cr.id);
					ps.addBatch();
				}
				ps.executeBatch();
				loadConnections("");
			} catch (SQLException ex) {
				showError(ex);
			}
		}
	}

	private void applySelectedConnectionsToTag() {
		if (selectedTag == null) {
			JOptionPane.showMessageDialog(this, "Select a tag in the Tags tab first.");
			return;
		}
		int[] rows = connTable.getSelectedRows();
		if (rows.length == 0) {
			JOptionPane.showMessageDialog(this, "Select connection rows to apply.");
			return;
		}
		try {
			conn.setAutoCommit(false);
			try (PreparedStatement ps = conn
					.prepareStatement("INSERT OR IGNORE INTO tag_connections(tag_id, connection_tag_id) VALUES(?,?)")) {
				for (int r : rows) {
					ConnRow cr = connTableModel.getRowAt(connTable.convertRowIndexToModel(r));
					// apply by duplicating the connection with selectedTag as tag_id
					ps.setInt(1, selectedTag.id);
					ps.setInt(2, cr.connectionTagId);
					ps.addBatch();
				}
				ps.executeBatch();
			}
			conn.commit();
			loadConnections("");
			loadTags("");
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
	}

	// ---------------- Utilities ----------------

	private String[] loadTagNamesArray() {
		List<String> res = new ArrayList<>();
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT tag FROM tags ORDER BY tag")) {
			while (rs.next())
				res.add(rs.getString(1));
		} catch (SQLException ex) {
			showError(ex);
		}
		return res.toArray(new String[0]);
	}

	private String buildTagDetails(int tagId) {
		StringBuilder sb = new StringBuilder();
		try {
			try (PreparedStatement ps = conn.prepareStatement("SELECT tag FROM tags WHERE id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					sb.append("Tag: ").append(rs.getString(1)).append("\n\n");
			}
			// files
			List<String> files = new ArrayList<>();
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT f.id, f.path FROM file_tags ft JOIN files f ON ft.file_id=f.id WHERE ft.tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					files.add(rs.getInt(1) + " : " + rs.getString(2));
			}
			sb.append("Used in ").append(files.size()).append(" file(s)\n");
			for (String f : files)
				sb.append("  - ").append(f).append("\n");
			sb.append("\nAliases:\n");
			try (PreparedStatement ps = conn.prepareStatement("SELECT alias FROM tag_aliases WHERE tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					sb.append("  - ").append(rs.getString(1)).append("\n");
			}
			sb.append("\nConnections:\n");
			try (PreparedStatement ps = conn.prepareStatement(
					"SELECT t2.tag FROM tag_connections tc JOIN tags t2 ON tc.connection_tag_id=t2.id WHERE tc.tag_id=?")) {
				ps.setInt(1, tagId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					sb.append("  - ").append(rs.getString(1)).append("\n");
			}
		} catch (SQLException ex) {
			showError(ex);
		}
		return sb.toString();
	}

	private int ensureTagExists(String name) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tags WHERE tag=?")) {
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1);
		}
		try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tags(tag) VALUES(?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, name);
			ps.executeUpdate();
			ResultSet k = ps.getGeneratedKeys();
			if (k.next())
				return k.getInt(1);
		}
		throw new SQLException("Failed to ensure tag");
	}

	private String[] splitComma(String s) {
		if (s == null)
			return new String[0];
		return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toArray(String[]::new);
	}

	private void showError(Exception ex) {
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
	}

	// ---------------- Init sample schema (if missing) ----------------
	private void initSchemaIfMissing() throws SQLException {
		// creates simple sane schema if not present. skip if already exists
		try (Statement st = conn.createStatement()) {
			st.execute(
					"CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL UNIQUE)");
			st.execute(
					"CREATE TABLE IF NOT EXISTS files (id INTEGER PRIMARY KEY AUTOINCREMENT, hash TEXT, path TEXT NOT NULL UNIQUE)");
			st.execute(
					"CREATE TABLE IF NOT EXISTS file_tags (id INTEGER PRIMARY KEY AUTOINCREMENT, file_id INTEGER NOT NULL, tag_id INTEGER NOT NULL, FOREIGN KEY(file_id) REFERENCES files(id) ON DELETE CASCADE, FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE)");
			st.execute(
					"CREATE TABLE IF NOT EXISTS tag_aliases (id INTEGER PRIMARY KEY AUTOINCREMENT, tag_id INTEGER NOT NULL, alias TEXT NOT NULL UNIQUE, FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE)");
			st.execute(
					"CREATE TABLE IF NOT EXISTS tag_connections (id INTEGER PRIMARY KEY AUTOINCREMENT, tag_id INTEGER NOT NULL, connection_tag_id INTEGER NOT NULL, FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE, FOREIGN KEY(connection_tag_id) REFERENCES tags(id) ON DELETE CASCADE)");
		}
	}

	// ---------------- Simple row classes & table models ----------------
	private static class TagRow {
		final int id;
		final String tag;
		final int fileCount;

		TagRow(int id, String tag, int fileCount) {
			this.id = id;
			this.tag = tag;
			this.fileCount = fileCount;
		}

		public String toString() {
			return tag;
		}
	}

	private static class AliasRow {
		final int id;
		int tagId;
		final String alias;

		AliasRow(int id, int tagId, String alias) {
			this.id = id;
			this.tagId = tagId;
			this.alias = alias;
		}
	}

	private static class ConnRow {
		final int id;
		final int tagId;
		final int connectionTagId;
		final String t1;
		final String t2;

		ConnRow(int id, int tagId, int connectionTagId, String t1, String t2) {
			this.id = id;
			this.tagId = tagId;
			this.connectionTagId = connectionTagId;
			this.t1 = t1;
			this.t2 = t2;
		}
	}

	private static class AliasTableModel extends AbstractTableModel {
		private final List<AliasRow> rows = new ArrayList<>();
		private final List<Boolean> checked = new ArrayList<>();
		private final String[] cols = { "Sel", "ID", "Alias", "TagID" };

		public void addRow(AliasRow r) {
			rows.add(r);
			checked.add(Boolean.FALSE);
			fireTableDataChanged();
		}

		public void clear() {
			rows.clear();
			checked.clear();
			fireTableDataChanged();
		}

		public void moveSelectedToTop() {
			List<AliasRow> newRows = new ArrayList<>();
			List<Boolean> newChecked = new ArrayList<>();
			// selected first
			for (int i = 0; i < rows.size(); i++)
				if (checked.get(i)) {
					newRows.add(rows.get(i));
					newChecked.add(true);
				}
			for (int i = 0; i < rows.size(); i++)
				if (!checked.get(i)) {
					newRows.add(rows.get(i));
					newChecked.add(false);
				}
			rows.clear();
			rows.addAll(newRows);
			checked.clear();
			checked.addAll(newChecked);
			fireTableDataChanged();
		}

		public void deleteChecked() {
			for (int i = checked.size() - 1; i >= 0; i--)
				if (checked.get(i)) {
					rows.remove(i);
					checked.remove(i);
				}
			fireTableDataChanged();
		}

		public AliasRow getRowAt(int modelRow) {
			return rows.get(modelRow);
		}

		public void addRows(Collection<AliasRow> c) {
			rows.addAll(c);
			for (int i = 0; i < c.size(); i++)
				checked.add(false);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return cols.length;
		}

		@Override
		public String getColumnName(int c) {
			return cols[c];
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return c == 0 ? Boolean.class : (c == 1 ? Integer.class : String.class);
		}

		@Override
		public boolean isCellEditable(int r, int c) {
			return c == 0;
		}

		@Override
		public Object getValueAt(int r, int c) {
			AliasRow row = rows.get(r);
			return switch (c) {
			case 0 -> checked.get(r);
			case 1 -> row.id;
			case 2 -> row.alias;
			default -> row.tagId;
			};
		}

		@Override
		public void setValueAt(Object val, int r, int c) {
			if (c == 0) {
				checked.set(r, (Boolean) val);
				fireTableRowsUpdated(r, r);
			}
		}
	}

	private static class ConnTableModel extends AbstractTableModel {
		private final List<ConnRow> rows = new ArrayList<>();
		private final List<Boolean> checked = new ArrayList<>();
		private final String[] cols = { "Sel", "ID", "TagA", "TagB", "A_id", "B_id" };

		public void addRow(ConnRow r) {
			rows.add(r);
			checked.add(Boolean.FALSE);
			fireTableDataChanged();
		}

		public void clear() {
			rows.clear();
			checked.clear();
			fireTableDataChanged();
		}

		public ConnRow getRowAt(int modelRow) {
			return rows.get(modelRow);
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return cols.length;
		}

		@Override
		public String getColumnName(int c) {
			return cols[c];
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return c == 0 ? Boolean.class : (c == 1 || c == 4 || c == 5 ? Integer.class : String.class);
		}

		@Override
		public boolean isCellEditable(int r, int c) {
			return c == 0;
		}

		@Override
		public Object getValueAt(int r, int c) {
			ConnRow row = rows.get(r);
			return switch (c) {
			case 0 -> checked.get(r);
			case 1 -> row.id;
			case 2 -> row.t1;
			case 3 -> row.t2;
			case 4 -> row.tagId;
			default -> row.connectionTagId;
			};
		}

		@Override
		public void setValueAt(Object val, int r, int c) {
			if (c == 0) {
				checked.set(r, (Boolean) val);
				fireTableRowsUpdated(r, r);
			}
		}
	}
}
