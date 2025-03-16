package com.telemetryparser.util;

import java.util.Map;

public record EngineData(ROIRatios ratios, Map<Integer, EngineLocation> engines)
{

}
