package com.telemetryparser.core.fixed;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.TelemetrySnapshot;
import com.telemetryparser.timeline.Branch;
import com.telemetryparser.timeline.BranchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FrameBranchManager
{
	private final List<Branch> branches = new ArrayList<>();
	private long branchCounter = 0;

	public BranchResult handleFrame(TelemetrySnapshot candidate)
	{
		List<Branch> viableBranches = findViableBranches(candidate.getSystemTime(), candidate.getPlayerTime());
		if(viableBranches.isEmpty())
		{
			return createNewBranch().addSnapshot(candidate);
		}
		boolean accepted = false;
		boolean anyOutlier = false;
		Map<Parameter, Double> lastParams = candidate.parameterMap;
		Double lastTime = candidate.timeOccured;
		for (Branch branch : viableBranches)
		{
			BranchResult br = branch.addSnapshot(candidate);
			if (br.accepted())
			{
				accepted = true;
				lastParams = br.finalParameterMap();
				lastTime = br.time();
			}
			if (br.outlierFound())
			{
				anyOutlier = true;
			}
		}
		return new BranchResult(accepted, anyOutlier, lastParams, lastTime);
	}

	private Branch createNewBranch()
	{
		branchCounter++;
		Branch branch = new Branch(branchCounter);
		branches.add(branch);
		System.out.println("Created new branch " + branchCounter);
		return branch;
	}

	private List<Branch> findViableBranches(long systemTime, long playerTime)
	{
		List<Branch> viable = new ArrayList<>();
		for (Branch branch : branches)
		{
			if (branch.canAccept(systemTime, playerTime))
			{
				viable.add(branch);
			}
		}
		return viable;
	}

	public void clearData()
	{
		branchCounter = 0;
		branches.clear();
	}

	public List<TelemetrySnapshot> getMergedBranches()
	{
		List<TelemetrySnapshot> merged = new ArrayList<>();
		List<SecondRangeData> segmented = new ArrayList<>();
		for (Branch branch : branches)
		{
			for (TelemetrySnapshot snap : branch.branchSnapshots())
			{
				segmented.add(new SecondRangeData(branch.branchId(), snap, (int) Math.floor(snap.getTime())));
			}
		}
		Map<Integer, List<SecondRangeData>> grouped = segmented.stream().collect(Collectors.groupingBy(SecondRangeData::secondBucket));
		for (Map.Entry<Integer, List<SecondRangeData>> entry : grouped.entrySet())
		{
			List<SecondRangeData> dataInSecond = entry.getValue();
			Map<Long, Long> countsByBranch = dataInSecond.stream().collect(Collectors.groupingBy(SecondRangeData::branchId, Collectors.counting()));
			long bestBranchId = Collections.max(countsByBranch.entrySet(), Comparator.comparingLong(Map.Entry::getValue)).getKey();
			for (SecondRangeData dr : dataInSecond)
			{
				if (dr.branchId() == bestBranchId)
				{
					merged.add(dr.telemetrySnapshot());
				}
			}
		}
		merged.sort(Comparator.comparingDouble(TelemetrySnapshot::getTime));
		return merged;
	}

	private record SecondRangeData(long branchId, TelemetrySnapshot telemetrySnapshot, int secondBucket)
	{
	}
}
