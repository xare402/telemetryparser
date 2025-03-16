package com.telemetryparser.datatransformation.uicomponents;

import javax.swing.*;
import java.awt.*;

public class RestrictListDialog extends JDialog
{
	private final JSlider slider;
	private final JTextField textField;
	private final FilterListPanel filterListPanel;
	private Integer result;

	public RestrictListDialog(Window owner, java.util.List<String> columns)
	{
		super(owner, ModalityType.APPLICATION_MODAL);
		setLayout(new BorderLayout());

		filterListPanel = new FilterListPanel(columns);
		slider = new JSlider(1, 100, 10);
		textField = new JTextField("10", 5);

		slider.addChangeListener(e -> textField.setText(String.valueOf(slider.getValue())));

		textField.addActionListener(e ->
		{
			try
			{
				int val = Integer.parseInt(textField.getText());
				slider.setValue(val);
			}
			catch (NumberFormatException ignored)
			{

			}
		});

		JPanel panel = new JPanel();
		panel.add(filterListPanel);
		panel.add(slider);
		panel.add(textField);

		JButton ok = new JButton("OK");
		ok.addActionListener(e ->
		{
			result = slider.getValue();
			dispose();
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e ->
		{
			result = null;
			dispose();
		});

		JPanel btnPanel = new JPanel();
		btnPanel.add(ok);
		btnPanel.add(cancel);

		add(panel, BorderLayout.CENTER);
		add(btnPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	public RestrictListResult showDialog(String title, int initialValue)
	{
		setTitle(title);
		slider.setValue(initialValue);
		textField.setText(String.valueOf(initialValue));
		setVisible(true);
		RestrictListResult restrictListResult = new RestrictListResult(filterListPanel.getSelectedFilters(), result);
		return restrictListResult;
	}
}
