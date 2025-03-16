package com.telemetryparser.datatransformation.util;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import static java.util.Map.Entry.comparingByKey;

public class ListPair
{
	private final List<Double> xValues;
	private final List<Double> yValues;

	public ListPair(List<Double> xValues, List<Double> yValues)
	{
		this.xValues = xValues;
		this.yValues = yValues;
	}

	public List<Double> getXValues()
	{
		return xValues;
	}

	public List<Double> getYValues()
	{
		return yValues;
	}

	public void sortByX()
	{
		List<AbstractMap.SimpleEntry<Double, Double>> zipped = new ArrayList<>();
		for (int i = 0; i < xValues.size(); i++)
		{
			zipped.add(new AbstractMap.SimpleEntry<>(xValues.get(i), yValues.get(i)));
		}

		zipped.sort(comparingByKey());

		xValues.clear();
		yValues.clear();

		for (AbstractMap.SimpleEntry<Double, Double> entry : zipped)
		{
			xValues.add(entry.getKey());
			yValues.add(entry.getValue());
		}
	}

	@Override
	public String toString()
	{
		return "ListPair{xValues=" + xValues + ", yValues=" + yValues + "}";
	}
}
