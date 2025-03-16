package com.telemetryparser.ui.components.slider;

public enum SliderSetting
{
	RADIUS("Radius", 1, 0, 250, 68),
	AMOUNT("Amount", 1, 0, 100, 27),
	UNSHARP_THRESHOLD("Unsharp Threshold", 2, 0, 100, 30),
	BW_THRESHOLD("Black/White Threshold", 0, 0, 255, 200),
	SCALE("Scale", 1, 10, 40, 30),
	ORIENTATION_STRENGTH("Acceptable Correlation", 1, 0, 10, 8),
	ORIENTATION_SYMMETRY("Acceptable Symmetry", 1, 0, 10, 3);

	private final String displayName;
	private final int decimalPlaces;
	private final int min;
	private final int max;
	private final int defaultValue;

	SliderSetting(String displayName, int decimalPlaces, int min, int max, int defaultValue)
	{
		this.displayName = displayName;
		this.decimalPlaces = decimalPlaces;
		this.min = min;
		this.max = max;
		this.defaultValue = defaultValue;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public NamedScaledSlider createSlider()
	{
		return new NamedScaledSlider(displayName, decimalPlaces, min, max, defaultValue);
	}
}