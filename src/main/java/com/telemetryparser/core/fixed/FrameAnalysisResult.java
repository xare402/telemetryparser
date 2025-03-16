package com.telemetryparser.core.fixed;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.core.TelemetrySnapshot;
import com.telemetryparser.util.Util;
import java.util.LinkedHashMap;
import java.util.Map;

public record FrameAnalysisResult(FrameData frameData, Map<Parameter, CVResult> results)
{
	public Integer getTimeAsSeconds()
	{
		Integer seconds = null;
		CVResult timeResult = results.get(Parameter.TIME);
		if(timeResult instanceof CVTextResult textResult)
		{
			try
			{
				seconds = Util.determineSeconds(textResult.getResult());
			}
			catch (Exception ignored)
			{

			}
		}
		return seconds;
	}

	public TelemetrySnapshot createSnapshot(int seconds)
	{
		Map<Parameter, Double> numericMap = new LinkedHashMap<>();
		for(Parameter parameter : Parameter.values())
		{
			if(!parameter.include)
			{
				continue;
			}
			numericMap.put(parameter, null);
		}
		for(Parameter parameter : Parameter.values())
		{
			if(!parameter.include || parameter.type == ParameterType.ENGINES_VARIANT)
			{
				continue;
			}

			CVResult result = results.get(parameter);
			if(result != null)
			{
				Double value = null;
				switch(result)
				{
					case CVTextResult textResult -> value = textResult.getAsTranslatedUnit(parameter, frameData.preset());
					case CVEngineResult engineResult ->
					{
						value = (double) engineResult.getEngineStates().getEnginesActive();
						if(parameter == Parameter.STAGE_1_ENGINES)
						{
							numericMap.put(Parameter.STAGE_1_CENTER_ENGINES, (double) engineResult.getEngineStates().getEnginesActiveForRing(1));
							numericMap.put(Parameter.STAGE_1_MIDDLE_ENGINES, (double) engineResult.getEngineStates().getEnginesActiveForRing(2));
							numericMap.put(Parameter.STAGE_1_OUTER_ENGINES, (double) engineResult.getEngineStates().getEnginesActiveForRing(3));
						}
						else if(parameter == Parameter.STAGE_2_ENGINES)
						{
							numericMap.put(Parameter.STAGE_2_CENTER_ENGINES, (double) engineResult.getEngineStates().getEnginesActiveForRing(1));
							numericMap.put(Parameter.STAGE_2_OUTER_ENGINES, (double) engineResult.getEngineStates().getEnginesActiveForRing(2));

						}
					}
					case CVOrientationResult orientationResult -> value = orientationResult.getComputedOrientation();
					case CVFuelResult fuelResult -> value = fuelResult.getFuelPercent();
					default -> {}
				}
				numericMap.put(parameter, value);
			}
			else
			{
				numericMap.put(parameter, null);
			}
		}
		return new TelemetrySnapshot(seconds, numericMap, frameData.systemTime(), frameData.playerTime());
	}
}
