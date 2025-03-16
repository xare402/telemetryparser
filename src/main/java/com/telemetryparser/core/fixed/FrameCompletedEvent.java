package com.telemetryparser.core.fixed;


import com.telemetryparser.core.FrameStatus;
import com.telemetryparser.core.Parameter;
import java.util.Map;

public record FrameCompletedEvent(long elapsedMs, boolean flagged, long flaggedFrameCount, long totalFrameCount, Double time, FrameAnalysisResult frameAnalysisResult, Map<Parameter, Double> data, FrameStatus frameStatus)
{
	public FrameCompletedEvent(FrameAnalysisResult frameAnalysisResult, FrameStatus frameStatus)
	{
		this(0, true, 0, 0, 0d, frameAnalysisResult, null, frameStatus);
	}
}
