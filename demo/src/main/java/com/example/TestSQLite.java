package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestSQLite {
	public static void main(String[] args) {
		try {
			// Create a new database file named testdb.db in the current directory
			Connection conn = DriverManager.getConnection("jdbc:sqlite:testdb.db");
			System.out.println("Connected!");

			// Create a table
			String createTable = "CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY, name TEXT);";
			conn.createStatement().execute(createTable);

			// Insert a row
			String insert = "INSERT INTO test (name) VALUES (?);";
			PreparedStatement pstmt = conn.prepareStatement(insert);
			pstmt.setString(1, "HelloWorld");
			pstmt.executeUpdate();

			// Read and print all rows
			ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM test;");
			while (rs.next()) {
				System.out.println("Row: id=" + rs.getInt("id") + ", name=" + rs.getString("name"));
			}

			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}