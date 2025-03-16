package com.telemetryparser.util.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Polynomial
{
	private final List<Double> coefficients;

	public Polynomial(double... coefficients)
	{
		this.coefficients = new ArrayList<>();
		for (double c : coefficients)
		{
			this.coefficients.add(c);
		}
		trimTrailingZeros(this.coefficients);
	}

	public Polynomial(List<Double> coefficients)
	{
		this.coefficients = new ArrayList<>(coefficients);
		trimTrailingZeros(this.coefficients);
	}

	public double calculate(double x)
	{
		double result = 0.0;
		double powerOfX = 1.0;
		for (double c : coefficients)
		{
			result += c * powerOfX;
			powerOfX *= x;
		}
		return result;
	}

	public Polynomial derived()
	{
		List<Double> derivCoeffs = getDerivativeCoefficients(this.coefficients, 1);
		return new Polynomial(derivCoeffs);
	}

	public Polynomial integrated()
	{

		List<Double> integratedCoeffs = new ArrayList<>();
		integratedCoeffs.add(0.0);

		for (int i = 0; i < coefficients.size(); i++)
		{
			double newCoeff = coefficients.get(i) / (i + 1);
			integratedCoeffs.add(newCoeff);
		}

		trimTrailingZeros(integratedCoeffs);

		return new Polynomial(integratedCoeffs);
	}

	public double calculateDerivative(double x)
	{
		return calculateDerivative(x, 1);
	}

	public double calculateDerivative(double x, int order)
	{
		if (order < 1)
		{
			throw new IllegalArgumentException("Order of derivative must be >= 1");
		}

		List<Double> derivativeCoeffs = getDerivativeCoefficients(this.coefficients, order);

		double result = 0.0;
		double powerOfX = 1.0;
		for (double c : derivativeCoeffs)
		{
			result += c * powerOfX;
			powerOfX *= x;
		}
		return result;
	}

	public Polynomial add(Polynomial polynomial)
	{
		List<Double> resultCoeffs = new ArrayList<>();
		int maxSize = Math.max(this.coefficients.size(), polynomial.coefficients.size());

		for (int i = 0; i < maxSize; i++)
		{
			double c1 = i < this.coefficients.size() ? this.coefficients.get(i) : 0.0;
			double c2 = i < polynomial.coefficients.size() ? polynomial.coefficients.get(i) : 0.0;
			resultCoeffs.add(c1 + c2);
		}

		trimTrailingZeros(resultCoeffs);
		return new Polynomial(resultCoeffs);
	}

	public Polynomial subtract(Polynomial polynomial)
	{
		List<Double> resultCoeffs = new ArrayList<>();
		int maxSize = Math.max(this.coefficients.size(), polynomial.coefficients.size());

		for (int i = 0; i < maxSize; i++)
		{
			double c1 = i < this.coefficients.size() ? this.coefficients.get(i) : 0.0;
			double c2 = i < polynomial.coefficients.size() ? polynomial.coefficients.get(i) : 0.0;
			resultCoeffs.add(c1 - c2);
		}

		trimTrailingZeros(resultCoeffs);
		return new Polynomial(resultCoeffs);
	}

	private List<Double> getDerivativeCoefficients(List<Double> originalCoeffs, int order)
	{
		List<Double> current = new ArrayList<>(originalCoeffs);

		for (int k = 0; k < order; k++)
		{
			List<Double> next = new ArrayList<>();
			for (int i = 1; i < current.size(); i++)
			{
				next.add(i * current.get(i));
			}
			current = next;
		}

		return current;
	}

	private void trimTrailingZeros(List<Double> coeffs)
	{
		int i = coeffs.size() - 1;
		while (i > 0 && Math.abs(coeffs.get(i)) < 1e-15)
		{
			coeffs.remove(i);
			i--;
		}
	}

	public List<Double> getCoefficients()
	{
		return Collections.unmodifiableList(this.coefficients);
	}

	@Override
	public String toString()
	{
		if (coefficients.isEmpty())
		{
			return "0";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = coefficients.size() - 1; i >= 0; i--)
		{
			double c = coefficients.get(i);
			if (Math.abs(c) < 1e-15)
			{
				continue;
			}
			if (!sb.isEmpty())
			{
				sb.append(c >= 0 ? " + " : " - ");
			}
			else if (c < 0)
			{
				sb.append("-");
			}
			double absC = Math.abs(c);

			if (i == 0)
			{
				sb.append(String.format("%.2f", absC));
			}
			else if (i == 1)
			{
				if (absC == 1.0)
				{
					sb.append("x");
				}
				else
				{
					sb.append(String.format("%.2f*x", absC));
				}
			}
			else
			{
				if (absC == 1.0)
				{
					sb.append("x^").append(i);
				}
				else
				{
					sb.append(String.format("%.2f*x^%d", absC, i));
				}
			}
		}
		return sb.toString();
	}

	public Polynomial multiply(Polynomial polynomial)
	{
		List<Double> resultCoeffs = new ArrayList<>();
		int sizeThis = this.coefficients.size();
		int sizeOther = polynomial.coefficients.size();

		for (int i = 0; i < sizeThis + sizeOther - 1; i++)
		{
			resultCoeffs.add(0.0);
		}

		for (int i = 0; i < sizeThis; i++)
		{
			for (int j = 0; j < sizeOther; j++)
			{
				double newVal = resultCoeffs.get(i + j) + this.coefficients.get(i) * polynomial.coefficients.get(j);
				resultCoeffs.set(i + j, newVal);
			}
		}

		trimTrailingZeros(resultCoeffs);

		return new Polynomial(resultCoeffs);
	}

	public Polynomial copyAndScalePolynomial(double value)
	{
		List<Double> scaledCoeffs = new ArrayList<>();
		for (Double c : this.coefficients)
		{
			scaledCoeffs.add(c * value);
		}
		return new Polynomial(scaledCoeffs);
	}
}
