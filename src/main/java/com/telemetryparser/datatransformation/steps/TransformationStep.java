package com.telemetryparser.datatransformation.steps;

import com.telemetryparser.datatransformation.util.MalleableData;

public interface TransformationStep
{
	String getName();

	void apply(MalleableData data);
}
