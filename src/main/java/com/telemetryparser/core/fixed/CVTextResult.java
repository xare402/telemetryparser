package com.telemetryparser.core.fixed;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.settings.Preset;
import com.telemetryparser.util.TesseractManager;
import com.telemetryparser.util.UnitTranslation;
import com.telemetryparser.util.Util;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class CVTextResult extends CVResult
{
	private String result;
	public CVTextResult(BufferedImage original, BufferedImage processed, String result)
	{
		super(original, processed);
		this.result = result;
	}

	public String getResult()
	{
		return result;
	}

	public void replaceComma()
	{
		result = result.replace(".", "");
	}

	public void parseAsTime(String timePrefix)
	{
		result = Util.parseTime(result, timePrefix);
	}

	public static CompletableFuture<CVResult> fromImageAsync(BufferedImage image)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			BufferedImage preprocessed = preprocessImage(image, false);
			return new CVTextResult(image, preprocessed, TesseractManager.ocr(preprocessed));
		});
	}

	public Double getAsTranslatedUnit(Parameter param, Preset preset)
	{
		Double value = null;
		try
		{
			value = Double.parseDouble(result);
			if (param.type == ParameterType.SPEED)
			{
				value /= preset.speedTranslation().getTranslationAmount();
			}
			else if (param.type == ParameterType.ALTITUDE)
			{
				if (preset.altitudeTranslation() == UnitTranslation.Mixed)
				{
					if (value < 200) //todo more thorough handling maybe
					{
						value *= 5280;
					}
				}
				value /= preset.altitudeTranslation().getTranslationAmount();
			}
		}
		catch (Exception ignored)
		{

		}
		return value;
	}
}
