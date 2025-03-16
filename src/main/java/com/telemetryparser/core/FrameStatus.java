package com.telemetryparser.core;

public enum FrameStatus
{
	RECENT_JUMP("Player time changed within the last second, so considered invalid."),
	TIME_PARSE_FAILURE("Unable to parse time without exception."),
	SUCCESS("Frame was processed successfully."),
	SUCCESS_BUT_OUTLIER("Frame was processed but an outlier was found and flagged"),
	SUCCESS_BUT_FLAGGED("Frame was processed but branch manager refused to accept the frame"),
	SUCCESS_BUT_FLAGGED_AND_OUTLIER("Frame was processed but branch manager refused to accept frame and an outlier was found");

	public final String description;
	FrameStatus(String description)
	{
		this.description = description;
	}
}
