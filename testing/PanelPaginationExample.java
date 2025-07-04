import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class PanelPaginationExample {

	private static final int PANEL_SIZE = 50;
	private static final int TOTAL_PANELS = 1000;
	private static final int PANELS_PER_PAGE = 100;
	private static final int GRID_ROWS = 10;
	private static final int GRID_COLS = 10;

	private int currentPage = 0;
	private List<JPanel> pages;

	public void createAndShowGUI() {
		JFrame frame = new JFrame("1000 Panels with Paging");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 600);

		JPanel contentPanel = new JPanel(new BorderLayout());

		// Create all 1000 panels
		List<JPanel> allPanels = new ArrayList<>();
		for (int i = 0; i < TOTAL_PANELS; i++) {
			JPanel p = new JPanel();
			p.setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));
			p.setBackground(new Color((i * 50) % 255, (i * 80) % 255, (i * 130) % 255));
			p.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			allPanels.add(p);
		}

		// Group panels into pages
		pages = new ArrayList<>();
		for (int i = 0; i < TOTAL_PANELS; i += PANELS_PER_PAGE) {
			JPanel page = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, 2, 2));
			for (int j = i; j < i + PANELS_PER_PAGE && j < TOTAL_PANELS; j++) {
				page.add(allPanels.get(j));
			}
			pages.add(page);
		}

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(pages.get(currentPage));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// Navigation buttons
		JButton prevButton = new JButton("Previous");
		JButton nextButton = new JButton("Next");
		JLabel pageLabel = new JLabel();

		prevButton.addActionListener(e -> switchPage(scrollPane, pageLabel, -1));
		nextButton.addActionListener(e -> switchPage(scrollPane, pageLabel, 1));

		JPanel navigationPanel = new JPanel();
		navigationPanel.add(prevButton);
		navigationPanel.add(pageLabel);
		navigationPanel.add(nextButton);

		updatePageLabel(pageLabel);

		contentPanel.add(scrollPane, BorderLayout.CENTER);
		contentPanel.add(navigationPanel, BorderLayout.SOUTH);

		frame.setContentPane(contentPanel);
		frame.setVisible(true);
	}

	private void switchPage(JScrollPane scrollPane, JLabel pageLabel, int direction) {
		int newPage = currentPage + direction;
		if (newPage >= 0 && newPage < pages.size()) {
			currentPage = newPage;
			scrollPane.setViewportView(pages.get(currentPage));
			updatePageLabel(pageLabel);
		}
	}

	private void updatePageLabel(JLabel label) {
		label.setText("Page " + (currentPage + 1) + " of " + pages.size());
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new PanelPaginationExample().createAndShowGUI());
	}
}
