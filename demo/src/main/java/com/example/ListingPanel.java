package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;

public class ListingPanel extends JPanel {

	private int panelWidth = 40;
	private int panelHeight = 50;
	private int panelGap = 2;

	private JPanel contentPanel;
	private JScrollPane scrollPane;

	public ListingPanel() {
		setLayout(new BorderLayout());

		contentPanel = new JPanel(null);
		contentPanel.setLayout(null); // We'll use absolute positioning

		scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// Layout panels on resize
		scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutPanels();
			}
		});

		setLayout(new BorderLayout());
		JPanel topPanel = SetUpTopPanel();
		topPanel.setPreferredSize(new Dimension(1, 60));
		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		// Example: add some panels for demonstration
		for (int i = 0; i < 200; i++) {
			JPanel p = new JPanel();
			p.setBackground(new Color((i * 5) % 255, (i * 13) % 255, (i * 17) % 255));
			addPanel(p);
		}

		layoutPanels();
	}

	public void addPanel(JPanel panel) {
		panel.setSize(panelWidth, panelHeight);
		contentPanel.add(panel);
		layoutPanels();
	}

	private JPanel SetUpTopPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		c.insets = new java.awt.Insets(5, 10, 5, 10); // top, left, bottom, right

		// Width
		c.gridx = 0;
		c.weightx = 0;
		panel.add(new JLabel("Width:"), c);
		c.gridx = 1;
		c.weightx = 1;
		JSlider widthSlider = new JSlider(20, 400, panelWidth);
		widthSlider.setPreferredSize(new Dimension(100, 20));
		widthSlider.addChangeListener(e -> {
			panelWidth = widthSlider.getValue();
			layoutPanels();
		});
		panel.add(widthSlider, c);

		// Height
		c.gridx = 2;
		c.weightx = 0;
		panel.add(new JLabel("Height:"), c);
		c.gridx = 3;
		c.weightx = 1;
		JSlider heightSlider = new JSlider(20, 400, panelHeight);
		heightSlider.setPreferredSize(new Dimension(100, 20));
		heightSlider.addChangeListener(e -> {
			panelHeight = heightSlider.getValue();
			layoutPanels();
		});
		panel.add(heightSlider, c);

		// Gap
		c.gridx = 4;
		c.weightx = 0;
		panel.add(new JLabel("Gap:"), c);
		c.gridx = 5;
		c.weightx = 1;
		JSlider gapSlider = new JSlider(0, 20, panelGap);
		gapSlider.setPreferredSize(new Dimension(100, 20));
		gapSlider.addChangeListener(e -> {
			panelGap = gapSlider.getValue();
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
}