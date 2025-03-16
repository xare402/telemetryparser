package com.telemetryparser.datatransformation.util;

import com.telemetryparser.util.math.CubicSplinePolynomial;
import com.telemetryparser.util.math.Polynomial;
import com.telemetryparser.util.math.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MalleableData
{
	private final Map<String, List<Double>> columns;
	private final String timeColumn;

	public MalleableData(Map<String, List<Double>> columns, String primaryColumn)
	{
		this.columns = columns;
		this.timeColumn = primaryColumn;
	}

	private static ListPair removeNulls(List<Double> xValues, List<Double> yValues)
	{
		List<Double> newX = new ArrayList<>();
		List<Double> newY = new ArrayList<>();

		for (int i = 0; i < xValues.size(); i++)
		{
			Double x = xValues.get(i);
			Double y = yValues.get(i);
			if (x != null && y != null)
			{
				newX.add(x);
				newY.add(y);
			}
		}

		return new ListPair(newX, newY);
	}

	public Map<String, List<Double>> getCurrentData()
	{
		return columns;
	}

	public void removeDuplicates()
	{
		for (Map.Entry<String, List<Double>> entry : columns.entrySet())
		{
			List<Double> column = entry.getValue();

			int i = 0;
			while (i < column.size())
			{
				Double currentVal = column.get(i);

				if (currentVal == null)
				{
					i++;
					continue;
				}

				int startIndex = i;
				i++;

				while (i < column.size() && currentVal.equals(column.get(i)))
				{
					i++;
				}

				int endIndex = i - 1;
				int blockLength = endIndex - startIndex + 1;
				int centerIndex = startIndex + (blockLength - 1) / 2;

				for (int j = startIndex; j <= endIndex; j++)
				{
					if (j != centerIndex)
					{
						column.set(j, null);
					}
				}
			}
		}
	}

	public void restrictLists(Set<String> toRestrict, int amount)
	{
		if (columns == null || columns.isEmpty())
		{
			throw new IllegalArgumentException("columns map is null or empty.");
		}
		if (!columns.containsKey(timeColumn))
		{
			throw new IllegalArgumentException("xAxis column '" + timeColumn + "' does not exist.");
		}
		if (amount <= 0)
		{
			throw new IllegalArgumentException("amount must be greater than 0.");
		}

		List<Double> xAxisList = columns.get(timeColumn);
		int oldSize = xAxisList.size();

		int blockCount = (oldSize + amount - 1) / amount;

		for (Map.Entry<String, List<Double>> entry : columns.entrySet())
		{
			String columnName = entry.getKey();
			if(!toRestrict.contains(columnName))
			{
				continue;
			}
			List<Double> oldList = entry.getValue();
			List<Double> newList = new ArrayList<>(blockCount);

			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int blockStart = blockIndex * amount;
				int blockEnd = Math.min(blockStart + amount, oldSize);

				if (columnName.equals(timeColumn))
				{
					if (blockStart < oldSize)
					{
						newList.add(oldList.get(blockStart));
					}
					else
					{
						newList.add(null);
					}
				}
				else
				{
					double sum = 0.0;
					int count = 0;

					int actualEnd = Math.min(blockEnd, oldList.size());

					for (int i = blockStart; i < actualEnd; i++)
					{
						Double val = oldList.get(i);
						if (val != null)
						{
							sum += val;
							count++;
						}
					}

					if (count == 0)
					{
						newList.add(null);
					}
					else
					{
						newList.add(sum / count);
					}
				}
			}

			columns.put(columnName, newList);
		}
	}

	public void interpolateAndDerive2x(String inColumn, String interpolatedColumn, String firstDerivativeColumn, String secondDerivativeColumn)
	{
		List<Double> originalX = columns.get(timeColumn);
		List<Double> originalY = columns.get(inColumn);
		ListPair xAndY = removeNulls(columns.get(timeColumn), columns.get(inColumn));
		xAndY.sortByX();
		int startingX = xAndY.getXValues().getFirst().intValue();
		List<Double> xValues = xAndY.getXValues();
		List<Double> yValues = xAndY.getYValues();
		Double highestXValue = xValues.getLast();

		CubicSplinePolynomial poly = new CubicSplinePolynomial(xValues, yValues, CubicSplinePolynomial.NATURAL);

		CubicSplinePolynomial derivedPoly = poly.getDerivative();
		CubicSplinePolynomial secondDerivedPoly = derivedPoly.getDerivative();
		List<Double> calculatedYValues = new ArrayList<>(Collections.nCopies(originalX.size(), null));
		List<Double> calculatedDYValues = new ArrayList<>(Collections.nCopies(originalY.size(), null));
		List<Double> calculatedDDYValues = new ArrayList<>(Collections.nCopies(originalY.size(), null));
		for (int i = startingX; i < originalX.size(); i++)
		{
			if (i > 0 && originalX.get(i) <= highestXValue)
			{
				calculatedYValues.set(i, poly.calculate(originalX.get(i)) * (5/(9.81*18.0)));
				calculatedDYValues.set(i, derivedPoly.calculate(originalX.get(i)) * (5/(9.81*18.0)));
				calculatedDDYValues.set(i, secondDerivedPoly.calculate(originalX.get(i)) * (5/(9.81*18.0)));
			}
		}

		columns.put(interpolatedColumn, calculatedYValues);
		columns.put(firstDerivativeColumn, calculatedDYValues);
		columns.put(secondDerivativeColumn, calculatedDDYValues);
	}

	boolean hasPeaked = false;
	boolean shouldInvert = false;
	double peakHorizontalSpeed = 0.0;

	public void computeECEFColumns(double initialLatitude, double initialLongitude, double initialAltitude, double initialAzimuth,
								   String timeColumn, String altitudeChangeColumn, String speedMagnitudeColumn,
								   String outputECEFXColumn, String outputECEFYColumn, String outputECEFZColumn,
								   String outputDeltaECEFXColumn, String outputDeltaECEFYColumn, String outputDeltaECEFZColumn
	)
	{
		List<Double> timeChangeList = columns.get(timeColumn);
		List<Double> altitudeChangeList = columns.get(altitudeChangeColumn);
		List<Double> speedList = columns.get(speedMagnitudeColumn);

		if (altitudeChangeList == null || speedList == null)
		{
			throw new IllegalArgumentException("Input columns for altitude change or speed are missing.");
		}
		int rowCount = Math.min(altitudeChangeList.size(), speedList.size());

		List<Double> ecefX = new ArrayList<>(rowCount);
		List<Double> ecefY = new ArrayList<>(rowCount);
		List<Double> ecefZ = new ArrayList<>(rowCount);
		List<Double> deltaX = new ArrayList<>(rowCount);
		List<Double> deltaY = new ArrayList<>(rowCount);
		List<Double> deltaZ = new ArrayList<>(rowCount);
		for (int i = 0; i < rowCount; i++)
		{
			ecefX.add(null);
			ecefY.add(null);
			ecefZ.add(null);
			deltaX.add(null);
			deltaY.add(null);
			deltaZ.add(null);
		}

		Vec3 originECEF = CoordUtil.getECEF(initialLatitude, initialLongitude, initialAltitude);

		Vec3 currentECEF = originECEF;

		for (int i = 0; i < rowCount; i++)
		{
			Double altChangeKmH = altitudeChangeList.get(i);
			Double speedKmH = speedList.get(i);

			if (altChangeKmH == null || speedKmH == null)
			{
				continue;
			}

			Vec3 velocityECEF_km_h = CoordUtil.getECEFVelocities(currentECEF, originECEF, initialAzimuth, altChangeKmH, speedKmH);

			// Calculate the horizontal component to track its speed
			Vec3 upCurrent = currentECEF.copy().normalize();
			double dotUp = velocityECEF_km_h.copy().dot(upCurrent);
			Vec3 vertical = upCurrent.scale(dotUp / upCurrent.lengthSquared());
			Vec3 horizontal = velocityECEF_km_h.copy().sub(vertical);
			double horizontalSpeed = horizontal.length();

			if (!shouldInvert)
			{
				if (horizontalSpeed > peakHorizontalSpeed)
				{
					peakHorizontalSpeed = horizontalSpeed;
				}
				else if (peakHorizontalSpeed > 0 && horizontalSpeed < peakHorizontalSpeed * 0.7 && horizontalSpeed > 2000)
				{
					hasPeaked = true;
				}

				if (hasPeaked && horizontalSpeed < peakHorizontalSpeed * 0.05)
				{
					shouldInvert = true;
				}
			}

			if (shouldInvert && speedMagnitudeColumn.contains("1"))
			{
				velocityECEF_km_h.sub(horizontal.scale(2));
			}

			if(i > 0)
			{
				double timeDifference = timeChangeList.get(i) - timeChangeList.get(i-1);
				if(timeDifference <= 0)
				{
					continue;
				}
				Vec3 newECEF = CoordUtil.getTranslatedECEF(currentECEF, velocityECEF_km_h, timeDifference);

				ecefX.set(i, newECEF.x);
				ecefY.set(i, newECEF.y);
				ecefZ.set(i, newECEF.z);

				deltaX.set(i, velocityECEF_km_h.x);
				deltaY.set(i, velocityECEF_km_h.y);
				deltaZ.set(i, velocityECEF_km_h.z);

				currentECEF = newECEF;
			}
		}

		columns.put(outputECEFXColumn, ecefX);
		columns.put(outputECEFYColumn, ecefY);
		columns.put(outputECEFZColumn, ecefZ);
		columns.put(outputDeltaECEFXColumn, deltaX);
		columns.put(outputDeltaECEFYColumn, deltaY);
		columns.put(outputDeltaECEFZColumn, deltaZ);
	}

	public String getTimeColumn()
	{
		return timeColumn;
	}

	public void computePerEngineAcceleration(String timeColumn, String accelerationColumn, String engineColumn, String outputPerEngineAccelerationColumn)
	{
		List<Double> timeColumnValues = columns.get(timeColumn);
		List<Double> accelerationColumnValues = columns.get(accelerationColumn);
		List<Double> engineColumnValues = columns.get(engineColumn);
		int rowCount = timeColumnValues.size();
		List<Double> engineAcceleration = new ArrayList<>(rowCount);
		for(int i = 0; i < rowCount; i++)
		{
			engineAcceleration.add(null);
		}
		for(int i = 0; i < rowCount; i++)
		{
			Double accelValue = accelerationColumnValues.get(i);
			Double engineValue = engineColumnValues.get(i);
			if(accelValue != null && engineValue != null && engineValue != 0)
			{
				engineAcceleration.set(i, accelerationColumnValues.get(i) / engineColumnValues.get(i));
			}
		}
		columns.put(outputPerEngineAccelerationColumn, engineAcceleration);
	}
}
