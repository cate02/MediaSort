package com.example;

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
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class DbManager {

	private static Connection conn;

	List<FileItem> fileItems;

	static String dbPath;
	static String dbPathName = "MediaSort";
	static String contentPath;

	DbManager() {
		getContentPath();
		createDatabase();
		// find new/moved files
		processDirectory(contentPath);
		// from db compile list of files
		getFileItems();
	}

	private void createDatabase() {
		try {
			String tempConnDir = "jdbc:sqlite:" + dbPath + "\\db.db";
			conn = DriverManager.getConnection(tempConnDir);
			conn.createStatement().execute("PRAGMA foreign_keys = ON;");

			System.out.println(conn);
			System.out.println("Connection to SQLite has been established.");
			if (conn != null) {
				// create if not exist tags, PK id, not null string tag
				String createTagsTableSQL = "CREATE TABLE IF NOT EXISTS tags ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "tag TEXT NOT NULL UNIQUE);";
				conn.createStatement().execute(createTagsTableSQL);
				// file pk id hash path
				String createFilesTableSQL = "CREATE TABLE IF NOT EXISTS files ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "hash TEXT NOT NULL, "
						+ "path TEXT NOT NULL UNIQUE);";
				conn.createStatement().execute(createFilesTableSQL);
				// file tags ikd file_id tag_id
				String createFileTagsTableSQL = "CREATE TABLE IF NOT EXISTS file_tags ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "file_id INTEGER NOT NULL, "
						+ "tag_id INTEGER NOT NULL, " + "FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE, "
						+ "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE);";
				conn.createStatement().execute(createFileTagsTableSQL);
				// tag connections id fk parent_tag_id, child_tag_id
				String createTagConnectionsTableSQL = "CREATE TABLE IF NOT EXISTS tag_connections ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "parent_tag_id INTEGER NOT NULL, "
						+ "child_tag_id INTEGER NOT NULL, "
						+ "FOREIGN KEY (parent_tag_id) REFERENCES tags(id) ON DELETE CASCADE, "
						+ "FOREIGN KEY (child_tag_id) REFERENCES tags(id) ON DELETE CASCADE);";
				conn.createStatement().execute(createTagConnectionsTableSQL);
				// tag_aliases id, fk tag_id, alias
				String createTagAliasesTableSQL = "CREATE TABLE IF NOT EXISTS tag_aliases ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "tag_id INTEGER NOT NULL, "
						+ "alias TEXT NOT NULL UNIQUE, "
						+ "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE);";
				conn.createStatement().execute(createTagAliasesTableSQL);
				System.out.println("Database and tables created successfully.");
			}

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	private void getContentPath() {
		System.out.println("DbManager constructor");
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
		dbPath = contentPath + "\\" + dbPathName;
		System.out.println("DB Path: " + dbPath);
		File dbFile = new File(dbPath);
		if (!dbFile.exists()) {
			dbFile.mkdir();
			System.out.println("Directory created: " + dbPath);
		}
	}

	private static void processDirectory(String directoryPath) {
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
		try {
			Files.walk(directory.toPath()).filter(Files::isRegularFile).forEach(filePath -> {
				System.out.println("File: " + filePath);
				// if directory contains folder dbpath
				if (filePath.toString().contains(dbPathName)) {
					System.out.println("Directory contains db path");
					return; // Use 'continue' in a loop, but here in lambda, use return to skip
					// this
					// iteration
				}
				File newFile = filePath.toFile();
				try {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	/*
	 * public static List<File> getFiles() throws SQLException { String selectSQL =
	 * "SELECT * FROM files;"; PreparedStatement stmt =
	 * conn.prepareStatement(selectSQL); ResultSet rs = stmt.executeQuery();
	 * List<File> files = new ArrayList<>(); int i = 0; while (rs.next()) {
	 * files.add(new File(rs.getString("path"))); i++; } return files; }
	 */
	public static List<FileItem> getFileItems() {
		// fill fileitems from db
		List<FileItem> fileItems = new ArrayList<>();
		try {
			String selectSQL = "SELECT * FROM files;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			ResultSet rs = stmt.executeQuery();
			int i = 0;
			while (rs.next()) {
				File file = new File(rs.getString("path"));
				String path = rs.getString("path");
				String name = file.getName();
				int id = rs.getInt("id");
				try {
					fileItems.add(new FileItem(file, path, name, id, false));
				} catch (IOException ex) {
				}
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return fileItems;
	}

	public static List<FileItem> findFiles(List<String> tags) {
		List<FileItem> files = new ArrayList<>();
		try {
			StringBuilder query = new StringBuilder(
					"SELECT DISTINCT f.* FROM files f INNER JOIN file_tags ft ON f.id = ft.file_id INNER JOIN tags t ON ft.tag_id = t.id WHERE t.tag IN (");
			for (int i = 0; i < tags.size(); i++) {
				query.append("?");
				if (i < tags.size() - 1) {
					query.append(", ");
				}
			}
			query.append(") GROUP BY f.id HAVING COUNT(DISTINCT t.tag) = ?;");
			PreparedStatement stmt = conn.prepareStatement(query.toString());
			for (int i = 0; i < tags.size(); i++) {
				stmt.setString(i + 1, tags.get(i));
			}
			stmt.setInt(tags.size() + 1, tags.size());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				File file = new File(rs.getString("path"));
				String path = rs.getString("path");
				String name = file.getName();
				int id = rs.getInt("id");
				files.add(new FileItem(file, path, name, id, false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return files;
	}

	public static List<String> findTags(List<FileItem> files) {
		List<String> tags = new ArrayList<>();
		try {
			StringBuilder query = new StringBuilder(
					"SELECT DISTINCT t.tag FROM tags t INNER JOIN file_tags ft ON t.id = ft.tag_id WHERE ft.file_id IN (");
			for (int i = 0; i < files.size(); i++) {
				query.append("?");
				if (i < files.size() - 1) {
					query.append(", ");
				}
			}
			query.append(") GROUP BY t.id HAVING COUNT(DISTINCT ft.file_id) = ?;");
			PreparedStatement stmt = conn.prepareStatement(query.toString());
			for (int i = 0; i < files.size(); i++) {
				stmt.setInt(i + 1, files.get(i).id);
			}
			stmt.setInt(files.size() + 1, files.size());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				tags.add(rs.getString("tag"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tags;
	}

	public static void addTagToFiles(List<FileItem> files, String tag) {
		try {
			String insertSQL = "INSERT INTO file_tags (file_id, tag_id) VALUES (?, (SELECT id FROM tags WHERE tag = ?));";
			PreparedStatement stmt = conn.prepareStatement(insertSQL);
			for (FileItem file : files) {
				stmt.setInt(1, file.id);
				stmt.setString(2, tag);
				stmt.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean doesTagExist(String text) {
		try {
			String selectSQL = "SELECT COUNT(*) FROM tags WHERE tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, text);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static int createTag(String tag) {
		try {
			String insertSubTagSQL = "INSERT INTO tags (tag) VALUES (?);";
			PreparedStatement stmt = conn.prepareStatement(insertSubTagSQL);
			stmt.setString(1, tag);
			stmt.executeUpdate();
		} catch (SQLException ex) {
		}
		if (doesTagExist(tag))
			return 1;
		else
			return 0;
	}

	public static int createTagAlias(String tag, String alias) {
		try {
			String insertSubTagSQL = "INSERT INTO tag_aliases (tag_id, alias) VALUES ((SELECT id FROM tags WHERE tag = ?), ?);";
			PreparedStatement stmt = conn.prepareStatement(insertSubTagSQL);
			stmt.setString(1, tag);
			stmt.setString(2, alias);
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("Error creating tag alias: " + ex.getMessage());
			return 0;
		}
		return 1;
	}

	public static List<String> findTags(String search) {
		// select tags.tag from tags t left join tag_aliases ta on t.id = ta.tag_id
		// where t.tag like ? or ta.alias like ?;
		List<String> tags = new ArrayList<>();
		try {
			String selectSQL = "SELECT t.tag, t.id FROM tags t LEFT JOIN tag_aliases ta ON t.id = ta.tag_id WHERE t.tag LIKE ? OR ta.alias LIKE ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, "%" + search + "%");
			stmt.setString(2, "%" + search + "%");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				tags.add(rs.getString("tag"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tags;
	}

	public static void DeleteTag(String tag) {
		try {
			String deleteSQL = "DELETE FROM tags WHERE tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			stmt.setString(1, tag);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void DeleteTagFromFiles(String tag, List<FileItem> files) {
		try {
			String deleteSQL = "DELETE FROM file_tags WHERE file_id = ? AND tag_id = (SELECT id FROM tags WHERE tag = ?);";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			for (FileItem file : files) {
				stmt.setInt(1, file.id);
				stmt.setString(2, tag);
				stmt.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
