package com.telemetryparser.ui.components.slider;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

public class NamedScaledSlider extends JPanel
{
	private final JSlider slider;
	private final JLabel valueLabel;
	private final int decimalPlaces;
	private final int defaultValue;

	public NamedScaledSlider(String name, int decimalPlaces, int min, int max, int value)
	{
		this.decimalPlaces = decimalPlaces;
		this.defaultValue = value;

		double scaledMin = min / Math.pow(10, decimalPlaces);
		double scaledMax = max / Math.pow(10, decimalPlaces);
		String formatSpecifier = "%." + decimalPlaces + "f";
		String formattedMin = String.format(formatSpecifier, scaledMin);
		String formattedMax = String.format(formatSpecifier, scaledMax);

		String borderTitle = name + " (" + formattedMin + " - " + formattedMax + ")";
		setBorder(BorderFactory.createTitledBorder(borderTitle));

		slider = new JSlider(min, max, value);

		valueLabel = new JLabel(String.format(formatSpecifier, getScaledValue()));

		slider.addChangeListener(e -> valueLabel.setText(String.format(formatSpecifier, getScaledValue())));

		setLayout(new BorderLayout());
		add(slider, BorderLayout.CENTER);
		add(valueLabel, BorderLayout.EAST);
	}

	public double getScaledValue()
	{
		return slider.getValue() / Math.pow(10, decimalPlaces);
	}

	public void resetToDefaultValue()
	{
		slider.setValue(defaultValue);
	}

	public void addChangeListener(ChangeListener changeListener)
	{
		slider.addChangeListener(changeListener);
	}

	public void removeChangeListener(ChangeListener changeListener)
	{
		slider.removeChangeListener(changeListener);
	}

	public void setValue(Double sliderValue)
	{
		slider.setValue((int) (sliderValue * (Math.pow(10, decimalPlaces))));
	}

	public void setResetButton(FlatSVGIcon resetIcon)
	{
		JButton resetButton = new JButton(resetIcon);
		resetButton.setBorder(BorderFactory.createEmptyBorder());
		resetButton.addActionListener(al -> resetToDefaultValue());
		add(resetButton, BorderLayout.WEST);
	}
}
