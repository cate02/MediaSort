package com.example;

//support multiple sub tags. propably via join like with tag/file
//search tags
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MediaSort {
	public static Connection conn;
	public static PreparedStatement stmt;
	public static String selectSQL;
	static Color black = new Color(0, 0, 0);
	static Color gray = new Color(128, 128, 128);
	static String dbPath;
	static String dbPathName = "MediaSort";
	static String contentPath;

	public static void main(String[] args) {

		Preferences preferences = Preferences.userNodeForPackage(MediaSort.class);
		contentPath = preferences.get("contentPath", "");
		if (contentPath == null || contentPath.isEmpty()) {
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
			chooser.setDialogTitle("Select Content Directory");
			chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			int result = chooser.showOpenDialog(null);
			if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
				contentPath = chooser.getSelectedFile().getAbsolutePath();
				preferences.put("contentPath", contentPath); // Save for next time
			} else {
				JOptionPane.showMessageDialog(null, "No directory selected. Exiting.", "Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
		}

		// TagsGUI tagsGUI = new TagsGUI();
		// tagsGUI.main(args);
		dbPath = contentPath + "\\" + dbPathName;
		System.out.println("DB Path: " + dbPath);
		File dbFile = new File(dbPath);
		if (!dbFile.exists()) {
			dbFile.mkdir();
			System.out.println("Directory created: " + dbPath);
		}

		try {
			initialiseDB();
			processDirectory(contentPath);
			setupGUI();
			// openTagGUI(8);

			// assuming it doesnt exist create new file at dbpath

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	private static void initialiseDB() {
		try {
			String tempConnDir = "jdbc:sqlite:" + dbPath + "\\db.db";
			conn = DriverManager.getConnection(tempConnDir);
			System.out.println("Connection to SQLite has been established.");
			if (conn != null) {
				String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"hash TEXT NOT NULL, " +
						"path TEXT NOT NULL UNIQUE" +
						");";
				conn.createStatement().execute(createTableSQL);

				// create table if not exist "tags", linked to the id in "files"
				String createTagsTableSQL = "CREATE TABLE IF NOT EXISTS tags (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"main_tag TEXT NOT NULL UNIQUE," +
						"sub_tag TEXT" +
						");";
				conn.createStatement().execute(createTagsTableSQL);

				String createItemTagsTableSQL = "CREATE TABLE IF NOT EXISTS item_tags (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"file_id INTEGER NOT NULL, " +
						"tag_id INTEGER NOT NULL, " +
						"FOREIGN KEY (file_id) REFERENCES files(id), " +
						"FOREIGN KEY (tag_id) REFERENCES tags(id)" +
						");";
				conn.createStatement().execute(createItemTagsTableSQL);
			}

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	private static void processDirectory(String directoryPath) throws IOException, SQLException {
		// exlcude dbpathname

		// if no such file create it
		File file = new File(directoryPath);
		if (!file.exists()) {
			file.mkdir();
			directoryPath = file.getAbsolutePath();
			System.out.println("Directory created: " + directoryPath);
		}
		File directory = new File(directoryPath);

		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Provided path is not a directory: " + directoryPath);
		}

		// Walk through the directory recursively
		Files.walk(directory.toPath())
				.filter(Files::isRegularFile)
				.forEach(filePath -> {
					try {
						if (filePath.equals(dbPath)) {
							System.out.println("Directory is the same as db path");
							// return;
						}
						File newFile = filePath.toFile();
						if (!checkDuplicate(newFile)) {
							System.out.println("Unique file: " + filePath);
							insertIntoDatabase(newFile);
						} else {
							System.out.println("Duplicate file: " + filePath);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}

	private static String calculateHash(File file) throws Exception {
		byte[] message = Files.readAllBytes(file.toPath());
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] hashed = md.digest(message);

		return new String(hashed);
	}

	static boolean checkDuplicate(File file) throws Exception {
		String hash = calculateHash(file);
		// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
		String selectSQL = "SELECT COUNT(*) FROM files WHERE hash = ?;";
		PreparedStatement stmt = conn.prepareStatement(selectSQL);
		stmt.setString(1, hash);

		ResultSet rs = stmt.executeQuery();
		int occurrence = 0;
		while (rs.next()) {
			occurrence = rs.getInt(1);
		}
		// System.out.println(file.getName() + " " + occurrence + " Occurrence");
		if (occurrence == 0) {
			return false;
		}
		// CheckFilepath(hash);
		// set path to files path
		selectSQL = "SELECT path FROM files WHERE hash = ?;";
		stmt = conn.prepareStatement(selectSQL);
		stmt.setString(1, hash);
		String existingPath = stmt.executeQuery().getString("path");
		String newPath = file.getAbsolutePath();
		if (!existingPath.equals(newPath)) {
			String updateSQL = "UPDATE files SET path = ? WHERE hash = ?;";
			stmt = conn.prepareStatement(updateSQL);
			stmt.setString(1, newPath);
			stmt.setString(2, hash);
			stmt.executeUpdate();
			Path relativePath = Paths.get(existingPath).toAbsolutePath()
					.relativize(Paths.get(newPath).toAbsolutePath());
			System.out.println(file.getName() + " moved to " + relativePath);
		}

		// conn.close();
		return occurrence > 0;
	}

	private static void insertIntoDatabase(File file) throws Exception {
		// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
		String insertSQL = "INSERT INTO files (hash, path) VALUES (?, ?);";
		PreparedStatement stmt = conn.prepareStatement(insertSQL);
		stmt.setString(1, calculateHash(file));
		stmt.setString(2, file.getAbsolutePath().toString());
		stmt.executeUpdate();

	}

	static void setupGUI() throws Exception {
		JFrame frame = new JFrame();
		JPanel interfacePanel = new JPanel(); // search, listings
		JPanel listingPanel = new JPanel(); // listings
		JPanel tagsPanel = new JPanel();
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

		tagsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					// check duplicate or child tag etc
					selectedTagsArray.add(tagsList.getSelectedValue());
					selectedTags.setListData(selectedTagsArray.toArray(new HashMap[0]));
				}
			}
		});
		selectedTags.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					selectedTagsArray.remove(selectedTags.getSelectedValue());
					selectedTags.setListData(selectedTagsArray.toArray(new HashMap[0]));
				}
			}
		});

		selectedTags.addPropertyChangeListener(listener -> {
			System.out.println("selected tags changed");
			// find list<file> files with tags
			List<File> files = new ArrayList<>();
			// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
			String selectSQL = "SELECT files.path FROM files " +
					"INNER JOIN item_tags ON files.id = item_tags.file_id " +
					"INNER JOIN tags ON item_tags.tag_id = tags.id " +
					"WHERE tags.main_tag = ? OR tags.sub_tag = ?;";
			try {
				PreparedStatement stmt = conn.prepareStatement(selectSQL);
				for (HashMap<String, Integer> tag : selectedTagsArray) {
					// sysout tag
					System.out.println(tag.keySet().iterator().next());
					stmt.setString(1, tag.keySet().iterator().next());
					stmt.setString(2, tag.keySet().iterator().next());
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						files.add(new File(rs.getString("path")));
					}
				}
				// if no tags selected
				if (selectedTagsArray.isEmpty()) {
					files = getFiles();
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			listingPanel.removeAll();
			for (File file : files) {
				try {
					createListing(file, listingPanel);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			frame.pack();

		});

		tagSearchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			private void updateSearchResults() {
				String search = tagSearchField.getText();
				// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
				String selectSQL = "SELECT * FROM tags WHERE main_tag LIKE ? OR sub_tag LIKE ?;";
				try {
					PreparedStatement stmt = conn.prepareStatement(selectSQL);
					stmt.setString(1, "%" + search + "%");
					stmt.setString(2, "%" + search + "%");
					ResultSet rs = stmt.executeQuery();

					ArrayList<HashMap<String, Integer>> tagsArray = new ArrayList<>();
					while (rs.next()) {
						HashMap<String, Integer> tag = new HashMap<>();
						tag.put(rs.getString("main_tag"), rs.getInt("id"));
						tagsArray.add(tag);
					}

					tagsList.setListData(tagsArray.toArray(new HashMap[0]));
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				frame.pack();
				// panel.pack();
			}
		});
		frame.add(listingPanel, BorderLayout.CENTER);
		frame.add(tagsPanel, BorderLayout.WEST);
		tagsPanel.setPreferredSize(new Dimension(200, 0));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("MediaSort");

		List<File> files = getFiles();
		for (File file : files) {
			createListing(file, listingPanel);
		}
		// center gui in middle of screen
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

	static void createListing(File file, JPanel parentPanel) throws Exception {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		// panel.setLayout(new GridLayout(2, 1));
		panel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, gray));
		JLabel nameLabel = new JLabel(file.getName());
		JLabel imgLabel = new JLabel();

		if (isImageFile(file)) {
			Image image = ImageIO.read(file);
			Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			imgLabel.setIcon(new ImageIcon(scaledImage));
		} else {
			imgLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
		}

		JButton tagsButton = new JButton("Tags");
		tagsButton.addActionListener(e -> {
			int fileId = 0;
			try {
				String selectSQL = "SELECT id FROM files WHERE path = ?;";
				PreparedStatement stmt = conn.prepareStatement(selectSQL);
				stmt.setString(1, file.getAbsolutePath());
				ResultSet rs = stmt.executeQuery();
				fileId = rs.getInt("id");
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			openTagGUI(fileId);

		});

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		namePanel.add(nameLabel, BorderLayout.CENTER);
		namePanel.add(tagsButton, BorderLayout.EAST);
		panel.add(imgLabel, BorderLayout.CENTER);
		panel.add(namePanel, BorderLayout.SOUTH);

		// TODO: fix not registering when mouse moves a pixel
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						Desktop.getDesktop().open(file);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		parentPanel.add(panel);
		// sysout file name
		System.out.println("a " + file.getName());
	}

	static HashMap<String, Integer> existingTagsTagMap = new HashMap<>();
	static JList<String> existingTagsList = new JList<>(new String[] { "tag" });
	static JScrollPane existingTagsScrollPane;

	static void openTagGUI(int id) {

		JPanel openTagPanel = new JPanel();
		openTagPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;

		JFrame frame = new JFrame();
		frame.setPreferredSize(new Dimension(250, 300));
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, black));
		panel.setLayout(new GridLayout(0, 1));

		JLabel titleLabel = new JLabel();
		String fileName = "";
		// set title label to file name
		selectSQL = "SELECT path FROM files WHERE id = ?;";
		try {
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			String filePath = null;
			if (rs.next())
				filePath = rs.getString("path");
			if (filePath != null) {
				// find file name from path location
				fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
			} else {
				System.out.println("File not found");
			}
		} catch (

		SQLException ex) {
			ex.printStackTrace();
		}
		titleLabel.setText("Tags for " + fileName);
		panel.add(titleLabel);
		c.gridx = 0;
		c.gridy = 0;
		openTagPanel.add(titleLabel, c);

		// ============================================================
		// ================ Existing tags ============================
		// ============================================================

		existingTagsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		existingTagsScrollPane = new JScrollPane(existingTagsList);
		existingTagsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 5;
		openTagPanel.add(existingTagsScrollPane, c);

		getExistingTags(id);

		JButton removeTagButton = new JButton("Remove tag");
		removeTagButton.addActionListener(e -> {
			List<String> selectedTags = existingTagsList.getSelectedValuesList();
			if (selectedTags.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "No tag selected", "Error", JOptionPane.ERROR_MESSAGE);
				// return;
			}
			// remove tag from file
			// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
			String deleteSQL = "DELETE FROM item_tags WHERE file_id = ? AND tag_id = ?;";
			try {
				PreparedStatement stmt = conn.prepareStatement(deleteSQL);
				for (String selectedTag : selectedTags) {
					int tagId = existingTagsTagMap.get(selectedTag);
					stmt.setInt(1, id); // file id
					stmt.setInt(2, tagId); // tag id
					stmt.addBatch();
				}
				stmt.executeBatch();
				getExistingTags(id);
				// JOptionPane.showMessageDialog(frame, "Tags removed", "Success",
				// JOptionPane.INFORMATION_MESSAGE);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		});
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1;
		openTagPanel.add(removeTagButton, c);

		// ============================================================
		// ===================== Add tags ============================
		// ============================================================

		List<String> searchedTags = new ArrayList<>();
		JList<String> searchedTagsList = new JList<>(searchedTags.toArray(new String[0]));
		searchedTagsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		HashMap<String, Integer> tagMap = new HashMap<>();
		JScrollPane scrollPane = new JScrollPane(searchedTagsList);

		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JLabel searchTagLabel = new JLabel("Search for existing tags");
		JTextField searchField = new JTextField();

		// when searchfield is updated
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			private void updateSearchResults() {
				selectSQL = "SELECT * FROM tags WHERE main_tag LIKE ? OR sub_tag LIKE ?;";
				try {
					searchedTags.clear();
					tagMap.clear(); // Clear the previous mappings
					PreparedStatement stmt = conn.prepareStatement(selectSQL);
					stmt.setString(1, "%" + searchField.getText() + "%");
					stmt.setString(2, "%" + searchField.getText() + "%");
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						String tagDisplay = rs.getString("main_tag") + " - " + rs.getString("sub_tag");
						searchedTags.add(tagDisplay);
						tagMap.put(tagDisplay, rs.getInt("id")); // Map tag display to its ID
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				searchedTagsList.setListData(searchedTags.toArray(new String[0]));
			}
		});
		c.gridx = 0;
		c.gridy = 2;
		openTagPanel.add(searchTagLabel, c);
		// panel.add(searchTagLabel);

		JPanel newTagPanel = new JPanel();
		JPanel newTagHolderPanel = new JPanel();
		newTagHolderPanel.setLayout(new GridLayout(1, 2));
		newTagPanel.setLayout(new GridLayout(2, 1));
		newTagPanel.add(searchField);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		openTagPanel.add(searchField, c);
		newTagPanel.add(scrollPane);
		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 5;
		c.gridwidth = 1;
		openTagPanel.add(scrollPane, c);
		JButton addTagButton = new JButton("Add tag");
		addTagButton.addActionListener(e -> {
			List<String> selectedTags = searchedTagsList.getSelectedValuesList();
			if (selectedTags.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "No tag selected", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// add tag to file
			// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
			String insertSQL = "INSERT INTO item_tags (file_id, tag_id) VALUES (?, ?);";
			try {
				PreparedStatement stmt = conn.prepareStatement(insertSQL);
				for (String selectedTag : selectedTags) {

					// is tag dupe/already connected to file id
					String selectSQL = "SELECT COUNT(*) FROM item_tags WHERE file_id = ? AND tag_id = ?;";
					PreparedStatement stmt2 = conn.prepareStatement(selectSQL);
					stmt2.setInt(1, id);
					stmt2.setInt(2, tagMap.get(selectedTag));
					ResultSet rs = stmt2.executeQuery();
					if (rs.getInt(1) > 0) {
						JOptionPane.showMessageDialog(frame, "Tag already connected to file", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					int tagId = tagMap.get(selectedTag);
					stmt.setInt(1, id); // file id
					stmt.setInt(2, tagId); // tag id
					stmt.addBatch();
				}
				stmt.executeBatch();
				getExistingTags(id);
				// JOptionPane.showMessageDialog(frame, "Tags added", "Success",
				// JOptionPane.INFORMATION_MESSAGE);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		});
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 1;
		openTagPanel.add(addTagButton, c);
		newTagHolderPanel.add(newTagPanel);
		// newTagHolderPanel.add(addTagButton);
		// panel.add(newTagHolderPanel);

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
			// delete tag
			// call tagsgui

			String deleteSQL = "DELETE FROM tags WHERE id = ?;";
			try {
				PreparedStatement stmt = conn.prepareStatement(deleteSQL);
				stmt.setInt(1, existingTagsTagMap.get(existingTagsList.getSelectedValue()));
				stmt.executeUpdate();
				getExistingTags(id);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		});
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 2;
		openTagPanel.add(controlPanel, c);
		c.gridx = 1;
		c.gridy = 5;
		openTagPanel.add(createTagButton, c);
		c.gridx = 2;
		c.gridy = 5;
		openTagPanel.add(editTagButton, c);
		controlPanel.add(createTagButton);
		controlPanel.add(editTagButton);
		controlPanel.add(deleteTagButton);
		// panel.add(controlPanel);

		// frame.add(panel);

		JPanel testPanel = new JPanel();
		testPanel.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		JButton test1 = new JButton("test1");
		JButton test2 = new JButton("test2");
		JButton test3 = new JButton("test3");
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 444;
		testPanel.add(test1, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		// c.weightx = 2;
		testPanel.add(test2, c);
		c.gridx = 3;
		c.gridy = 1;
		// c.weightx = 1;
		testPanel.add(test3, c);

		frame.pack();
		// frame.add(panel);
		frame.add(openTagPanel);
		frame.setVisible(true);
		frame.setTitle("MediaSort");
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	static void getExistingTags(int id) {
		List<String> tags = new ArrayList<>();
		existingTagsTagMap.clear();
		selectSQL = "SELECT tags.main_tag, tags.sub_tag, item_tags.tag_id FROM tags " +
				"INNER JOIN item_tags ON tags.id = item_tags.tag_id " +
				"WHERE item_tags.file_id = ?;";
		try {
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String tagDisplay = rs.getString("main_tag");
				tags.add(tagDisplay);
				existingTagsTagMap.put(tagDisplay, rs.getInt("tag_id")); // Map tag display to its ID
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		System.out.println(tags);
		existingTagsList.setListData(tags.toArray(new String[0]));
		existingTagsList.revalidate();
		existingTagsList.repaint();
	}

	static void addTagGUI(JFrame frame, int id) {
		frame.getContentPane().removeAll();
		JPanel addTagPanel = new JPanel();
		addTagPanel.setLayout(new GridLayout(0, 1));
		frame.setPreferredSize(new Dimension(250, 300));

		List<String> searchedTags = new ArrayList<>();
		JList<String> searchedTagsList = new JList<>(searchedTags.toArray(new String[0]));
		searchedTagsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		HashMap<String, Integer> tagMap = new HashMap<>();
		JScrollPane scrollPane = new JScrollPane(searchedTagsList);

		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JLabel titleLabel = new JLabel("Search for existing tags");
		addTagPanel.add(titleLabel);
		JTextField searchField = new JTextField();
		addTagPanel.add(searchField);
		addTagPanel.add(scrollPane);
		// when searchfield is updated
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			private void updateSearchResults() {
				String selectSQL = "SELECT * FROM tags WHERE main_tag LIKE ? OR sub_tag LIKE ?;";
				try {
					searchedTags.clear();
					tagMap.clear(); // Clear the previous mappings
					PreparedStatement stmt = conn.prepareStatement(selectSQL);
					stmt.setString(1, "%" + searchField.getText() + "%");
					stmt.setString(2, "%" + searchField.getText() + "%");
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						String tagDisplay = rs.getString("main_tag") + " - " + rs.getString("sub_tag");
						searchedTags.add(tagDisplay);
						tagMap.put(tagDisplay, rs.getInt("id")); // Map tag display to its ID
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				searchedTagsList.setListData(searchedTags.toArray(new String[0]));
			}
		});

		JButton addTagButton = new JButton("Add tag");
		JButton createTagButton = new JButton("Create new tag");

		addTagButton.addActionListener(e -> {
			List<String> selectedTags = searchedTagsList.getSelectedValuesList();
			if (selectedTags.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "No tag selected", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// add tag to file
			// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
			String insertSQL = "INSERT INTO item_tags (file_id, tag_id) VALUES (?, ?);";
			try {
				PreparedStatement stmt = conn.prepareStatement(insertSQL);
				for (String selectedTag : selectedTags) {

					// is tag dupe/already connected to file id
					String selectSQL = "SELECT COUNT(*) FROM item_tags WHERE file_id = ? AND tag_id = ?;";
					PreparedStatement stmt2 = conn.prepareStatement(selectSQL);
					stmt2.setInt(1, id);
					stmt2.setInt(2, tagMap.get(selectedTag));
					ResultSet rs = stmt2.executeQuery();
					if (rs.getInt(1) > 0) {
						JOptionPane.showMessageDialog(frame, "Tag already connected to file", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					int tagId = tagMap.get(selectedTag);
					stmt.setInt(1, id); // file id
					stmt.setInt(2, tagId); // tag id
					stmt.addBatch();
				}
				stmt.executeBatch();
				JOptionPane.showMessageDialog(frame, "Tags added", "Success", JOptionPane.INFORMATION_MESSAGE);
				frame.dispose();
				openTagGUI(id);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		});

		createTagButton.addActionListener(e -> {
			createTagGUI();
			System.out.println("create tag button clicked");
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			openTagGUI(id);
			frame.dispose();
		});

		// replace frame with addTagPanel
		addTagPanel.add(addTagButton);
		addTagPanel.add(createTagButton);
		addTagPanel.add(cancelButton);

		frame.add(addTagPanel);
		frame.pack();
		frame.requestFocus();
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
		JLabel subTagLabel = new JLabel("Sub Tag:");
		JTextField subTagField = new JTextField();

		createControlPanel.add(mainTagLabel);
		createControlPanel.add(mainTagField);
		createControlPanel.add(subTagLabel);
		createControlPanel.add(subTagField);
		contentPanel.add(createControlPanel);
		JButton addSubTagButton = new JButton("Add Sub Tag");
		List<String> subTagsTemp = new ArrayList<>();
		contentPanel.add(addSubTagButton);

		JList<String> existingSubTagsList = new JList<>(subTagsTemp.toArray(new String[0]));
		// horizontal wrap, scrollable
		existingSubTagsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		JScrollPane scrollPane = new JScrollPane(existingSubTagsList);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		addSubTagButton.addActionListener(e -> {
			String subTag = subTagField.getText();
			if (subTag.isEmpty()) {
				return;
			} // if already exists
			if (subTagsTemp.contains(subTag)) {
				JOptionPane.showMessageDialog(frame, "Sub Tag already exists", "Error", JOptionPane.ERROR_MESSAGE);
				subTagField.setText(subTag);
				return;
			}

			subTagsTemp.add(subTag);
			subTagField.setText("");
			// refresh jlist
			existingSubTagsList.setListData(subTagsTemp.toArray(new String[0]));
		});

		JLabel existingSubTagsLabel = new JLabel("Existing Sub Tags:");

		contentPanel.add(existingSubTagsLabel);
		contentPanel.add(scrollPane);

		JPanel tagControlPanel = new JPanel();
		tagControlPanel.setLayout(new GridLayout(1, 2));
		JButton createTagButton = new JButton("Create Tag");
		createTagButton.addActionListener(e -> {
			if (mainTagField.getText().isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Main Tag cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// is main tag already in db
			String selectSQL = "SELECT COUNT(*) FROM tags WHERE main_tag = ?;";
			try {
				PreparedStatement stmt = conn.prepareStatement(selectSQL);
				stmt.setString(1, mainTagField.getText());
				ResultSet rs = stmt.executeQuery();
				if (rs.getInt(1) > 0) {
					System.out.println("Main Tag already exists");
					// main tag already exists
					JOptionPane.showMessageDialog(frame, "Main Tag already exists", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// add the sub tags from the list to the db
				for (String subTag : subTagsTemp) {
					String insertSubTagSQL = "INSERT INTO tags (main_tag, sub_tag) VALUES (?, ?);";
					stmt = conn.prepareStatement(insertSubTagSQL);
					stmt.setString(1, mainTagField.getText());
					stmt.setString(2, subTag);
					stmt.executeUpdate();
				}

				// add tag to db
				// Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
				/*
				 * String insertSQL = "INSERT INTO tags (main_tag, sub_tag) VALUES (?, ?);";
				 * stmt = conn.prepareStatement(insertSQL);
				 * stmt.setString(1, mainTagField.getText());
				 * stmt.setString(2, subTagField.getText());
				 * stmt.executeUpdate();
				 */
				JOptionPane.showMessageDialog(frame, "Tag created", "Success", JOptionPane.INFORMATION_MESSAGE);
				frame.dispose();
			} catch (SQLException ex) {
				ex.printStackTrace();
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

	static boolean isImageFile(File file) {
		String[] imageExtensions = new String[] { "jpg", "jpeg", "png", "gif", "bmp" };
		for (String extension : imageExtensions) {
			if (file.getName().endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	static List<File> getFiles() throws SQLException {
		String selectSQL = "SELECT * FROM files;";
		PreparedStatement stmt = conn.prepareStatement(selectSQL);
		ResultSet rs = stmt.executeQuery();
		List<File> files = new ArrayList<>();
		int i = 0;
		while (rs.next()) {
			files.add(new File(rs.getString("path")));
			i++;
		}
		return files;
	}
}