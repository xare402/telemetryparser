package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.util.MalleableData;

public class InterpolateAndDerive2xStep implements TransformationStep
{
	private final String inColumn;
	private final String interpolatedColumn;
	private final String firstDerivativeColumn;
	private final String secondDerivativeColumn;

	public InterpolateAndDerive2xStep(String inColumn,
									  String interpolatedColumn,
									  String firstDerivativeColumn,
									  String secondDerivativeColumn)
	{
		this.inColumn = inColumn;
		this.interpolatedColumn = interpolatedColumn;
		this.firstDerivativeColumn = firstDerivativeColumn;
		this.secondDerivativeColumn = secondDerivativeColumn;
	}

	@Override
	public String getName()
	{
		return "Interpolate & Derive 2x";
	}

	@Override
	public void apply(MalleableData data)
	{
		data.interpolateAndDerive2x(inColumn, interpolatedColumn, firstDerivativeColumn, secondDerivativeColumn);
	}
}
