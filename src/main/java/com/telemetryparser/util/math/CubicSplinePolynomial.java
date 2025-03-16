package com.telemetryparser.util.math;

import java.util.ArrayList;
import java.util.List;

public class CubicSplinePolynomial
{
	public static final int NATURAL = 0;
	private final List<Double> xValues;
	private final List<Polynomial> segments;

	public CubicSplinePolynomial(List<Double> xValues, List<Double> yValues, int boundaryCondition)
	{
		if (xValues.size() != yValues.size())
		{
			throw new IllegalArgumentException("xValues and yValues must have the same size.");
		}
		if (xValues.size() < 2)
		{
			throw new IllegalArgumentException("At least two points are required for spline interpolation.");
		}
		if (boundaryCondition != NATURAL)
		{
			throw new UnsupportedOperationException("Only NATURAL boundary condition is implemented.");
		}

		this.xValues = new ArrayList<>(xValues);
		this.segments = buildSplineSegments(xValues, yValues);
	}

	private CubicSplinePolynomial(List<Double> xValues, List<Polynomial> segments)
	{
		this.xValues = xValues;
		this.segments = segments;
	}

	public Polynomial getPolyAt(double x)
	{
		int i = findSegmentIndex(x);
		return segments.get(i);
	}

	public double calculate(double x)
	{
		Polynomial p = getPolyAt(x);
		return p.calculate(x);
	}

	public CubicSplinePolynomial getDerivative()
	{
		List<Polynomial> derivativeSegments = new ArrayList<>();
		for (Polynomial segment : segments)
		{
			Polynomial deriv = segment.derived();
			derivativeSegments.add(deriv);
		}
		return new CubicSplinePolynomial(this.xValues, derivativeSegments);
	}

	private List<Polynomial> buildSplineSegments(List<Double> xs, List<Double> ys)
	{
		int n = xs.size();
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++)
		{
			x[i] = xs.get(i);
			y[i] = ys.get(i);
		}

		double[] h = new double[n - 1];
		for (int i = 0; i < n - 1; i++)
		{
			h[i] = x[i + 1] - x[i];
			if (h[i] <= 0)
			{
				throw new IllegalArgumentException("xValues must be strictly increasing. Current: " + x[i] + " Next: " + x[i+1]);
			}
		}

		double[] M = solveSecondDerivativesNatural(x, y, h);

		List<Polynomial> resultSegments = new ArrayList<>(n - 1);
		for (int i = 0; i < n - 1; i++)
		{
			double a = y[i];
			double b = (y[i + 1] - y[i]) / h[i] - (h[i] * (2 * M[i] + M[i + 1])) / 6.0;
			double c = M[i] / 2.0;
			double d = (M[i + 1] - M[i]) / (6.0 * h[i]);

			double A = a - b * x[i] + c * x[i] * x[i] - d * x[i] * x[i] * x[i];
			double B = b - 2.0 * c * x[i] + 3.0 * d * x[i] * x[i];
			double C = c - 3.0 * d * x[i];
			double D = d;

			Polynomial segmentPoly = new Polynomial(A, B, C, D);
			resultSegments.add(segmentPoly);
		}
		return resultSegments;
	}

	private double[] solveSecondDerivativesNatural(double[] x, double[] y, double[] h)
	{
		int n = x.length;
		double[] alpha = new double[n];
		double[] l = new double[n];
		double[] mu = new double[n];
		double[] z = new double[n];
		double[] M = new double[n];

		for (int i = 1; i < n - 1; i++)
		{
			alpha[i] = (3.0 / h[i]) * (y[i + 1] - y[i]) - (3.0 / h[i - 1]) * (y[i] - y[i - 1]);
		}

		l[0] = 1.0;
		mu[0] = 0.0;
		z[0] = 0.0;

		for (int i = 1; i < n - 1; i++)
		{
			l[i] = 2.0 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1];
			mu[i] = h[i] / l[i];
			z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i];
		}

		l[n - 1] = 1.0;
		z[n - 1] = 0.0;
		M[n - 1] = 0.0;

		for (int j = n - 2; j >= 0; j--)
		{
			M[j] = z[j] - mu[j] * M[j + 1];
		}
		return M;
	}

	private int findSegmentIndex(double x)
	{
		int n = xValues.size();
		if (x <= xValues.getFirst())
		{
			return 0;
		}
		if (x >= xValues.get(n - 1))
		{
			return n - 2;
		}
		for (int i = 0; i < n - 1; i++)
		{
			if (xValues.get(i) <= x && x < xValues.get(i + 1))
			{
				return i;
			}
		}
		return n - 2;
	}

	public List<Polynomial> getSegments()
	{
		return List.copyOf(segments);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("CubicSplinePolynomial:\n");
		for (int i = 0; i < segments.size(); i++)
		{
			sb.append(String.format(" Segment[%d] on [%.3f, %.3f]: %s\n",
				i, xValues.get(i), xValues.get(i + 1), segments.get(i).toString()));
		}
		return sb.toString();
	}
}
