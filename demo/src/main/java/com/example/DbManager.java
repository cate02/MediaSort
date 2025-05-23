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
			System.out.println(conn);
			System.out.println("Connection to SQLite has been established.");
			if (conn != null) {
				String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "hash TEXT NOT NULL, " + "path TEXT NOT NULL UNIQUE" + ");";
				conn.createStatement().execute(createTableSQL);

				// create table if not exist "tags", linked to the id in "files"
				String createTagsTableSQL = "CREATE TABLE IF NOT EXISTS tags ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "main_tag TEXT NOT NULL UNIQUE," + "sub_tag TEXT"
						+ ");";
				conn.createStatement().execute(createTagsTableSQL);

				String createItemTagsTableSQL = "CREATE TABLE IF NOT EXISTS item_tags ("
						+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "file_id INTEGER NOT NULL, "
						+ "tag_id INTEGER NOT NULL, " + "FOREIGN KEY (file_id) REFERENCES files(id), "
						+ "FOREIGN KEY (tag_id) REFERENCES tags(id)" + ");";
				conn.createStatement().execute(createItemTagsTableSQL);
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
					fileItems.add(new FileItem(file, path, name, id));
				} catch (IOException ex) {
				}
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return fileItems;
	}

}
