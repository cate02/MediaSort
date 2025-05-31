package com.example;

import java.io.File;
import java.io.IOException;

public class FileItem {
	public final File file;
	public final String path;
	public final String name;
	public final int id;
	public boolean isSelected;

	public FileItem(File file, String path, String name, int id, boolean isSelected) throws IOException {
		this.file = file;
		this.path = path;
		this.name = name;
		this.id = id;
		this.isSelected = isSelected;
		FilePanel filePanel = new FilePanel(this);
	}
}
