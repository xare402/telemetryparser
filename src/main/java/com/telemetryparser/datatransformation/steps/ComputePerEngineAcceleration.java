package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.util.MalleableData;

public class ComputePerEngineAcceleration implements TransformationStep
{
	private final String timeColumn;
	private final String accelerationColumn;
	private final String engineColumn;
	private final String outputPerEngineAccelerationColumn;

	public ComputePerEngineAcceleration(String timeColumn, String accelerationColumn, String engineColumn, String outputPerEngineAccelerationColumn)
	{
		this.timeColumn = timeColumn;
		this.accelerationColumn = accelerationColumn;
		this.engineColumn = engineColumn;
		this.outputPerEngineAccelerationColumn = outputPerEngineAccelerationColumn;
	}

	@Override
	public String getName()
	{
		return "Compute Per Engine Acceleration";
	}

	@Override
	public void apply(MalleableData data)
	{
		data.computePerEngineAcceleration(timeColumn, accelerationColumn, engineColumn, outputPerEngineAccelerationColumn);
	}
}
