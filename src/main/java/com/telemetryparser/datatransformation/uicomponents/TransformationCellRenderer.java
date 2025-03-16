package com.telemetryparser.datatransformation.uicomponents;

import com.telemetryparser.datatransformation.steps.TransformationStep;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TransformationCellRenderer implements ListCellRenderer<TransformationStep>
{
	private final JPanel rendererPanel = new JPanel(null);
	private final JLabel nameLabel = new JLabel();
	private final JLabel removeLabel = new JLabel("X");
	private final Map<TransformationStep, Rectangle> removeRects = new HashMap<>();

	public TransformationCellRenderer()
	{
		rendererPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		rendererPanel.add(nameLabel);
		rendererPanel.add(removeLabel);
	}

	@Override
	public Component getListCellRendererComponent(
		JList<? extends TransformationStep> list,
		TransformationStep value,
		int index,
		boolean isSelected,
		boolean cellHasFocus)
	{
		rendererPanel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
		rendererPanel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

		nameLabel.setText(value.getName());
		nameLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
		removeLabel.setForeground(Color.RED);

		int w = list.getWidth();
		int h = 30;
		rendererPanel.setPreferredSize(new Dimension(w, h));

		nameLabel.setBounds(5, 5, w - 40, 20);
		removeLabel.setBounds(w - 30, 5, 20, 20);

		Rectangle xRect = new Rectangle(removeLabel.getX(), removeLabel.getY(), removeLabel.getWidth(), removeLabel.getHeight());
		removeRects.put(value, xRect);

		return rendererPanel;
	}

	public Rectangle getRemoveRect(TransformationStep step)
	{
		return removeRects.getOrDefault(step, new Rectangle(0, 0, 0, 0));
	}
}
