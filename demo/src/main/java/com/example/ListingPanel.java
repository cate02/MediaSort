package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;

public class ListingPanel extends JPanel {
	private boolean debugListing = false;

	private static Preferences preferences;

	public static int panelWidth = 40;
	public static int panelHeight = 50;
	private int panelGap = 2;

	private JPanel contentPanel;
	private JScrollPane scrollPane;
	private JCheckBox autoRefreshCheck = new JCheckBox("Auto Refresh", false);
	private int scrollBarPosition = 0;

	public ListingPanel() {
		preferences = Preferences.userNodeForPackage(MediaSort.class);
		panelWidth = preferences.getInt("panelWidth", 40);
		panelHeight = preferences.getInt("panelHeight", 50);
		panelGap = preferences.getInt("panelGap", 2);
		autoRefreshCheck.setSelected(preferences.getBoolean("autoRefresh", false));
		// remember scroll bar position
		scrollBarPosition = preferences.getInt("scrollBarPosition", 0);

		setLayout(new BorderLayout());

		contentPanel = new JPanel(null);
		contentPanel.setLayout(null);

		scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		// scrollPane.getVerticalScrollBar().setValue(scrollBarPosition);

		// Layout panels on resize
		scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutPanels();
			}
		});

		scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			scrollBarPosition = e.getValue();
			preferences.putInt("scrollBarPosition", scrollBarPosition);
		});

		setLayout(new BorderLayout());
		JPanel topPanel = SetUpTopPanel();
		topPanel.setPreferredSize(new Dimension(1, 60));
		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		// Example: add some panels for demonstration
		if (debugListing) {
			for (int i = 0; i < 200; i++) {
				JPanel p = new JPanel();
				p.setBackground(new Color((i * 5) % 255, (i * 13) % 255, (i * 17) % 255));
				addPanel(p);
			}
		}
		layoutPanels();
		updateImageScales();
	}

	public void addPanel(JPanel panel) {
		panel.setSize(panelWidth, panelHeight);
		contentPanel.add(panel);
		layoutPanels();
		updateImageScales();
	}

	private JPanel SetUpTopPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		c.insets = new java.awt.Insets(5, 10, 5, 10); // top, left, bottom, right

		// Width
		// Use GridBagLayout or similar
		// c.insets = new Insets(2, 4, 2, 4); // optional padding

		// --- Auto-refresh toggle above Width ---
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		autoRefreshCheck.addActionListener(e -> {
			preferences.putBoolean("autoRefresh", autoRefreshCheck.isSelected());
		});
		panel.add(autoRefreshCheck, c);

		// --- Refresh button above Height ---
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(e -> {
			updateImageScales();
			layoutPanels();
		});
		panel.add(refreshButton, c);

		// --- Width Label + Slider (row 1) ---
		c.gridy = 1;
		c.gridwidth = 1;

		c.gridx = 0;
		c.weightx = 0;
		panel.add(new JLabel("Width:"), c);

		c.gridx = 1;
		c.weightx = 1;
		JSlider widthSlider = new JSlider(20, 400, panelWidth);
		widthSlider.setPreferredSize(new Dimension(100, 20));
		widthSlider.addChangeListener(e -> {
			panelWidth = widthSlider.getValue();
			preferences.putInt("panelWidth", panelWidth);
			if (autoRefreshCheck.isSelected()) {
				updateImageScales();
				layoutPanels();
			}
		});
		panel.add(widthSlider, c);

		// --- Height Label + Slider (row 1) ---
		c.gridx = 2;
		c.weightx = 0;
		panel.add(new JLabel("Height:"), c);

		c.gridx = 3;
		c.weightx = 1;
		JSlider heightSlider = new JSlider(20, 400, panelHeight);
		heightSlider.setPreferredSize(new Dimension(100, 20));
		heightSlider.addChangeListener(e -> {
			panelHeight = heightSlider.getValue();
			preferences.putInt("panelHeight", panelHeight);
			if (autoRefreshCheck.isSelected()) {
				updateImageScales();
				layoutPanels();
			}
		});
		panel.add(heightSlider, c);

		// --- Gap Label + Slider (row 1), spans height of two rows ---
		c.gridx = 4;
		c.gridy = 0;
		c.gridheight = 2;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		panel.add(new JLabel("Gap:"), c);

		c.gridx = 5;
		c.weightx = 1;
		JSlider gapSlider = new JSlider(0, 20, panelGap);
		gapSlider.setPreferredSize(new Dimension(100, 40)); // taller for visual balance
		gapSlider.addChangeListener(e -> {
			panelGap = gapSlider.getValue();
			preferences.putInt("panelGap", panelGap);
			layoutPanels();
		});
		panel.add(gapSlider, c);

		return panel;
	}

	private void layoutPanels() {
		int width = scrollPane.getViewport().getWidth();
		if (width == 0)
			width = 600; // fallback for initial layout

		int panelsPerRow = Math.max(1, (width - panelGap) / (panelWidth + panelGap));

		int x = panelGap;
		int y = panelGap;
		int count = 0;
		int totalPanels = contentPanel.getComponentCount();

		for (Component comp : contentPanel.getComponents()) {
			comp.setBounds(x, y, panelWidth, panelHeight);
			count++;
			if (count % panelsPerRow == 0) {
				x = panelGap;
				y += panelHeight + panelGap;
			} else {
				x += panelWidth + panelGap;
			}
		}

		int totalRows = (int) Math.ceil((double) totalPanels / panelsPerRow);
		int totalHeight = totalRows * (panelHeight + panelGap) + panelGap;

		contentPanel.setPreferredSize(new Dimension(width, totalHeight));
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	void updateImageScales() {
		for (FilePanel filePanel : GUI.activeFiles) {
			filePanel.drawImage();
		}
	}
}