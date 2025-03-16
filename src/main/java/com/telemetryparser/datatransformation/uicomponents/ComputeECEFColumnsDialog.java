package com.telemetryparser.datatransformation.uicomponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class ComputeECEFColumnsDialog extends JDialog
{
	private final JTextField latField;
	private final JTextField lonField;
	private final JTextField altField;
	private final JTextField azField;
	private final JComboBox<String> timeColumnComboBox;
	private final JComboBox<String> altChangeColCombo;
	private final JComboBox<String> speedColCombo;
	private final JTextField ecefXField;
	private final JTextField ecefYField;
	private final JTextField ecefZField;
	private final JTextField deltaXField;
	private final JTextField deltaYField;
	private final JTextField deltaZField;
	private boolean confirmed;

	public ComputeECEFColumnsDialog(Window owner, List<String> allColumns)
	{
		super(owner, "Compute ECEF Columns", ModalityType.APPLICATION_MODAL);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JComboBox<PredefinedLocation> locationCombo = new JComboBox<>(PredefinedLocation.values());
		latField = new JTextField("0.0");
		lonField = new JTextField("0.0");
		altField = new JTextField("0.0");
		azField = new JTextField("0.0");

		timeColumnComboBox = new JComboBox<>(allColumns.toArray(new String[0]));
		altChangeColCombo = new JComboBox<>(allColumns.toArray(new String[0]));
		speedColCombo = new JComboBox<>(allColumns.toArray(new String[0]));

		ecefXField = new JTextField("ecefX");
		ecefYField = new JTextField("ecefY");
		ecefZField = new JTextField("ecefZ");
		deltaXField = new JTextField("deltaX");
		deltaYField = new JTextField("deltaY");
		deltaZField = new JTextField("deltaZ");

		addField(locationCombo, "Location Preset:", gbc);
		addField(latField, "Latitude:", gbc);
		addField(lonField, "Longitude:", gbc);
		addField(altField, "Altitude:", gbc);
		addField(azField, "Azimuth:", gbc);
		addField(timeColumnComboBox, "Time Change Col: ", gbc);
		addField(altChangeColCombo, "Altitude Change Col:", gbc);
		addField(speedColCombo, "Speed Magnitude Col:", gbc);
		addField(ecefXField, "ECEF X Col:", gbc);
		addField(ecefYField, "ECEF Y Col:", gbc);
		addField(ecefZField, "ECEF Z Col:", gbc);
		addField(deltaXField, "Delta ECEF X:", gbc);
		addField(deltaYField, "Delta ECEF Y:", gbc);
		addField(deltaZField, "Delta ECEF Z:", gbc);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		buttonPanel.add(ok);
		buttonPanel.add(cancel);

		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy++;
		add(buttonPanel, gbc);

		locationCombo.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				PredefinedLocation loc = (PredefinedLocation) e.getItem();
				if (loc == PredefinedLocation.CUSTOM)
				{
					latField.setEditable(true);
					lonField.setEditable(true);
					altField.setEditable(true);
				}
				else
				{
					latField.setText(String.valueOf(loc.lat));
					lonField.setText(String.valueOf(loc.lon));
					altField.setText(String.valueOf(loc.alt));
					latField.setEditable(false);
					lonField.setEditable(false);
					altField.setEditable(false);
				}
			}
		});

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

	public boolean showDialog()
	{
		setVisible(true);
		return confirmed;
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

	public double getInitialLatitude()
	{
		return Double.parseDouble(latField.getText().trim());
	}

	public double getInitialLongitude()
	{
		return Double.parseDouble(lonField.getText().trim());
	}

	public double getInitialAltitude()
	{
		return Double.parseDouble(altField.getText().trim());
	}

	public double getInitialAzimuth()
	{
		return Double.parseDouble(azField.getText().trim());
	}

	public String getTimeColumn()
	{
		return (String) timeColumnComboBox.getSelectedItem();
	}

	public String getAltitudeChangeColumn()
	{
		return (String) altChangeColCombo.getSelectedItem();
	}

	public String getSpeedMagnitudeColumn()
	{
		return (String) speedColCombo.getSelectedItem();
	}

	public String getEcefXColumn()
	{
		return ecefXField.getText().trim();
	}

	public String getEcefYColumn()
	{
		return ecefYField.getText().trim();
	}

	public String getEcefZColumn()
	{
		return ecefZField.getText().trim();
	}

	public String getDeltaXColumn()
	{
		return deltaXField.getText().trim();
	}

	public String getDeltaYColumn()
	{
		return deltaYField.getText().trim();
	}

	public String getDeltaZColumn()
	{
		return deltaZField.getText().trim();
	}

	public enum PredefinedLocation
	{
		ORBITAL_PAD_A("Orbital Pad A", 25.9961684, -97.1536817, 0),
		LC_39("LC-39", 28.6050359132, -80.6026042562, 0),
		SLC4_EAST("SLC4-East", 34.6330, -120.6153, 0),
		CUSTOM("Custom", 0, 0, 0);

		public final String label;
		public final double lat;
		public final double lon;
		public final double alt;

		PredefinedLocation(String label, double lat, double lon, double alt)
		{
			this.label = label;
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
