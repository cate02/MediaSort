package com.example;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TagsGUI {
	public static void main(String[] args) {
		JFrame frame = new JFrame("Tags");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 300);
		JPanel panel = new JPanel();
		panel.add(new JLabel("test"));
		frame.add(panel);
		frame.setVisible(true);
		System.out.println("teddst");
	}
}
