package com.telemetryparser.timeline;

import com.telemetryparser.core.TelemetrySnapshot;
import com.telemetryparser.core.OutlierTracker;
import java.util.ArrayList;
import java.util.List;

public class Branch
{
	private final long id;
	private final List<TelemetrySnapshot> snapshots = new ArrayList<>();
	private final TimeCalibrator calibrator = new TimeCalibrator();
	private final OutlierTracker tracker = new OutlierTracker();
	private boolean calibrationComplete = false;

	public Branch(long id)
	{
		this.id = id;
	}

	public BranchResult addSnapshot(TelemetrySnapshot candidate)
	{
		if (!calibrationComplete)
		{
			boolean consistent = calibrator.collectInitialFrames(candidate);
			if (!consistent)
			{
				return new BranchResult(false, false, candidate.parameterMap, null);
			}
			if (calibrator.isReady())
			{
				calibrationComplete = true;
				System.out.println("Player time calibration complete for branch " + id);
			}
		}
		else
		{
			if (!calibrator.validateTime(candidate))
			{
				return new BranchResult(false, false, candidate.parameterMap, null);
			}
			calibrator.updateFractionalSecond(candidate);
		}
		candidate.timeOccured = calibrator.calculateCalibratedTime(candidate.getSystemTime());
		snapshots.add(candidate);

		boolean anyParamOutlier = tracker.detectAndNullifyOutliers(candidate.parameterMap, candidate.timeOccured);

		return new BranchResult(true, anyParamOutlier, candidate.parameterMap, candidate.timeOccured);
	}


	public boolean canAccept(long systemTime, long playerTime)
	{
		if (!calibrationComplete)
		{
			return true;
		}
		return calibrator.isTimeInRange(systemTime, playerTime);
	}

	public List<TelemetrySnapshot> branchSnapshots()
	{
		return snapshots;
	}

	public long branchId()
	{
		return id;
	}
}
