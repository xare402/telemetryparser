package com.telemetryparser.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutlierTracker
{
	private record TimeValue(double time, double value) { }
	private final Map<Parameter, TimeValue> lastValues = new HashMap<>();
	private final Map<Parameter, Deque<TimeValue>> initialValuesMap = new HashMap<>();
	public OutlierTracker()
	{
		for(Parameter parameter : Parameter.values())
		{
			if(parameter.include)
			{
				initialValuesMap.put(parameter, new ArrayDeque<>());
				lastValues.put(parameter, null);
			}
		}
	}
	public boolean detectAndNullifyOutliers(Map<Parameter, Double> parameterMap, Double time)
	{
		boolean anyOutlier = false;

		for (Map.Entry<Parameter, Double> entry : parameterMap.entrySet())
		{
			if(entry.getKey().type == ParameterType.ENGINES || entry.getKey().type == ParameterType.ENGINES_VARIANT)
			{
				continue;
			}
			Parameter p = entry.getKey();
			Double nextValue = entry.getValue();
			if (nextValue == null)
			{
				continue;
			}

			TimeValue lastTimeValue = lastValues.get(p);

			if (lastTimeValue == null)
			{
				Deque<TimeValue> queue = initialValuesMap.get(p);
				queue.add(new TimeValue(time, nextValue));

				if (queue.size() < 10)
				{
					parameterMap.put(p, null);
				}
				else if (queue.size() == 10)
				{
					double average = computeAverage(queue);
					double medianTime = computeMedianTime(queue);

					TimeValue initTv = new TimeValue(medianTime, average);
					lastValues.put(p, initTv);

					queue.clear();

					anyOutlier = compareDifference(parameterMap, time, anyOutlier, p, nextValue, initTv);
				}
			}
			else
			{
				anyOutlier = compareDifference(parameterMap, time, anyOutlier, p, nextValue, lastTimeValue);
			}
		}
		return anyOutlier;
	}

	private boolean compareDifference(Map<Parameter, Double> parameterMap, Double time, boolean anyOutlier, Parameter p, Double nextValue, TimeValue initTv)
	{
		if(p.type == ParameterType.ENGINES)
		{
			return anyOutlier;
		}
		double diff = Math.abs(nextValue - initTv.value);
		double timeDiff = time - initTv.time;

		if ((diff / timeDiff > p.type.acceptableChange && diff > 3) || timeDiff > 45)
		{
			parameterMap.put(p, null);
			anyOutlier = true;
		}
		else
		{
			lastValues.put(p, new TimeValue(time, nextValue));
		}
		return anyOutlier;
	}

	private double computeAverage(Deque<TimeValue> queue)
	{
		double sum = 0.0;
		for (TimeValue tv : queue)
		{
			sum += tv.value;
		}
		return sum / queue.size();
	}

	private double computeMedianTime(Deque<TimeValue> queue)
	{
		List<TimeValue> sorted = new ArrayList<>(queue);
		sorted.sort(Comparator.comparingDouble(a -> a.time));
		TimeValue tv5 = sorted.get(4);
		TimeValue tv6 = sorted.get(5);
		return (tv5.time + tv6.time) / 2.0;
	}

}
