import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class SmartGridExample extends JFrame {

	private static final int PANEL_SIZE = 50;
	private static final int PANEL_GAP = 2;
	private static final int TOTAL_PANELS = 1000;

	private JPanel container;

	public SmartGridExample() {
		setTitle("Smart Grid Layout (Square Panels, Scrollable)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);

		container = new JPanel(null); // we'll manually lay out components
		container.setPreferredSize(new Dimension(0, 0)); // dynamic

		JScrollPane scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane);

		// Create all 1000 panels
		for (int i = 0; i < TOTAL_PANELS; i++) {
			JPanel p = new JPanel();
			p.setBackground(new Color((i * 5) % 255, (i * 13) % 255, (i * 17) % 255));
			p.setSize(PANEL_SIZE, PANEL_SIZE);
			p.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			container.add(p);
		}

		// Layout on resize
		container.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutPanels();
			}
		});

		// Layout initially
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				layoutPanels();
			}

			public void componentResized(ComponentEvent e) {
				layoutPanels();
			}
		});

		setVisible(true);
	}

	private void layoutPanels() {
		int width = container.getParent().getWidth(); // scroll pane viewport width
		int panelsPerRow = Math.max(1, (width - PANEL_GAP) / (PANEL_SIZE + PANEL_GAP));

		int x = PANEL_GAP;
		int y = PANEL_GAP;
		int count = 0;

		for (Component comp : container.getComponents()) {
			comp.setBounds(x, y, PANEL_SIZE, PANEL_SIZE);
			count++;
			if (count % panelsPerRow == 0) {
				x = PANEL_GAP;
				y += PANEL_SIZE + PANEL_GAP;
			} else {
				x += PANEL_SIZE + PANEL_GAP;
			}
		}

		int totalRows = (int) Math.ceil((double) TOTAL_PANELS / panelsPerRow);
		int totalHeight = totalRows * (PANEL_SIZE + PANEL_GAP) + PANEL_GAP;

		container.setPreferredSize(new Dimension(width, totalHeight));
		container.revalidate();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(SmartGridExample::new);
	}
}
