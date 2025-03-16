package com.telemetryparser.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AdaptiveGridPanel extends JComponent
{
	private final List<PanelEntry> panelEntries = new ArrayList<>();

	private final JPopupMenu mainPopupMenu = new JPopupMenu();
	private final MouseAdapter popupListener;

	private int rows = 0;
	private int cols = 0;
	public AdaptiveGridPanel()
	{
		setLayout(null);

		popupListener = new MouseAdapter()
		{
			private void maybeShowPopup(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					mainPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowPopup(e);
			}
		};
		addMouseListener(popupListener);
	}

	public AdaptiveGridPanel(int rows, int cols)
	{
		this();
		this.rows = rows;
		this.cols = cols;
	}

	public void addComponent(String label, JComponent component)
	{
		PanelEntry entry = new PanelEntry(label, component);
		panelEntries.add(entry);

		component.addMouseListener(popupListener);
		add(component);

		revalidate();
		repaint();
		rebuildMenu();
	}

	public void removeComponent(String label)
	{
		panelEntries.removeIf(entry ->
		{
			if (entry.label.equals(label))
			{
				if (entry.active)
				{
					remove(entry.component);
					entry.component.removeMouseListener(popupListener);

				}
				return true;
			}
			return false;
		});

		revalidate();
		repaint();
		rebuildMenu();
	}

	public void removeComponent(JComponent component)
	{
		PanelEntry found = null;
		for (PanelEntry entry : panelEntries)
		{
			if (entry.component == component)
			{
				found = entry;
				break;
			}
		}
		if (found != null)
		{
			if (found.active)
			{
				remove(found.component);
			}
			panelEntries.remove(found);
			found.component.removeMouseListener(popupListener);
			revalidate();
			repaint();
			rebuildMenu();
		}
	}

	public void deactivateComponent(String label)
	{
		for (PanelEntry entry : panelEntries)
		{
			if (entry.label.equals(label))
			{
				if (entry.active)
				{
					entry.active = false;
					remove(entry.component);
				}
			}
		}
		revalidate();
		repaint();
		rebuildMenu();
	}

	public void deactivateComponent(JComponent component)
	{
		for (PanelEntry entry : panelEntries)
		{
			if (entry.component == component && entry.active)
			{
				entry.active = false;
				remove(entry.component);
				revalidate();
				repaint();
				rebuildMenu();
				return;
			}
		}
	}

	public void reactivateComponent(String label)
	{
		for (PanelEntry entry : panelEntries)
		{
			if (entry.label.equals(label))
			{
				if (!entry.active)
				{
					entry.active = true;
					add(entry.component);
				}
			}
		}
		revalidate();
		repaint();
		rebuildMenu();
	}

	public void reactivateComponent(JComponent component)
	{
		for (PanelEntry entry : panelEntries)
		{
			if (entry.component == component && !entry.active)
			{
				entry.active = true;
				add(entry.component);
				revalidate();
				repaint();
				rebuildMenu();
				return;
			}
		}
	}

	public void rebuildMenu()
	{
		mainPopupMenu.removeAll();

		for (PanelEntry entry : panelEntries)
		{
			final JCheckBoxMenuItem item = new JCheckBoxMenuItem(entry.label, entry.active);

			item.addActionListener(e ->
			{
				if (item.isSelected())
				{
					if (!entry.active)
					{
						entry.active = true;
						add(entry.component);
						revalidate();
						repaint();
					}
				}
				else
				{
					if (entry.active)
					{
						entry.active = false;
						remove(entry.component);
						revalidate();
						repaint();
					}
				}
			});
			mainPopupMenu.add(item);
		}
		JMenuItem clearAll = new JMenuItem("Hide all");
		clearAll.addActionListener(e ->
		{
			for (PanelEntry panel : panelEntries)
			{
				if (panel.active)
				{
					panel.active = false;
					remove(panel.component);
					rebuildMenu();
					revalidate();
					repaint();
				}
			}
		});
		mainPopupMenu.add(clearAll);
	}

	@Override
	public void doLayout()
	{
		int totalActive = 0;
		for (PanelEntry entry : panelEntries)
		{
			if (entry.active)
			{
				totalActive++;
			}
		}
		if (totalActive == 0)
		{
			return;
		}

		int actualRows = rows;
		int actualCols = cols;

		if (actualRows == 0 && actualCols == 0)
		{
			int side = (int) Math.ceil(Math.sqrt(totalActive));
			actualRows = side;
			actualCols = side;
		}
		else if (actualRows == 0)
		{
			actualRows = (int) Math.ceil((double) totalActive / actualCols);
		}
		else if (actualCols == 0)
		{
			actualCols = (int) Math.ceil((double) totalActive / actualRows);
		}

		int minComponentHeight = 50;
		int maxRowsBasedOnHeight = getHeight() / minComponentHeight;

		if (actualRows > maxRowsBasedOnHeight)
		{
			actualRows = maxRowsBasedOnHeight;
		}

		if (actualRows < 1)
		{
			return;
		}

		int cellWidth = getWidth() / Math.max(1, actualCols);
		int cellHeight = getHeight() / actualRows;


		int index = 0;
		for (PanelEntry entry : panelEntries)
		{
			if (entry.active)
			{
				if (index >= actualRows * actualCols)
				{
					break;
				}
				int r = index / actualCols;
				int c = index % actualCols;
				int x = c * cellWidth;
				int y = r * cellHeight;

				entry.component.setBounds(x, y, cellWidth, cellHeight);
				index++;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(400, 300);
	}

	public int getRows()
	{
		return rows;
	}

	public void setRows(int rows)
	{
		this.rows = rows;
		revalidate();
		repaint();
	}

	public int getCols()
	{
		return cols;
	}

	public void setCols(int cols)
	{
		this.cols = cols;
		revalidate();
		repaint();
	}

	private static class PanelEntry
	{
		final String label;
		final JComponent component;
		boolean active;

		PanelEntry(String label, JComponent component)
		{
			this.label = label;
			this.component = component;
			this.active = true;
		}
	}
}
