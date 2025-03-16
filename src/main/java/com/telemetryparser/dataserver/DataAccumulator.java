package com.telemetryparser.dataserver;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.core.TelemetrySnapshot;
import com.telemetryparser.util.math.CubicSplinePolynomial;
import com.telemetryparser.util.math.Polynomial;

import java.util.*;
import java.util.stream.Collectors;

public class DataAccumulator
{
	private final double acceptableTimeDelta;
	private final int windowCount;
	private final Set<ParameterType> ignoredTypes;

	private final Map<Parameter, Deque<DataPoint>> dataMap = new HashMap<>();

	private final Map<Parameter, List<DataPoint>> averagedPointsMap = new HashMap<>();

	private final List<DataAccumulatorListener> listeners = new ArrayList<>();

	private double lastTime = Double.NEGATIVE_INFINITY;
	private final Map<Parameter, Double> lastValues = new HashMap<>();

	public DataAccumulator(double acceptableTimeDelta, int windowCount, ParameterType... ignoredParameters)
	{
		this.acceptableTimeDelta = acceptableTimeDelta;
		this.windowCount = windowCount;
		this.ignoredTypes = (ignoredParameters == null)
			? Collections.emptySet()
			: Arrays.stream(ignoredParameters).collect(Collectors.toSet());
	}

	public void addListener(DataAccumulatorListener listener)
	{
		listeners.add(listener);
	}

	public void addSnapshot(TelemetrySnapshot snapshot)
	{
		if (lastTime != Double.NEGATIVE_INFINITY && snapshot.getTime() > lastTime + 60)
		{
			return;
		}
		lastTime = snapshot.getTime();
		Map<Parameter, Double> triggeredAverages = new HashMap<>();

		for (Map.Entry<Parameter, Double> entry : snapshot.parameterMap.entrySet())
		{
			Parameter param = entry.getKey();
			Double value = entry.getValue();

			if (ignoredTypes.contains(param.type) || value == null)
			{
				continue;
			}

			dataMap.computeIfAbsent(param, k -> new LinkedList<>());

			Deque<DataPoint> bucket = dataMap.get(param);
			if (bucket.peekLast() != null && bucket.peekLast().value == value)
			{
				continue;
			}
			bucket.addLast(new DataPoint(snapshot.getTime(), value));

			if (bucket.size() >= windowCount
				|| (snapshot.getTime() - Objects.requireNonNull(bucket.peekFirst()).time) >= acceptableTimeDelta)
			{
				double sum = 0.0;
				for (DataPoint dp : bucket)
				{
					sum += dp.value;
				}
				double avg = sum / bucket.size();

				triggeredAverages.put(param, avg);

				bucket.clear();
			}
		}

		for (Map.Entry<Parameter, Double> e : triggeredAverages.entrySet())
		{
			Parameter param = e.getKey();
			double avgVal = e.getValue();

			lastValues.putIfAbsent(param, null);
			if (lastValues.get(param) != null)
			{
				if (lastValues.get(param) == avgVal)
				{
					continue;
				}
			}
			lastValues.put(param, avgVal);

			averagedPointsMap.computeIfAbsent(param, k -> new ArrayList<>(3));

			List<DataPoint> avgList = averagedPointsMap.get(param);
			avgList.add(new DataPoint(snapshot.getTime(), avgVal));

			if (avgList.size() == 3)
			{

				double t0 = avgList.get(0).time;
				double t1 = avgList.get(1).time;
				double t2 = avgList.get(2).time;

				double v0 = avgList.get(0).value;
				double v1 = avgList.get(1).value;
				double v2 = avgList.get(2).value;

				List<Double> xValues = Arrays.asList(t0, t1, t2);
				List<Double> yValues = Arrays.asList(v0, v1, v2);

				CubicSplinePolynomial csp = new CubicSplinePolynomial(xValues, yValues, CubicSplinePolynomial.NATURAL);

				Polynomial polynomialResult = csp.getSegments().getFirst();

				for (DataAccumulatorListener listener : listeners)
				{
					listener.onCubicSplineCompleted(e.getKey(), t0, t2, polynomialResult);
				}

				DataPoint thirdPoint = avgList.get(2);
				avgList.clear();
				avgList.add(thirdPoint);
			}
		}
	}

	private record DataPoint(double time, double value)
		{
		}



}
