package com.example.demotests;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class DragSelectDemo extends JFrame {

	private final Set<SquarePanel> selectedPanels = new HashSet<>();
	private boolean mouseDown = false;

	public DragSelectDemo() {
		setTitle("Drag-Select Demo");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400, 400);

		JPanel grid = new JPanel(new GridLayout(3, 3, 5, 5));
		grid.setBackground(Color.DARK_GRAY);

		for (int i = 0; i < 9; i++) {
			SquarePanel square = new SquarePanel();
			addSelectionBehavior(square);
			grid.add(square);
		}

		add(grid, BorderLayout.CENTER);
		setLocationRelativeTo(null);
	}

	private void addSelectionBehavior(SquarePanel panel) {
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseDown = true;
				toggleSelection(panel);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseDown = false;
			}
		});

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (mouseDown) {
					toggleSelection(panel);
				}
			}
		});
	}

	private void toggleSelection(SquarePanel panel) {
		if (panel.isSelected()) {
			panel.setSelected(false);
			selectedPanels.remove(panel);
		} else {
			panel.setSelected(true);
			selectedPanels.add(panel);
		}
	}

	// --- Inner class for square items ---
	static class SquarePanel extends JPanel {
		private boolean selected = false;

		public SquarePanel() {
			setPreferredSize(new Dimension(100, 100));
			setBackground(Color.LIGHT_GRAY);
		}

		public void setSelected(boolean sel) {
			this.selected = sel;
			setBackground(sel ? Color.GREEN : Color.LIGHT_GRAY);
			repaint();
		}

		public boolean isSelected() {
			return selected;
		}
	}

	// --- Main entry ---
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new DragSelectDemo().setVisible(true));
	}
}
