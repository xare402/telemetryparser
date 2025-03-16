package com.telemetryparser.datatransformation.uicomponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class InterpolateAndDerive2xDialog extends JDialog
{
	private final JComboBox<String> inColumnCombo;
	private final JTextField interpolatedColumnField;
	private final JTextField firstDerivativeField;
	private final JTextField secondDerivativeField;
	private boolean confirmed;

	public InterpolateAndDerive2xDialog(Window owner, List<String> allColumns)
	{
		super(owner, "Interpolate & Derive 2x", ModalityType.APPLICATION_MODAL);
		setLayout(new GridLayout(5, 2, 5, 5));

		inColumnCombo = new JComboBox<>(allColumns.toArray(new String[0]));
		interpolatedColumnField = new JTextField();
		firstDerivativeField = new JTextField();
		secondDerivativeField = new JTextField();

		add(new JLabel("In Column:"));
		add(inColumnCombo);
		add(new JLabel("Interpolated Column:"));
		add(interpolatedColumnField);
		add(new JLabel("1st Derivative:"));
		add(firstDerivativeField);
		add(new JLabel("2nd Derivative:"));
		add(secondDerivativeField);

		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		ok.addActionListener(e ->
		{
			confirmed = true;
			dispose();
		});
		cancel.addActionListener(e ->
		{
			confirmed = false;
			dispose();
		});

		add(ok);
		add(cancel);

		inColumnCombo.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				String inCol = (String) inColumnCombo.getSelectedItem();
				if (inCol != null)
				{
					String abridged = abridgeColumnName(inCol);
					interpolatedColumnField.setText(abridged + " Y Position");
					firstDerivativeField.setText(abridged + " Y Velocity");
					secondDerivativeField.setText(abridged + " Y Acceleration");
				}
			}
		});

		if (!allColumns.isEmpty())
		{
			inColumnCombo.setSelectedIndex(0);
		}
		pack();
		setLocationRelativeTo(owner);
	}

	public boolean showDialog()
	{
		setVisible(true);
		return confirmed;
	}

	public String getInColumn()
	{
		return (String) inColumnCombo.getSelectedItem();
	}

	public String getInterpolatedColumn()
	{
		return interpolatedColumnField.getText().trim();
	}

	public String getFirstDerivativeColumn()
	{
		return firstDerivativeField.getText().trim();
	}

	public String getSecondDerivativeColumn()
	{
		return secondDerivativeField.getText().trim();
	}

	private String abridgeColumnName(String original)
	{
		int idx = original.lastIndexOf(' ');
		if (idx > 0)
		{
			return original.substring(0, idx);
		}
		return original;
	}
}
