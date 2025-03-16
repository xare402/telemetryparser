package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.uicomponents.RestrictListResult;
import com.telemetryparser.datatransformation.util.MalleableData;

public class RestrictListsStep implements TransformationStep
{
	private final RestrictListResult result;

	public RestrictListsStep(RestrictListResult result)
	{
		this.result = result;
	}

	@Override
	public String getName()
	{
		return "Restrict Lists (" + result.value() + ")";
	}

	@Override
	public void apply(MalleableData data)
	{
		data.restrictLists(result.toRestrict(), result.value());
	}
}
