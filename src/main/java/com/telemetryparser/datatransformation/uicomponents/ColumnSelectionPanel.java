package com.telemetryparser.datatransformation.uicomponents;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ColumnSelectionPanel extends JPanel
{
	private final JPanel checkPanel;
	private final Set<JCheckBox> checkBoxes = new LinkedHashSet<>();
	private Consumer<Set<String>> onSelectionChanged;
	private boolean suppressNotification = false;

	public ColumnSelectionPanel()
	{
		setLayout(new BorderLayout());
		checkPanel = new JPanel();
		checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
		add(new JScrollPane(checkPanel), BorderLayout.CENTER);
	}

	public void setColumns(Set<String> columns)
	{
		suppressNotification = true;
		checkPanel.removeAll();
		checkBoxes.clear();
		for (String col : columns)
		{
			JCheckBox box = new JCheckBox(col, true);
			box.addActionListener(e -> notifySelection());
			checkBoxes.add(box);
			checkPanel.add(box);
		}
		revalidate();
		repaint();
		suppressNotification = false;
	}

	public Set<String> getAllColumns()
	{
		Set<String> cols = new LinkedHashSet<>();
		for (JCheckBox box : checkBoxes)
		{
			cols.add(box.getText());
		}
		return cols;
	}

	public Set<String> getSelectedColumns()
	{
		Set<String> selected = new LinkedHashSet<>();
		for (JCheckBox box : checkBoxes)
		{
			if (box.isSelected())
			{
				selected.add(box.getText());
			}
		}
		return selected;
	}

	public void setSelectedColumns(Set<String> columnsToSelect)
	{
		suppressNotification = true;
		for (JCheckBox box : checkBoxes)
		{
			box.setSelected(columnsToSelect.contains(box.getText()));
		}
		suppressNotification = false;
	}

	public void addColumnSelectionListener(Consumer<Set<String>> listener)
	{
		this.onSelectionChanged = listener;
	}

	private void notifySelection()
	{
		if (!suppressNotification && onSelectionChanged != null)
		{
			onSelectionChanged.accept(getSelectedColumns());
		}
	}
}
