package com.telemetryparser.core.fixed;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.ui.DebugWindow;
import com.telemetryparser.ui.components.slider.SliderSetting;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CVOrientationResult extends CVResult
{
	private static final Map<Parameter, Double> orientationAngleMap = new ConcurrentHashMap<>();
	private final double orientation;
	private final double correlation;
	private final double symmetry;
	private Double computedOrientation;
	private final BufferedImage filteredImage;
	public CVOrientationResult(BufferedImage original, BufferedImage processed, BufferedImage filteredImage, double orientation, double correlation, double symmetry, Parameter parameter)
	{
		super(original, processed);
		this.filteredImage = filteredImage;
		this.orientation = orientation;
		this.correlation = correlation;
		this.symmetry = symmetry;
		computeOrientation(parameter);
	}

	public BufferedImage getFilteredImage()
	{
		return filteredImage;
	}

	private void computeOrientation(Parameter parameter)
	{
		boolean strengthAcceptable = correlation >= DebugWindow.getSettingValue(SliderSetting.ORIENTATION_STRENGTH);
		boolean symmetryAcceptable = symmetry >= DebugWindow.getSettingValue(SliderSetting.ORIENTATION_SYMMETRY);

		Double rawAngle = (strengthAcceptable && symmetryAcceptable) ? orientation : null;
		if(rawAngle == null)
		{
			computedOrientation = null;
			return;
		}
		double newAngle = rawAngle;
		Double oldAngle = orientationAngleMap.get(parameter);

		if(oldAngle != null)
		{
			double diff = newAngle - oldAngle;
			if(diff > 170)
			{
				newAngle -= 180;
			}
			else if(diff < -170)
			{
				newAngle += 180;
			}
		}
		orientationAngleMap.put(parameter, newAngle);
		computedOrientation = newAngle;
	}

	public double getOrientationStrength()
	{
		return correlation;
	}

	public double getOrientationSymmetry()
	{
		return symmetry;
	}

	public Double getComputedOrientation()
	{
		return computedOrientation;
	}

	public double getOrientation()
	{
		return orientation;
	}

	public static CompletableFuture<CVResult> fromImageAsync(BufferedImage original, Parameter parameter)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			if (original == null)
			{
				return new CVOrientationResult(null, null, null, 0d, 0d, 0d, parameter);
			}
			BufferedImage image = preprocessImage(original, false);

			int width = image.getWidth();
			int height = image.getHeight();
			int[][] gray = new int[height][width];

			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					int rgb = image.getRGB(x, y);
					int r = (rgb >> 16) & 0xFF;
					int g = (rgb >> 8) & 0xFF;
					int b = (rgb) & 0xFF;
					int grayVal = (r + g + b) / 3;
					gray[y][x] = grayVal;
				}
			}

			List<int[]> edgePixels = new ArrayList<>();
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					if (gray[y][x] >= 128)
					{
						edgePixels.add(new int[] {x, y});
					}
				}
			}

			if (edgePixels.isEmpty())
			{
				return new CVOrientationResult(image, null, null, 0d, 0d, 0d, parameter);
			}

			BufferedImage filteredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					filteredImage.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
			for (int[] pixel : edgePixels)
			{
				filteredImage.setRGB(pixel[0], pixel[1], Color.WHITE.getRGB());
			}

			double sumX = 0.0;
			double sumY = 0.0;
			int n = edgePixels.size();
			for (int[] pt : edgePixels)
			{
				sumX += pt[0];
				sumY += pt[1];
			}
			double meanX = sumX / n;
			double meanY = sumY / n;

			double sxx = 0.0;
			double syy = 0.0;
			double sxy = 0.0;
			for (int[] pt : edgePixels)
			{
				double dx = pt[0] - meanX;
				double dy = pt[1] - meanY;
				sxx += dx * dx;
				syy += dy * dy;
				sxy += dx * dy;
			}

			double trace = sxx + syy;
			double diff = sxx - syy;
			double det = 0.25 * diff * diff + sxy * sxy;
			double lambda1 = 0.5 * trace + Math.sqrt(det);
			double lambda2 = 0.5 * trace - Math.sqrt(det);

			double lambdaMax = Math.max(lambda1, lambda2);
			double a = sxx - lambdaMax;
			double b = sxy;
			double c = sxy;
			double d = syy - lambdaMax;

			double vx, vy;
			if (Math.abs(b) > 1e-12)
			{
				vx = 1.0;
				vy = -a / b;
			}
			else if (Math.abs(c) > 1e-12)
			{
				vx = -d / c;
				vy = 1.0;
			}
			else
			{
				vx = 1.0;
				vy = 0.0;
			}

			double lengthVec = Math.sqrt(vx * vx + vy * vy);
			vx /= lengthVec;
			vy /= lengthVec;

			double angleRad = Math.atan2(vx, vy);
			double angleDeg = Math.toDegrees(angleRad);

			if (angleDeg > 90)
			{
				angleDeg -= 180;
			}
			angleDeg *= -1;
			double angleRounded = Math.round(angleDeg * 10.0) / 10.0;

			double lambdaMin = Math.min(lambda1, lambda2);
			double orientationStrength = 0.0;
			if ((lambdaMax + lambdaMin) > 1e-12)
			{
				orientationStrength = (lambdaMax - lambdaMin) / (lambdaMax + lambdaMin);
			}

			double symmetry = computeMirrorSymmetry(edgePixels, meanX, meanY, vx, vy, width, height);

			return new CVOrientationResult(original, image, filteredImage, angleRounded, orientationStrength, symmetry, parameter);
		});
	}

	private static double computeMirrorSymmetry(List<int[]> edgePixels, double meanX, double meanY, double vx, double vy, int width, int height)
	{
		Set<Long> edgeSet = new HashSet<>(edgePixels.size());
		for (int[] p : edgePixels)
		{
			int x = p[0];
			int y = p[1];
			long packed = ((long) y << 20) | (long) x;
			edgeSet.add(packed);
		}

		int total = edgePixels.size();
		int matched = 0;

		for (int[] p : edgePixels)
		{
			double px = p[0];
			double py = p[1];

			double dx  = px - meanX;
			double dy  = py - meanY;

			double dot = dx*vx + dy*vy;

			double rx = meanX + 2*dot*vx - dx;
			double ry = meanY + 2*dot*vy - dy;

			int rxi = (int) Math.round(rx);
			int ryi = (int) Math.round(ry);

			if (rxi >= 0 && rxi < width && ryi >= 0 && ryi < height)
			{
				long packedRef = ((long) ryi << 20) | (long) rxi;
				if (edgeSet.contains(packedRef))
				{
					matched++;
				}
			}
		}

		return (double) matched / (double) total;
	}

}
