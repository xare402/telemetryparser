package com.telemetryparser.core.fixed;

import com.telemetryparser.util.EngineLocation;

public record EngineState(EngineLocation engineLocation, boolean active)
{
}
