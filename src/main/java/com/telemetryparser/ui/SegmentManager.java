package com.telemetryparser.ui;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.util.math.Polynomial;
import com.telemetryparser.util.math.CubicSplinePolynomial; // <-- Assuming you have this import

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SegmentManager
{
	private final Map<Parameter, List<TelemetrySegment>> segmentsMap = new HashMap<>();
	private final List<TelemetrySegment> horizontalSpeedSegments = new ArrayList<>();
	public void addSegment(Parameter parameter, double firstTime, double lastTime, Polynomial polynomial)
	{
		TelemetrySegment seg = new TelemetrySegment(firstTime, lastTime, polynomial);
		segmentsMap.computeIfAbsent(parameter, k -> new ArrayList<>()).add(seg);

		if (parameter == Parameter.STAGE_1_SPEED || parameter == Parameter.STAGE_1_ALTITUDE)
		{
			Parameter otherParam = (parameter == Parameter.STAGE_1_SPEED) ? Parameter.STAGE_1_ALTITUDE : Parameter.STAGE_1_SPEED;

			List<TelemetrySegment> otherSegs = segmentsMap.get(otherParam);
			if (otherSegs != null && !otherSegs.isEmpty())
			{
				for (TelemetrySegment otherSeg : otherSegs)
				{
					double overlapStart = Math.max(firstTime, otherSeg.firstTime);
					double overlapEnd = Math.min(lastTime, otherSeg.lastTime);

					if (overlapEnd > overlapStart)
					{
						double t0 = overlapStart;
						double t2 = overlapEnd;
						double t1 = 0.5 * (t0 + t2);

						TelemetrySegment speedSeg = (parameter == Parameter.STAGE_1_SPEED) ? seg : otherSeg;
						TelemetrySegment altitudeSeg = (parameter == Parameter.STAGE_1_ALTITUDE) ? seg : otherSeg;

						Double val0 = computeHorizontal(speedSeg, altitudeSeg, t0);
						Double val1 = computeHorizontal(speedSeg, altitudeSeg, t1);
						Double val2 = computeHorizontal(speedSeg, altitudeSeg, t2);

						if (val0 == null || val1 == null || val2 == null)
						{
							continue;
						}

						List<Double> xVals = new ArrayList<>();
						List<Double> yVals = new ArrayList<>();
						xVals.add(t0);
						yVals.add(val0);
						xVals.add(t1);
						yVals.add(val1);
						xVals.add(t2);
						yVals.add(val2);

						CubicSplinePolynomial csp = new CubicSplinePolynomial(xVals, yVals, CubicSplinePolynomial.NATURAL);

						Polynomial horizPoly = csp.getSegments().getFirst();

						TelemetrySegment horizSeg = new TelemetrySegment(overlapStart, overlapEnd, horizPoly);
						horizontalSpeedSegments.add(horizSeg);
					}
				}
			}
		}
	}

	public Double evaluateStage1HorizontalSpeedIntegral(Double start, Double end)
	{
		List<TelemetrySegment> sorted = new ArrayList<>(horizontalSpeedSegments);
		if (sorted.isEmpty())
		{
			return null;
		}
		sorted.sort(Comparator.comparingDouble(seg -> seg.firstTime));

		List<double[]> coverage = getMergedCoverage(sorted);

		double overallStart = coverage.getFirst()[0];
		double overallEnd = coverage.getLast()[1];

		double from = (start == null) ? overallStart : start;
		double to = (end == null) ? overallEnd : end;

		if (to < from || from < overallStart || to > overallEnd)
		{
			return null;
		}

		if (lacksCoverage(coverage, from, to))
		{
			return null;
		}

		double totalIntegral = 0.0;
		double currentStart = from;

		for (TelemetrySegment seg : sorted)
		{
			if (seg.lastTime < from)
			{
				continue;
			}
			if (seg.firstTime > to)
			{
				break;
			}

			double segStart = Math.max(seg.firstTime, currentStart);
			double segEnd = Math.min(seg.lastTime, to);

			if (segStart > currentStart + 1e-12)
			{
				return null;
			}

			double valStart = seg.integratedPolynomial.calculate(segStart);
			double valEnd = seg.integratedPolynomial.calculate(segEnd);

			totalIntegral += (valEnd - valStart);

			currentStart = segEnd;

			if (currentStart >= to - 1e-12)
			{
				break;
			}
		}

		if (currentStart < to - 1e-12)
		{
			return null;
		}

		return totalIntegral;
	}

	private Double computeHorizontal(TelemetrySegment speedSeg, TelemetrySegment altSeg, double t)
	{
		double sVal = speedSeg.polynomial.calculate(t);
		double aPrime = altSeg.derivativePolynomial.calculate(t) * 3600; //derivative converts km/h to km/s, this converts back


		double radicand = sVal * sVal - aPrime * aPrime;
		if (radicand < 0)
		{
			return null;
		}
		return Math.sqrt(radicand);
	}

	public Double getHorizontalSpeed(Double time)
	{
		if (time == null)
		{
			return null;
		}
		for (TelemetrySegment seg : horizontalSpeedSegments)
		{
			if (time >= seg.firstTime && time <= seg.lastTime)
			{
				return seg.polynomial.calculate(time);
			}
		}
		return null;
	}

	public void reset()
	{
		segmentsMap.clear();
		horizontalSpeedSegments.clear();
	}

	public Double evaluateIndefiniteIntegral(Parameter parameter, Double start, Double end)
	{
		List<TelemetrySegment> sortedSegments = getSortedSegments(parameter);
		if (sortedSegments.isEmpty())
		{
			return null;
		}

		List<double[]> coverage = getMergedCoverage(sortedSegments);

		double overallStart = coverage.getFirst()[0];
		double overallEnd = coverage.getLast()[1];

		double from = (start == null) ? overallStart : start;
		double to = (end == null) ? overallEnd : end;

		if (to < from || from < overallStart || to > overallEnd)
		{
			return null;
		}

		double totalIntegral = 0.0;
		double currentStart = from;

		for (TelemetrySegment seg : sortedSegments)
		{
			if (seg.lastTime < from)
			{
				continue;
			}
			if (seg.firstTime > to)
			{
				break;
			}

			double segStart = Math.max(seg.firstTime, currentStart);
			double segEnd = Math.min(seg.lastTime, to);

			if (segStart > currentStart + 1e-12)
			{
				return null;
			}

			double valStart = seg.integratedPolynomial.calculate(segStart);
			double valEnd = seg.integratedPolynomial.calculate(segEnd);
			totalIntegral += (valEnd - valStart);

			currentStart = segEnd;
			if (currentStart >= to - 1e-12)
			{
				break;
			}
		}

		if (currentStart < to - 1e-12)
		{
			return null;
		}

		return totalIntegral;
	}

	public Double evaluateIndefiniteIntegralOfDerivative(Parameter parameter, Double start, Double end)
	{
		List<TelemetrySegment> sortedSegments = getSortedSegments(parameter);
		if (sortedSegments.isEmpty())
		{
			return null;
		}

		List<double[]> coverage = getMergedCoverage(sortedSegments);
		double overallStart = coverage.get(0)[0];
		double overallEnd = coverage.get(coverage.size() - 1)[1];

		double from = (start == null) ? overallStart : start;
		double to = (end == null) ? overallEnd : end;

		if (to < from || from < overallStart || to > overallEnd)
		{
			return null;
		}

		if (lacksCoverage(coverage, from, to))
		{
			return null;
		}

		Double pFrom = evaluatePolynomial(parameter, from);
		Double pTo = evaluatePolynomial(parameter, to);
		if (pFrom == null || pTo == null)
		{
			return null;
		}
		return pTo - pFrom;
	}

	public Double evaluateIndefiniteIntegralFromMagnitudeAndVertical(Parameter magnitudeParam, Parameter altitudeParam, Double start, Double end)
	{
		List<TelemetrySegment> magSegments = getSortedSegments(magnitudeParam);
		List<TelemetrySegment> altSegments = getSortedSegments(altitudeParam);
		if (magSegments.isEmpty() || altSegments.isEmpty())
		{
			return null;
		}

		List<double[]> magCoverage = getMergedCoverage(magSegments);
		List<double[]> altCoverage = getMergedCoverage(altSegments);

		double magStart = magCoverage.getFirst()[0];
		double magEnd = magCoverage.getLast()[1];
		double altStart = altCoverage.getFirst()[0];
		double altEnd = altCoverage.getLast()[1];

		double overallStart = Math.max(magStart, altStart);
		double overallEnd = Math.min(magEnd, altEnd);

		double from = (start == null) ? overallStart : start;
		double to = (end == null) ? overallEnd : end;

		if (to < from || from < overallStart || to > overallEnd)
		{
			return null;
		}

		if (lacksCoverage(magCoverage, from, to) || lacksCoverage(altCoverage, from, to))
		{
			return null;
		}

		double dt = 0.1;
		double integral = 0.0;

		double prevT = from;
		Double prevVal = getFunctionValue(magnitudeParam, altSegments, from);
		if (prevVal == null)
		{
			return null;
		}

		for (double t = from + dt; t <= to + 1e-12; t += dt)
		{
			double currentT = Math.min(t, to);
			Double currentVal = getFunctionValue(magnitudeParam, altSegments, currentT);

			if (currentVal == null)
			{
				return null;
			}
			integral += 0.5 * (currentVal + prevVal) * (currentT - prevT);

			prevT = currentT;
			prevVal = currentVal;

			if (Math.abs(currentT - to) < 1e-12)
			{
				break;
			}
		}

		return integral;
	}

	public Double evaluate(Parameter parameter, double time)
	{
		TelemetrySegment seg = findSegment(parameter, time);
		return (seg != null) ? seg.polynomial.calculate(time) : null;
	}

	public Double getHorizontalSpeedFromMagnitudeAndAltitude(Parameter speedParam, Parameter altitudeParam, double time)
	{
		TelemetrySegment speedSegment = findSegment(speedParam, time);
		TelemetrySegment altitudeSegment = findSegment(altitudeParam, time);

		if (speedSegment != null && altitudeSegment != null)
		{
			Polynomial speedSquared = speedSegment.polynomial.multiply(speedSegment.polynomial);
			Polynomial dAltScaled = altitudeSegment.derivativePolynomial.copyAndScalePolynomial(3600);
			Polynomial speedYSquared = dAltScaled.multiply(dAltScaled);
			Polynomial difference = speedSquared.subtract(speedYSquared);

			double squaredValue = difference.calculate(time);
			if (squaredValue < 0)
			{
				return null;
			}
			return Math.sqrt(squaredValue);
		}
		return null;
	}

	public Double evaluateDerivative(Parameter parameter, double time)
	{
		TelemetrySegment seg = findSegment(parameter, time);
		return (seg != null) ? seg.derivativePolynomial.calculate(time) : null;
	}

	private TelemetrySegment findSegment(Parameter parameter, double time)
	{
		List<TelemetrySegment> segments = segmentsMap.get(parameter);
		if (segments == null)
		{
			return null;
		}

		for (TelemetrySegment seg : segments)
		{
			if (time >= seg.firstTime && time <= seg.lastTime)
			{
				return seg;
			}
		}
		return null;
	}

	private Double getFunctionValue(Parameter magnitudeParam, List<TelemetrySegment> altSegments, double t)
	{
		Double magVal = evaluatePolynomial(magnitudeParam, t);
		if (magVal == null)
		{
			return null;
		}

		TelemetrySegment altSeg = null;
		for (TelemetrySegment s : altSegments)
		{
			if (t >= s.firstTime && t <= s.lastTime)
			{
				altSeg = s;
				break;
			}
		}
		if (altSeg == null)
		{
			return null;
		}

		double vertSpeed = altSeg.derivativePolynomial.calculate(t);

		double val = magVal * magVal - vertSpeed * vertSpeed;
		if (val < 0.0)
		{
			return null;
		}
		return (Math.sqrt(val)) / 3600.0;
	}

	private List<TelemetrySegment> getSortedSegments(Parameter parameter)
	{
		List<TelemetrySegment> segs = segmentsMap.get(parameter);
		if (segs == null || segs.isEmpty())
		{
			return Collections.emptyList();
		}
		List<TelemetrySegment> sorted = new ArrayList<>(segs);
		sorted.sort(Comparator.comparingDouble(s -> s.firstTime));
		return sorted;
	}

	private List<double[]> getMergedCoverage(List<TelemetrySegment> sortedSegments)
	{
		List<double[]> coverage = new ArrayList<>();
		if (sortedSegments.isEmpty())
		{
			return coverage;
		}

		double currentStart = sortedSegments.getFirst().firstTime;
		double currentEnd = sortedSegments.getFirst().lastTime;

		for (int i = 1; i < sortedSegments.size(); i++)
		{
			TelemetrySegment seg = sortedSegments.get(i);
			if (seg.firstTime <= currentEnd + 1e-12)
			{
				currentEnd = Math.max(currentEnd, seg.lastTime);
			}
			else
			{
				coverage.add(new double[]{currentStart, currentEnd});
				currentStart = seg.firstTime;
				currentEnd = seg.lastTime;
			}
		}
		coverage.add(new double[]{currentStart, currentEnd});
		return coverage;
	}

	private boolean lacksCoverage(List<double[]> coverage, double start, double end)
	{
		double current = start;
		for (double[] interval : coverage)
		{
			double iStart = interval[0];
			double iEnd = interval[1];
			if (iStart > current + 1e-12)
			{
				return true;
			}
			if (iEnd > current)
			{
				current = iEnd;
			}
			if (current >= end - 1e-12)
			{
				return false;
			}
		}
		return (!(current >= end - 1e-12));
	}

	private Double evaluatePolynomial(Parameter parameter, double time)
	{
		List<TelemetrySegment> segs = segmentsMap.get(parameter);
		if (segs == null)
		{
			return null;
		}
		for (TelemetrySegment seg : segs)
		{
			if (time >= seg.firstTime && time <= seg.lastTime)
			{
				return seg.polynomial.calculate(time);
			}
		}
		return null;
	}

	private static class TelemetrySegment
	{
		final double firstTime;
		final double lastTime;
		final Polynomial polynomial;
		final Polynomial derivativePolynomial;
		final Polynomial integratedPolynomial;

		TelemetrySegment(double firstTime, double lastTime, Polynomial polynomial)
		{
			this.firstTime = firstTime;
			this.lastTime = lastTime;
			this.polynomial = polynomial;

			this.derivativePolynomial = polynomial.derived();
			this.integratedPolynomial = polynomial.integrated();
		}
	}
}
