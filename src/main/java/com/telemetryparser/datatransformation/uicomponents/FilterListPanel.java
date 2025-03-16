package com.telemetryparser.datatransformation.uicomponents;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

public class FilterListPanel extends JPanel
{

	private final JRadioButton includeRadioButton;
	private final JRadioButton excludeRadioButton;
	private final List<JCheckBox> checkBoxes;

	public FilterListPanel(List<String> columns)
	{
		setLayout(new BorderLayout());

		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		includeRadioButton = new JRadioButton("Include", true);
		excludeRadioButton = new JRadioButton("Exclude");

		ButtonGroup group = new ButtonGroup();
		group.add(includeRadioButton);
		group.add(excludeRadioButton);

		radioPanel.add(includeRadioButton);
		radioPanel.add(excludeRadioButton);

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

		checkBoxes = new ArrayList<>();
		for (String col : columns)
		{
			JCheckBox checkBox = new JCheckBox(col, true);
			checkBoxes.add(checkBox);
			checkBoxPanel.add(checkBox);
		}

		JScrollPane scrollPane = new JScrollPane(checkBoxPanel);

		add(radioPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	public Set<String> getSelectedFilters()
	{
		Set<String> selected = new HashSet<>();
		for (JCheckBox cb : checkBoxes)
		{
			if (cb.isSelected())
			{
				selected.add(cb.getText());
			}
		}
		return selected;
	}

	public boolean isExclude()
	{
		return excludeRadioButton.isSelected();
	}

	public boolean isInclude()
	{
		return includeRadioButton.isSelected();
	}
}
