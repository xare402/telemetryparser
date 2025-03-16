package com.telemetryparser.core;

public enum Parameter
{
	TIME("Time", ParameterType.TIME, 0, false),
	STAGE_1_SPEED("Stage 1 Speed", ParameterType.SPEED, 0),
	STAGE_2_SPEED("Stage 2 Speed", ParameterType.SPEED, 0),
	STAGE_1_ALTITUDE("Stage 1 Altitude", ParameterType.ALTITUDE, 1),
	STAGE_2_ALTITUDE("Stage 2 Altitude", ParameterType.ALTITUDE, 1),
	STAGE_1_LOX("Stage 1 LOX", ParameterType.FUEL, 1),
	STAGE_2_LOX("Stage 2 LOX", ParameterType.FUEL, 1),
	STAGE_1_CH4("Stage 1 CH4", ParameterType.FUEL, 1),
	STAGE_2_CH4("Stage 2 CH4", ParameterType.FUEL, 1),
	STAGE_1_ORIENTATION("Stage 1 Pitch", ParameterType.PITCH, 0),
	STAGE_2_ORIENTATION("Stage 2 Pitch", ParameterType.PITCH, 0),
	STAGE_1_ENGINES("Stage 1 Engines", ParameterType.ENGINES, 0),
	STAGE_2_ENGINES("Stage 2 Engines", ParameterType.ENGINES, 0),
	STAGE_1_CENTER_ENGINES("Stage 1 Center Engines", ParameterType.ENGINES_VARIANT, 0),
	STAGE_2_CENTER_ENGINES("Stage 2 Center Engines", ParameterType.ENGINES_VARIANT, 0),
	STAGE_1_MIDDLE_ENGINES("Stage 1 Middle Engines", ParameterType.ENGINES_VARIANT, 0),
	STAGE_1_OUTER_ENGINES("Stage 1 Outer Engines", ParameterType.ENGINES_VARIANT, 0),
	STAGE_2_OUTER_ENGINES("Stage 2 Outer Engines", ParameterType.ENGINES_VARIANT, 0);

	public final String name;
	final int precision;
	public final boolean include;

	public final ParameterType type;

	Parameter(String name, ParameterType type, int precision, boolean include)
	{
		this.name = name;
		this.precision = precision;
		this.include = include;
		this.type = type;
	}

	Parameter(String name, ParameterType type, int precision)
	{
		this(name, type, precision, true);
	}

	public static String getAsHeader()
	{
		StringBuilder builder = new StringBuilder();
		for (Parameter parameter : Parameter.values())
		{
			builder.append(parameter.name).append(",");
		}
		return builder.substring(0, builder.length() - 1);
	}
}
