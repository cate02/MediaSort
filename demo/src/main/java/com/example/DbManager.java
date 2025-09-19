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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class DbManager {

	private static Connection conn;

	List<FileItem> fileItems;

	static String dbPath;
	static String dbPathName = "MediaSort";
	static String dbName = "mediasort";
	static String contentPath;
	private static Preferences preferences;
	static String tempConnDir;
	public static TagManager tagManager;

	DbManager() {
		preferences = Preferences.userNodeForPackage(MediaSort.class);
		getContentPath();
		System.out.println(dbPath + "\n" + contentPath);
	}

	static void createDatabase() {
		try {
			tempConnDir = "jdbc:sqlite:" + dbPath + "\\" + dbName + ".db";
			conn = DriverManager.getConnection(tempConnDir);
			conn.createStatement().execute("PRAGMA foreign_keys = ON;");

			System.out.println("Connection to SQLite has been established. " + tempConnDir);
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
				// file_tags.db id, FK:file_id, FK:tag_id
				String createFileTagsDbTableSQL = "CREATE TABLE IF NOT EXISTS file_tags ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "file_id INTEGER NOT NULL, "
						+ "tag_id INTEGER NOT NULL, " + "FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE, "
						+ "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE);";
				conn.createStatement().execute(createFileTagsDbTableSQL);
				// aliases.db id, alias
				String createAliasesDbTableSQL = "CREATE TABLE IF NOT EXISTS aliases ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "alias TEXT NOT NULL UNIQUE);";
				conn.createStatement().execute(createAliasesDbTableSQL);
				// tag_aliases.db id, FK:tag_id, FK:alias_id
				String createTagAliasesDbTableSQL = "CREATE TABLE IF NOT EXISTS tag_aliases ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "tag_id INTEGER NOT NULL, "
						+ "alias_id INTEGER NOT NULL, " + "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, "
						+ "FOREIGN KEY (alias_id) REFERENCES aliases(id) ON DELETE CASCADE);";
				conn.createStatement().execute(createTagAliasesDbTableSQL);

				System.out.println("Database and tables created successfully.");
				processDirectory(contentPath);
			}

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	private static void getContentPath() {

		contentPath = preferences.get("contentPath", "");
		dbPath = contentPath + "\\" + dbPathName;
		if (!new File(dbPath).exists()) {
			System.out.println("path " + dbPath + " does not exist, creating it");
			File dbDir = new File(dbPath);
			if (!dbDir.exists()) {
				dbDir.mkdir();
				System.out.println("Directory created: " + dbPath);
			}

			// contentPath = null;
			// preferences.put("contentPath", "");
			// getContentPath();
		}

		if (contentPath == null || contentPath.isEmpty()) {
			System.out.println("no content path set, changing directory");
			changeDirectory();

		}
		if (contentPath == null || contentPath.isEmpty()) {
			System.out.println("fuck");
		} else {
			dbPath = contentPath + "\\" + dbPathName;
			System.out.println("DB Path: " + dbPath);
			File dbFile = new File(dbPath);
			if (!dbFile.exists()) {
				dbFile.mkdir();
				System.out.println("Directory created: " + dbPath);
			}
			createDatabase();
		}

	}

	public static void changeDirectory() {
		javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
		chooser.setDialogTitle("Select Content Directory");
		chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		// set chooser default directory to contentPath
		if (contentPath != null && !contentPath.isEmpty()) {
			chooser.setCurrentDirectory(new File(contentPath));
		} else {
			chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		}
		int result = chooser.showOpenDialog(null);
		if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
			contentPath = chooser.getSelectedFile().getAbsolutePath();
			preferences.put("contentPath", contentPath); // Save for next time
			getContentPath();
			/*
			 * dbPath = contentPath + "\\" + dbPathName; System.out.println("DB Path: " +
			 * dbPath); File dbFile = new File(dbPath); if (!dbFile.exists()) {
			 * dbFile.mkdir(); System.out.println("Directory created: " + dbPath); }
			 */
		} else {
			JOptionPane.showMessageDialog(null, "No directory selected.", "Error", JOptionPane.ERROR_MESSAGE);
			// System.exit(0);
		}
		tagManager.refreshTagManager();
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
				// System.out.println("File: " + filePath);
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
						// System.out.println("Duplicate file: " + filePath);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			getFileItems();
		}

		catch (IOException e) {
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
				FileItem temp = new FileItem(file, path, name, id, false);
				temp.tagsList = findTags(temp);
				fileItems.add(temp);
				i++;
			}
		} catch (Exception e) {
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
				FileItem temp = new FileItem(file, path, name, id, false);
				temp.tagsList = findTags(temp);
				files.add(temp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return files;
	}

	public static int findAliasId(String alias) {
		int id = 0;
		try {
			String selectSQL = "SELECT id FROM aliases WHERE alias = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, alias);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				id = rs.getInt("id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	public static int findTagId(String tag) {
		int id = 0;
		try {
			String selectSQL = "SELECT id FROM tags WHERE tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, tag);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				id = rs.getInt("id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	public static int findConnectionId(String connection) {
		int id = 0;
		try {
			String selectSQL = "SELECT id FROM tags WHERE tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, connection);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				id = rs.getInt("id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	public static List<String> findTags(FileItem file) {

		List<FileItem> files = new ArrayList<>();
		files.add(file);
		return findTags(files);
	}

	public static List<String> findTags(List<FileItem> files) {
		List<String> tags = new ArrayList<>();
		// find all tags for list of files, include any duplicate tags into the list
		try {
			StringBuilder query = new StringBuilder(
					"SELECT t.tag FROM tags t INNER JOIN file_tags ft ON t.id = ft.tag_id INNER JOIN files f ON ft.file_id = f.id WHERE f.id IN (");
			for (int i = 0; i < files.size(); i++) {
				query.append("?");
				if (i < files.size() - 1) {
					query.append(", ");
				}
			}
			query.append(");");
			PreparedStatement stmt = conn.prepareStatement(query.toString());
			for (int i = 0; i < files.size(); i++) {
				stmt.setInt(i + 1, files.get(i).id);
			}
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
		if (doesTagExist(tag) || doesAliasExist(tag))
			System.err.println("how was this called");
		try {
			String insertSubTagSQL = "INSERT INTO tags (tag) VALUES (?);";
			PreparedStatement stmt = conn.prepareStatement(insertSubTagSQL);
			stmt.setString(1, tag);
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("Error creating tag: " + ex.getMessage());
			return 0;
		}
		if (doesTagExist(tag))
			return 1;
		else
			return 0;
	}

	public static int createTagAlias(String tag, String alias) {
		try {
			String insertSubTagSQL = "INSERT INTO tag_aliases (tag_id, alias_id) VALUES ((SELECT id FROM tags WHERE tag = ?), (SELECT id FROM aliases WHERE alias = ?));";
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
			String selectSQL = "SELECT t.tag, t.id FROM tags t LEFT JOIN tag_aliases ta ON t.id = ta.tag_id LEFT JOIN aliases a ON ta.alias_id = a.id WHERE t.tag LIKE ? OR a.alias LIKE ?;";
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

	public static int deleteTag(String tag) {
		try {
			String deleteSQL = "DELETE FROM tags WHERE tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			stmt.setString(1, tag);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		if (doesTagExist(tag)) {
			return 0;
		} else {
			return 1;
		}
	}

	public static List<String> getTagsForAlias(String alias) {
		List<String> tags = new ArrayList<>();
		try {
			String selectSQL = "SELECT t.tag FROM tags t INNER JOIN tag_aliases ta ON t.id = ta.tag_id INNER JOIN aliases a ON ta.alias_id = a.id WHERE a.alias = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, alias);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				tags.add(rs.getString("tag"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tags;
	}

	public static List<String> getFilesForTag(String tag) {
		List<String> files = new ArrayList<>();
		try {
			String selectSQL = "SELECT f.path FROM files f INNER JOIN file_tags ft ON f.id = ft.file_id INNER JOIN tags t ON ft.tag_id = t.id WHERE t.tag = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, tag);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				// exclude dbpath section from path
				String path = rs.getString("path");
				if (path.contains(contentPath)) {
					path = path.substring(path.indexOf(contentPath) + contentPath.length());
				}
				files.add(path);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return files;
	}

	public static void deleteTagFromFiles(String tag, List<FileItem> files) {
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

	public static Map<String, Boolean> findAliases(String tag, String search) {
		Map<String, Boolean> aliasesMap = new LinkedHashMap<>();
		// add all aliases.alias in string, then for all aliases map, if taghasalias,
		// boolean=1
		try {
			String selectSQL = "SELECT alias FROM aliases WHERE alias LIKE ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, "%" + search + "%");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String alias = rs.getString("alias");
				boolean hasAlias = tagHasAlias(tag, alias);
				aliasesMap.put(alias, hasAlias);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return aliasesMap;
	}

	public static Map<String, Boolean> findConnections(String tag, String search) {
		Map<String, Boolean> connsMap = new LinkedHashMap<>();
		// return all tags.tag
		// if search like aliases.alias, also return tags.tag that has it's tag_id and
		// tag_aliases.alias_id
		// for all results, if String tag's tag_id is in tag_connections, make
		// appropriate returned connections have a true boolean
		try {
			String selectSQL = "SELECT t.tag FROM tags t LEFT JOIN tag_aliases ta ON t.id = ta.tag_id LEFT JOIN aliases a ON ta.alias_id = a.id WHERE t.tag LIKE ? OR a.alias LIKE ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, "%" + search + "%");
			stmt.setString(2, "%" + search + "%");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String connTag = rs.getString("tag");
				boolean hasConn = tagHasConn(tag, connTag);
				connsMap.put(connTag, hasConn);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return connsMap;
	}

	public static boolean tagHasAlias(String tag, String alias) {
		boolean result = false;
		// if in tag_aliases tag_id has alias_id
		try {
			String selectSQL = "SELECT COUNT(*) FROM tag_aliases WHERE tag_id = (SELECT id FROM tags WHERE tag = ?) AND alias_id = (SELECT id FROM aliases WHERE alias = ?);";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, tag);
			stmt.setString(2, alias);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static boolean tagHasConn(String tag, String connTag) {
		boolean result = false;
		// if in tag_connections tag_id has connection_id
		try {
			String selectSQL = "SELECT COUNT(*) FROM tag_connections WHERE tag_id = (SELECT id FROM tags WHERE tag = ?) AND connection_tag_id = (SELECT id FROM tags WHERE tag = ?);";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, tag);
			stmt.setString(2, connTag);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void addConnToTag(String tag, String connection) {
		try {
			String insertSQL = "INSERT INTO tag_connections (tag_id, connection_tag_id) VALUES ((SELECT id FROM tags WHERE tag = ?), (SELECT id FROM tags WHERE tag = ?));";
			PreparedStatement stmt = conn.prepareStatement(insertSQL);
			stmt.setString(1, tag);
			stmt.setString(2, connection);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void removeConnFromTag(String tag, String connection) {
		try {
			String deleteSQL = "DELETE FROM tag_connections WHERE tag_id = (SELECT id FROM tags WHERE tag = ?) AND connection_tag_id = (SELECT id FROM tags WHERE tag = ?);";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			stmt.setString(1, tag);
			stmt.setString(2, connection);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean isUniqueTagAlias(String text) {
		return !doesTagExist(text) && !doesAliasExist(text);
	}

	public static int createAlias(String alias) {
		if (doesAliasExist(alias) || doesTagExist(alias))
			System.err.println("how was this called");
		try {
			String insertAliasSQL = "INSERT INTO aliases (alias) VALUES (?);";
			PreparedStatement stmt = conn.prepareStatement(insertAliasSQL);
			stmt.setString(1, alias);
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("Error creating alias: " + ex.getMessage());
			return 0;
		}
		if (doesAliasExist(alias))
			return 1;
		else
			return 0;
	}

	public static int deleteAlias(String alias) {
		try {
			String deleteSQL = "DELETE FROM aliases WHERE alias = ?;";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			stmt.setString(1, alias);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		if (doesAliasExist(alias)) {
			return 0;
		} else {
			return 1;
		}
	}

	public static boolean doesAliasExist(String alias) {
		try {
			String selectSQL = "SELECT COUNT(*) FROM aliases WHERE alias = ?;";
			PreparedStatement stmt = conn.prepareStatement(selectSQL);
			stmt.setString(1, alias);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void addAliasToTag(String tag, String alias) {
		try {
			String insertSQL = "INSERT INTO tag_aliases (tag_id, alias_id) VALUES ((SELECT id FROM tags WHERE tag = ?), (SELECT id FROM aliases WHERE alias = ?));";
			PreparedStatement stmt = conn.prepareStatement(insertSQL);
			stmt.setString(1, tag);
			stmt.setString(2, alias);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void removeAliasFromTag(String tag, String alias) {
		try {
			String deleteSQL = "DELETE FROM tag_aliases WHERE tag_id = (SELECT id FROM tags WHERE tag = ?) AND alias_id = (SELECT id FROM aliases WHERE alias = ?);";
			PreparedStatement stmt = conn.prepareStatement(deleteSQL);
			stmt.setString(1, tag);
			stmt.setString(2, alias);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
