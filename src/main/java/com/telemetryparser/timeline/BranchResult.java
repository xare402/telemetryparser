package com.telemetryparser.timeline;

import com.telemetryparser.core.Parameter;
import java.util.Map;

public record BranchResult(boolean accepted, boolean outlierFound, Map<Parameter, Double> finalParameterMap, Double time)
{
}
