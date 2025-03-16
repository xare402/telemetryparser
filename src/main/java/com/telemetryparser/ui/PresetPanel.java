package com.telemetryparser.ui;

import com.telemetryparser.core.Parameter;
import static com.telemetryparser.core.Parameter.TIME;
import static com.telemetryparser.core.Parameter.values;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.settings.Preset;
import com.telemetryparser.settings.Settings;
import com.telemetryparser.util.TesseractManager;
import com.telemetryparser.util.UnitTranslation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PresetPanel extends JPanel
{
	private final JComboBox<String> regionSelector;
	private final JCheckBox overlayCheckbox;
	private final JCheckBox usesComma;
	private final JComboBox<UnitTranslation> speedTranslation;
	private final JComboBox<UnitTranslation> altitudeTranslation;
	private final JTextField timePrefixField;
	private final List<JComponent> presetDependentEnabledComponents = new ArrayList<>();
	private final MainWindow mainWindow;
	private final JPanel saveOrDeletePanel = new JPanel(new BorderLayout());
	private final JButton saveAsButton = new JButton("Save Preset");
	private final JButton deleteButton = new JButton("Delete Preset");
	private Preset activePreset = new Preset();
	JCheckBox telemetryCheckbox = new JCheckBox("Additional Telemetry?", false);
	public PresetPanel(MainWindow mainWindow)
	{
		this.mainWindow = mainWindow;
		setBorder(BorderFactory.createTitledBorder("Choose Preset"));
		setLayout(new GridLayout(0, 1));
		setPreferredSize(new Dimension(0, 360));

		saveAsButton.addActionListener(al -> saveAs());
		saveAsButton.setMargin(new Insets(0, 0, 0, 0));
		deleteButton.addActionListener(al -> deleteActivePreset());
		deleteButton.setMargin(new Insets(0, 0, 0, 0));
		saveOrDeletePanel.add(saveAsButton, BorderLayout.CENTER);

		JButton clearPresetButton = new JButton("Clear Preset");
		clearPresetButton.addActionListener(al -> mainWindow.clearPreset());
		clearPresetButton.setMargin(new Insets(0, 0, 0, 0));


		String[] options = Settings.collectPresets();
		regionSelector = new JComboBox<>(options);
		regionSelector.addActionListener(al -> presetChanged());
		overlayCheckbox = new JCheckBox("Preview?");
		overlayCheckbox.addActionListener(al -> mainWindow.toggleOverlay(overlayCheckbox.isSelected()));

		usesComma = new JCheckBox("Uses comma?");
		usesComma.setSelected(false);
		usesComma.addActionListener(al -> changeUsesComma());

		speedTranslation = new JComboBox<>(UnitTranslation.values());
		altitudeTranslation = new JComboBox<>(UnitTranslation.values());

		speedTranslation.addActionListener(al -> changeSpeedTranslationUnit());
		altitudeTranslation.addActionListener(al -> changeAltitudeTranslationUnit());

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(0, 2, 5, 2));
		wrapper.add(regionSelector, BorderLayout.CENTER);
		wrapper.add(overlayCheckbox, BorderLayout.EAST);

		telemetryCheckbox.addActionListener(al ->
		{
			mainWindow.setAdditionalTelemetryEnabled(telemetryCheckbox.isSelected());
		});
		add(telemetryCheckbox);

		add(wrapper);

		add(getSplitPanel(saveOrDeletePanel, clearPresetButton));

		JLabel speedLabel = new JLabel("Speed Units");
		JLabel altitudeLabel = new JLabel("Altitude Units");

		presetDependentEnabledComponents.add(clearPresetButton);
		presetDependentEnabledComponents.add(usesComma);
		presetDependentEnabledComponents.add(speedTranslation);
		presetDependentEnabledComponents.add(altitudeTranslation);
		presetDependentEnabledComponents.add(speedLabel);
		presetDependentEnabledComponents.add(altitudeLabel);

		add(getSplitPanel(speedLabel, speedTranslation));
		add(getSplitPanel(altitudeLabel, altitudeTranslation));

		JPanel timePrefixPanel = new JPanel(new BorderLayout());
		JLabel timePrefixLabel = new JLabel("Time Prefix:");
		presetDependentEnabledComponents.add(timePrefixLabel);
		timePrefixField = getTimePrefixField();
		presetDependentEnabledComponents.add(timePrefixField);
		timePrefixPanel.add(timePrefixLabel, BorderLayout.CENTER);
		timePrefixPanel.add(timePrefixField, BorderLayout.EAST);

		add(getSplitPanel(timePrefixPanel, usesComma));

		JLabel setLabel = new JLabel("Set Regions", SwingConstants.CENTER);
		presetDependentEnabledComponents.add(setLabel);
		add(getSplitPanel(setLabel, getRegionButton(TIME)));

		for (int i = 1; i < values().length; i += 2)
		{
			if (values()[i].type != ParameterType.ENGINES_VARIANT && values()[i + 1].type != ParameterType.ENGINES_VARIANT)
			{
				add(getSplitPanel(getRegionButton(Parameter.values()[i]), getRegionButton(Parameter.values()[i + 1])));
			}
		}
		regionSelector.setSelectedItem(Settings.getProperty("setting", "lastSelectedPreset", "Custom"));
	}

	private void deleteActivePreset()
	{
		Settings.deletePreset(getActivePreset());
		regionSelector.removeItem(getActivePreset());
	}

	private void changeUsesComma()
	{
		activePreset.setUsesComma(usesComma.isSelected());
		Settings.setProperty(getActivePreset(), "usesComma", String.valueOf(usesComma.isSelected()));
	}

	private void changeAltitudeTranslationUnit()
	{
		activePreset.setAltitudeTranslation((UnitTranslation) altitudeTranslation.getSelectedItem());
		Settings.setProperty(getActivePreset(), "altitude", String.valueOf(altitudeTranslation.getSelectedItem()));
	}

	private void changeSpeedTranslationUnit()
	{
		activePreset.setSpeedTranslation((UnitTranslation) speedTranslation.getSelectedItem());
		Settings.setProperty(getActivePreset(), "speed", String.valueOf(speedTranslation.getSelectedItem()));
	}

	private JTextField getTimePrefixField()
	{
		JTextField timePrefixField = new JTextField("T", 1);
		timePrefixField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				handleTextChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				handleTextChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				handleTextChanged();
			}

			private void handleTextChanged()
			{
				TesseractManager.changeTimePrefixAsync(timePrefixField.getText());
				Settings.setProperty(getActivePreset(), "timePrefix", timePrefixField.getText());
				activePreset.setTimePrefix(timePrefixField.getText());
			}
		});
		return timePrefixField;
	}

	private JButton getRegionButton(Parameter parameter)
	{
		String abbreviatedName = parameter.name.replace("Stage ", "S");
		JButton button = new JButton(abbreviatedName);
		presetDependentEnabledComponents.add(button);
		button.addActionListener(al -> mainWindow.setROI(parameter));
		return button;
	}

	public boolean getOverlayState()
	{
		return overlayCheckbox.isSelected() || telemetryCheckbox.isSelected();
	}

	private JPanel getSplitPanel(JComponent first, JComponent second)
	{
		JPanel split = new JPanel(new GridLayout(1, 2));
		split.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		split.add(first);
		split.add(second);
		return split;
	}

	public String getActivePreset()
	{
		return (String)regionSelector.getSelectedItem();
	}

	public Preset getPreset()
	{
		return activePreset;
	}

	private void saveAs()
	{
		JDialog dialog = new JDialog((Frame) null, "Save As", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLayout(new BorderLayout());

		JLabel instructionLabel = new JLabel("Enter a new preset name:");
		instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		dialog.add(instructionLabel, BorderLayout.NORTH);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

		JTextField nameField = new JTextField(20);
		Color defaultBg = nameField.getBackground();
		centerPanel.add(nameField, BorderLayout.CENTER);

		JLabel errorLabel = new JLabel(" ");
		errorLabel.setForeground(Color.RED);
		errorLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		centerPanel.add(errorLabel, BorderLayout.SOUTH);

		dialog.add(centerPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		nameField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				checkName();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				checkName();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				checkName();
			}

			private void checkName()
			{
				String inputName = nameField.getText().trim();
				boolean nameExists = false;

				for (int i = 0; i < regionSelector.getItemCount(); i++)
				{
					if (regionSelector.getItemAt(i).equalsIgnoreCase(inputName))
					{
						nameExists = true;
						break;
					}
				}

				if (nameExists)
				{
					errorLabel.setText("Name already exists!");
					nameField.setBackground(new Color(255, 50, 50, 100));
					okButton.setEnabled(false);
				}
				else
				{
					errorLabel.setText(" ");
					nameField.setBackground(defaultBg);
					okButton.setEnabled(!inputName.isEmpty());
				}
			}
		});

		okButton.addActionListener(e ->
		{
			String newName = nameField.getText().trim();
			if (!newName.isEmpty())
			{
				regionSelector.addItem(newName);
				regionSelector.setSelectedItem(newName);
				Settings.copyPreset("Custom", newName);
				presetChanged();
				mainWindow.loadROIs(newName);
				dialog.dispose();
			}
		});

		cancelButton.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void presetChanged()
	{
		activePreset = Settings.loadPreset(getActivePreset());
		Settings.setProperty("setting", "lastSelectedPreset", getActivePreset());
		applyPresetToUI(activePreset);
		mainWindow.switchROIs();
		if (Objects.equals(getActivePreset(), "Custom"))
		{
			saveOrDeletePanel.removeAll();
			saveOrDeletePanel.add(saveAsButton, BorderLayout.CENTER);
			saveOrDeletePanel.revalidate();
			saveOrDeletePanel.repaint();
			for (JComponent component : presetDependentEnabledComponents)
			{
				component.setEnabled(true);
			}
		}
		else
		{
			saveOrDeletePanel.removeAll();
			saveOrDeletePanel.add(deleteButton, BorderLayout.CENTER);
			saveOrDeletePanel.revalidate();
			saveOrDeletePanel.repaint();
			for (JComponent component : presetDependentEnabledComponents)
			{
				component.setEnabled(false);
			}
		}
	}

	private void applyPresetToUI(Preset preset)
	{
		speedTranslation.setSelectedItem(preset.speedTranslation());
		altitudeTranslation.setSelectedItem(preset.altitudeTranslation());
		timePrefixField.setText(preset.timePrefix());
		usesComma.setSelected(preset.usesComma());
	}
}
