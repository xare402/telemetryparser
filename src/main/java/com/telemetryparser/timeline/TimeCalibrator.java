package com.telemetryparser.timeline;

import com.telemetryparser.core.TelemetrySnapshot;

class TimeCalibrator
{
	private static final int REQUIRED_FRAMES = 5;
	private static final long ALLOWED_DEVIATION_MS = 2000;
	private int collectedFrames = 0;
	private long firstSystemTime = 0;
	private long firstPlayerTime = 0;
	private double initialSeconds = 0;
	private double lastRawSeconds = 0;
	private double fractionalOffset = 0;
	private long lastSystemTime = 0;
	private boolean fractionCalibrated = false;
	private double lastKnownTime = 0;

	public boolean collectInitialFrames(TelemetrySnapshot candidate)
	{
		if (collectedFrames == 0)
		{
			initialSeconds = candidate.getTime();
			lastRawSeconds = candidate.getTime();
			firstSystemTime = candidate.getSystemTime();
			firstPlayerTime = candidate.getPlayerTime();
		}

		if (!roughlyInRange(candidate.getTime(), lastRawSeconds))
		{
			return false;
		}
		lastRawSeconds = candidate.getTime();
		collectedFrames++;
		if (collectedFrames == REQUIRED_FRAMES)
		{
			lastSystemTime = candidate.getSystemTime();
			lastKnownTime = candidate.getTime();
		}
		return true;
	}

	public boolean isReady()
	{
		return collectedFrames >= REQUIRED_FRAMES;
	}

	public boolean validateTime(TelemetrySnapshot candidate)
	{
		double expected = lastKnownTime + (candidate.getSystemTime() - lastSystemTime) / 1000.0;
		double observed = candidate.getTime();
		double delta = Math.abs(observed - expected);
		if (delta > 2.0)
		{
			return false;
		}
		lastSystemTime = candidate.getSystemTime();
		lastKnownTime = observed;
		return true;
	}

	public void updateFractionalSecond(TelemetrySnapshot current)
	{
		double rawCurrent = current.getTime();
		if (!fractionCalibrated)
		{
			if (Math.floor(rawCurrent) != Math.floor(lastRawSeconds))
			{
				fractionalOffset = calculateFractionalOffset(rawCurrent, lastSystemTime);
				fractionCalibrated = true;
				System.out.println("Fractional second calibration done");
			}
		}
		lastRawSeconds = rawCurrent;
	}

	public double calculateCalibratedTime(long systemTime)
	{
		double baseOffset = (systemTime - firstSystemTime) / 1000.0;
		int zeroOffset = (initialSeconds <= 0) ? -1 : 0;
		return initialSeconds + baseOffset + fractionalOffset + zeroOffset;
	}

	public boolean isTimeInRange(long systemTime, long playerTime)
	{
		long elapsedFromFirstSystemTime = systemTime - firstSystemTime;
		long approximatePlayerDelta = playerTime - firstPlayerTime;
		long diff = Math.abs(elapsedFromFirstSystemTime - approximatePlayerDelta);
		return diff <= ALLOWED_DEVIATION_MS;
	}

	private boolean roughlyInRange(double current, double last)
	{
		return !(Math.abs(current - last) > 2.0);
	}

	private double calculateFractionalOffset(double rawSeconds, long lastSystemTime)
	{
		double offsetSeconds = (lastSystemTime - firstSystemTime) / 1000.0;
		double integerPart = Math.floor(rawSeconds);
		return integerPart - (initialSeconds + offsetSeconds);
	}
}
