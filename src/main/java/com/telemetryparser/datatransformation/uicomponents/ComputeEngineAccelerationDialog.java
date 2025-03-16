package com.telemetryparser.datatransformation.uicomponents;

import com.telemetryparser.datatransformation.steps.ComputePerEngineAcceleration;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ComputeEngineAccelerationDialog extends JDialog
{
	private final JComboBox<String> timeColumnComboBox;
	private final JComboBox<String> accelerationColumnComboBox;
	private final JComboBox<String> engineColumnComboBox;
	private final JTextField outputColumn;
	private boolean confirmed;

	public ComputeEngineAccelerationDialog(Window owner, List<String> allColumns)
	{
		super(owner, "Compute Engine Acceleration Columns", ModalityType.APPLICATION_MODAL);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		timeColumnComboBox = new JComboBox<>(allColumns.toArray(new String[0]));
		accelerationColumnComboBox = new JComboBox<>(allColumns.toArray(new String[0]));
		engineColumnComboBox = new JComboBox<>(allColumns.toArray(new String[0]));
		outputColumn = new JTextField("Per Engine Acceleration Stage ");

		addField(timeColumnComboBox, "Time: ", gbc);
		addField(accelerationColumnComboBox, "Acceleration: ", gbc);
		addField(engineColumnComboBox, "Engines: ", gbc);
		addField(outputColumn, "Output Column: ", gbc);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		buttonPanel.add(ok);
		buttonPanel.add(cancel);

		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy++;
		add(buttonPanel, gbc);

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

		pack();
		setLocationRelativeTo(owner);
	}

	private void addField(JComponent field, String label, GridBagConstraints gbc)
	{
		gbc.gridwidth = 1;
		add(new JLabel(label), gbc);
		gbc.gridx++;
		add(field, gbc);
		gbc.gridx = 0;
		gbc.gridy++;
	}

	public boolean showDialog()
	{
		setVisible(true);
		return confirmed;
	}

	public String getTimeColumn()
	{
		return (String) timeColumnComboBox.getSelectedItem();
	}

	public String getAccelerationColumn()
	{
		return (String) accelerationColumnComboBox.getSelectedItem();
	}

	public String getEngineColumn()
	{
		return (String) engineColumnComboBox.getSelectedItem();
	}

	public String getOutputColumn()
	{
		return outputColumn.getText();
	}
}
