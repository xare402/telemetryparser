package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.util.MalleableData;

public class ComputeECEFColumnsStep implements TransformationStep
{
	private final double initialLatitude;
	private final double initialLongitude;
	private final double initialAltitude;
	private final double initialAzimuth;
	private final String timeColumn;
	private final String altitudeChangeColumn;
	private final String speedMagnitudeColumn;
	private final String outputECEFXColumn;
	private final String outputECEFYColumn;
	private final String outputECEFZColumn;
	private final String outputDeltaECEFXColumn;
	private final String outputDeltaECEFYColumn;
	private final String outputDeltaECEFZColumn;

	public ComputeECEFColumnsStep(double initialLatitude,
								  double initialLongitude,
								  double initialAltitude,
								  double initialAzimuth,
								  String timeColumn,
								  String altitudeChangeColumn,
								  String speedMagnitudeColumn,
								  String outputECEFXColumn,
								  String outputECEFYColumn,
								  String outputECEFZColumn,
								  String outputDeltaECEFXColumn,
								  String outputDeltaECEFYColumn,
								  String outputDeltaECEFZColumn)
	{
		this.initialLatitude = initialLatitude;
		this.initialLongitude = initialLongitude;
		this.initialAltitude = initialAltitude;
		this.initialAzimuth = initialAzimuth;
		this.timeColumn = timeColumn;
		this.altitudeChangeColumn = altitudeChangeColumn;
		this.speedMagnitudeColumn = speedMagnitudeColumn;
		this.outputECEFXColumn = outputECEFXColumn;
		this.outputECEFYColumn = outputECEFYColumn;
		this.outputECEFZColumn = outputECEFZColumn;
		this.outputDeltaECEFXColumn = outputDeltaECEFXColumn;
		this.outputDeltaECEFYColumn = outputDeltaECEFYColumn;
		this.outputDeltaECEFZColumn = outputDeltaECEFZColumn;
	}

	@Override
	public String getName()
	{
		return "Compute ECEF Columns";
	}

	@Override
	public void apply(MalleableData data)
	{
		data.computeECEFColumns(
			initialLatitude, initialLongitude, initialAltitude, initialAzimuth,
			timeColumn, altitudeChangeColumn, speedMagnitudeColumn,
			outputECEFXColumn, outputECEFYColumn, outputECEFZColumn,
			outputDeltaECEFXColumn, outputDeltaECEFYColumn, outputDeltaECEFZColumn
		);
	}
}
