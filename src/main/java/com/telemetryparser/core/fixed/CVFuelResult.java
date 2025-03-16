package com.telemetryparser.core.fixed;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class CVFuelResult extends CVResult
{
	private final double fuelPercent;
	private final int columnsInRange;
	public CVFuelResult(BufferedImage original, BufferedImage processed, double fuelPercent, int columnsInRange)
	{
		super(original, processed);
		this.fuelPercent = fuelPercent;
		this.columnsInRange = columnsInRange;
	}


	public int getColumnsInRange()
	{
		return columnsInRange;
	}
	public double getFuelPercent()
	{
		return fuelPercent;
	}

	public static CompletableFuture<CVResult> fromImageAsync(BufferedImage image, int width)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			if (image != null)
			{
				int fuelCount = countRightToLeftBlock(computeColumnsInRange(image, 4));
				long fuelBarWidth = Math.round(.125 * width);
				return new CVFuelResult(image, null, (1 - ((double) fuelCount / fuelBarWidth)) * 100, fuelCount); //todo fix null :)
			}
			return new CVFuelResult(null, null, 0, 0);
		});
	}

	private static int countRightToLeftBlock(boolean[] inRange)
	{
		int width = inRange.length;
		boolean foundBlock = false;

		for (int x = width - 1; x >= 0; x--)
		{
			if (!foundBlock)
			{
				if (inRange[x])
				{
					foundBlock = true;
				}
			}
			else
			{
				if (!inRange[x])
				{
					return width-x;
				}
			}
		}
		return 0;
	}

	private static boolean[] computeColumnsInRange(BufferedImage image, int middleCount)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		if (middleCount > height)
		{
			middleCount = height;
		}

		int middleStart = (height / 2) - (middleCount / 2);
		int middleEnd = middleStart + middleCount - 1;

		if (middleStart < 0)
		{
			middleStart = 0;
		}
		if (middleEnd >= height)
		{
			middleEnd = height - 1;
		}
		int actualCount = (middleEnd - middleStart + 1);

		boolean[] result = new boolean[width];

		for (int x = 0; x < width; x++)
		{
			long sumR = 0, sumG = 0, sumB = 0;

			for (int y = middleStart; y <= middleEnd; y++)
			{
				int rgb = image.getRGB(x, y);
				Color c = new Color(rgb, true);
				sumR += c.getRed();
				sumG += c.getGreen();
				sumB += c.getBlue();
			}
			int avgR = (int) (sumR / actualCount);
			int avgG = (int) (sumG / actualCount);
			int avgB = (int) (sumB / actualCount);

			result[x] = (avgR < 70 && avgG < 70 && avgB < 70);
		}
		return result;
	}
}
