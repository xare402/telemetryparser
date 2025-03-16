package com.telemetryparser.core.fixed;

import com.telemetryparser.util.EngineLocation;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CVEngineResult extends CVResult
{
	private final CVEngineData engineData;
	public CVEngineResult(BufferedImage original, BufferedImage processed, CVEngineData engineData)
	{
		super(original, processed);
		this.engineData = engineData;
	}

	public CVEngineData getEngineStates()
	{
		return engineData;
	}

	public static CompletableFuture<CVResult> fromImageAsync(BufferedImage image, Map<Integer, EngineLocation> engineMap, int ring)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			BufferedImage processed = preprocessImage(image, false);
			Map<Integer, EngineState> engineStates = new LinkedHashMap<>();
			for(Integer engineNumber : engineMap.keySet())
			{
				EngineLocation engineLocation = engineMap.get(engineNumber);
				if(ring == engineLocation.ring() || ring == -1)
				{
					engineStates.put(engineNumber, new EngineState(engineLocation, isActive(processed, engineLocation.middle())));
				}
			}
			return new CVEngineResult(image, processed, new CVEngineData(engineStates));
		});
	}

	private static boolean isActive(BufferedImage image, Point p)
	{
		if (image == null)
		{
			return false;
		}

		for (int dy = -1; dy <= 1; dy++)
		{
			for (int dx = -1; dx <= 1; dx++)
			{
				int x = p.x + dx;
				int y = p.y + dy;

				if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
				{
					return false;
				}

				int rgb = image.getRGB(x, y);
				if (!isWhite(rgb))
				{
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isWhite(int rgb)
	{
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = (rgb) & 0xFF;
		return (r == 255 && g == 255 && b == 255);
	}
}
