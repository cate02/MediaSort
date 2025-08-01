
package com.example;

public class MediaSort {

	public static void main(String[] args) {
		DbManager dbManager = new DbManager();
		GUI gui = new GUI();
		InfoPanel.updateSearchTags();
		ListingPanel.updateImageScales();
		ListingPanel.layoutPanels();
		gui.frame.setVisible(true);
	}
}