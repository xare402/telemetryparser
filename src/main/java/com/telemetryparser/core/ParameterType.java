package com.telemetryparser.core;

public enum ParameterType
{
	TIME("Time", 0),
	FUEL("Fuel", 8),
	SPEED("Speed", 350),
	ALTITUDE("Altitude", 5),
	ENGINES("Engines", 10000),
	ENGINES_VARIANT("Engines Variant", 10000),
	PITCH("Orientation", 50);

	public final int acceptableChange;
	public final String name;

	ParameterType(String name, int acceptableChange)
	{
		this.name = name;
		this.acceptableChange = acceptableChange;
	}
}
