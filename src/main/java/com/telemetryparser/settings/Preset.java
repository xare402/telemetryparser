package com.telemetryparser.settings;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.util.EngineLocation;
import com.telemetryparser.util.ROIRatios;
import com.telemetryparser.util.UnitTranslation;
import java.util.HashMap;
import java.util.Map;

public class Preset
{
	private String name = "Custom";
	private final Map<Parameter, ROIRatios> ratios;
	private final Map<Integer, EngineLocation> stage1EngineMap;
	private final Map<Integer, EngineLocation> stage2EngineMap;
	private UnitTranslation speedTranslation;
	private UnitTranslation altitudeTranslation;
	private boolean usesComma;
	private String timePrefix;

	public Preset()
	{
		name = "Custom";
		ratios = new HashMap<>();
		stage1EngineMap = new HashMap<>();
		stage2EngineMap = new HashMap<>();
		speedTranslation = UnitTranslation.Kilometers;
		altitudeTranslation = UnitTranslation.Kilometers;
		usesComma = false;
		timePrefix = "T";
	}
	public Preset(Map<Parameter, ROIRatios> ratios,
				  Map<Integer, EngineLocation> stage1EngineMap,
				  Map<Integer, EngineLocation> stage2EngineMap,
				  UnitTranslation speedTranslation,
				  UnitTranslation altitudeTranslation,
				  boolean usesComma,
				  String timePrefix) {
		this.ratios = ratios;
		this.stage1EngineMap = stage1EngineMap;
		this.stage2EngineMap = stage2EngineMap;
		this.speedTranslation = speedTranslation;
		this.altitudeTranslation = altitudeTranslation;
		this.usesComma = usesComma;
		this.timePrefix = timePrefix;
	}

	public String name()
	{
		return name;
	}

	public Map<Parameter, ROIRatios> ratios()
	{
		return ratios;
	}

	public Map<Integer, EngineLocation> stage1EngineMap()
	{
		return stage1EngineMap;
	}

	public Map<Integer, EngineLocation> stage2EngineMap()
	{
		return stage2EngineMap;
	}

	public UnitTranslation speedTranslation()
	{
		return speedTranslation;
	}

	public UnitTranslation altitudeTranslation()
	{
		return altitudeTranslation;
	}

	public boolean usesComma()
	{
		return usesComma;
	}

	public String timePrefix()
	{
		return timePrefix;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setSpeedTranslation(UnitTranslation speedTranslation)
	{
		this.speedTranslation = speedTranslation;
	}

	public void setAltitudeTranslation(UnitTranslation altitudeTranslation)
	{
		this.altitudeTranslation = altitudeTranslation;
	}

	public void setUsesComma(boolean usesComma)
	{
		this.usesComma = usesComma;
	}

	public void setTimePrefix(String timePrefix)
	{
		this.timePrefix = timePrefix;
	}
}

