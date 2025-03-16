package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.util.MalleableData;

public class RemoveDuplicatesStep implements TransformationStep
{
	@Override
	public String getName()
	{
		return "Remove Duplicates";
	}

	@Override
	public void apply(MalleableData data)
	{
		data.removeDuplicates();
	}
}
