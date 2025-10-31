import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ThreeSplitPaneExample {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(ThreeSplitPaneExample::new);
	}

	public ThreeSplitPaneExample() {
		JFrame frame = new JFrame("Three-way SplitPane Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1200, 600);

		JPanel leftPanel = new JPanel();
		leftPanel.setBackground(Color.LIGHT_GRAY);
		leftPanel.add(new JLabel("Left panel"));

		JPanel middlePanel = new JPanel();
		middlePanel.setBackground(Color.WHITE);
		middlePanel.add(new JLabel("Middle panel"));

		JPanel rightPanel = new JPanel();
		rightPanel.setBackground(Color.GRAY);
		rightPanel.add(new JLabel("Right panel"));

		// Left | Middle
		JSplitPane leftMiddleSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, middlePanel);
		leftMiddleSplit.setResizeWeight(0.3); // left takes 30%
		leftMiddleSplit.setContinuousLayout(true);
		leftMiddleSplit.setOneTouchExpandable(true);

		// (Left|Middle) | Right
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftMiddleSplit, rightPanel);
		mainSplit.setResizeWeight(0.7); // right takes 30%
		mainSplit.setContinuousLayout(true);
		mainSplit.setOneTouchExpandable(true);

		// Optional: add divider drag tracking
		attachUserDragListener(leftMiddleSplit);
		attachUserDragListener(mainSplit);

		frame.add(mainSplit, BorderLayout.CENTER);
		frame.setVisible(true);
	}

	private void attachUserDragListener(JSplitPane splitPane) {
		final boolean[] userDragging = { false };
		BasicSplitPaneUI ui = (BasicSplitPaneUI) splitPane.getUI();
		ui.getDivider().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				userDragging[0] = true;
			}

			public void mouseReleased(MouseEvent e) {
				userDragging[0] = false;
			}
		});

		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
			if (userDragging[0]) {
				System.out.println("Divider moved manually: " + splitPane.getDividerLocation());
			}
		});
	}
}
