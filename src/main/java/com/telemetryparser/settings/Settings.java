package com.telemetryparser.settings;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.util.EngineLocation;
import com.telemetryparser.util.ROIRatios;
import com.telemetryparser.util.UnitTranslation;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class Settings
{
	private static final Properties properties = new Properties();
	private static final String ROI_FILE = "rois.properties";

	static
	{
		File roiFile = new File(ROI_FILE);
		File parentDir = roiFile.getParentFile();
		if (parentDir != null && !parentDir.exists())
		{
			if (!parentDir.mkdirs())
			{
				System.out.println("Could not create directories for properties file.");
			}
		}

		try
		{
			if (roiFile.createNewFile())
			{
				System.out.println("Empty properties file created successfully.");
			}
		}
		catch (IOException e)
		{
			System.out.println("An error occurred while creating the file: " + e.getMessage());
		}

		try
		{
			properties.load(new FileInputStream(ROI_FILE));
		}
		catch (IOException e)
		{
			System.out.println("Could not load initial properties");
		}
	}

	public static void deleteProperty(String group, String property)
	{
		String key = group + "." + property;
		if (properties.containsKey(key))
		{
			properties.remove(key);
			try(FileOutputStream fos = new FileOutputStream(ROI_FILE))
			{
				properties.store(fos, "ROI Properties");
			}
			catch (IOException e)
			{
				System.out.println("Failed to delete property: " + key + " due to: " + e.getMessage());
			}
		}
	}

	public static void deleteEngines(String group, int stage)
	{
		String prefix = group + "." + "Stage " + stage;
		deleteByPrefix(prefix);
	}


	public static void setProperty(String group, String property, String value)
	{
		properties.setProperty(group+"."+property, value);
		try(FileOutputStream fos = new FileOutputStream(ROI_FILE))
		{
			properties.store(fos, "ROI Properties");
		}
		catch (IOException e)
		{
			System.out.println("Failed to store property: " + group+"."+property + " with value: " + value);
		}
	}

	public static String getProperty(String group, String property, String defaultValue)
	{
		return properties.getProperty(group+"."+property, defaultValue);
	}

	public static String getProperty(String group, String property)
	{
		return properties.getProperty(group+"."+property);
	}

	public static Double getPropertyAsDouble(String group, String property)
	{
		try
		{
			return Double.parseDouble(properties.getProperty(group+"."+property));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static Preset loadPreset(String presetName)
	{
		Map<Parameter, ROIRatios> ratios = new LinkedHashMap<>();
		Map<Integer, EngineLocation> stage1EngineMap = new HashMap<>();
		Map<Integer, EngineLocation> stage2EngineMap = new HashMap<>();

		UnitTranslation speedTranslation = UnitTranslation.fromString(getProperty(presetName, "speed", "Kilometers"));
		UnitTranslation altitudeTranslation = UnitTranslation.fromString(getProperty(presetName, "altitude", "Kilometers"));

		boolean usesComma = Objects.equals(getProperty(presetName, "usesComma", "false"), "true");
		String timePrefix = getProperty(presetName, "timePrefix", "T");

		for (Parameter parameter : Parameter.values())
		{
			if (parameter.type != ParameterType.ENGINES_VARIANT)
			{
				if(parameter == Parameter.STAGE_1_ENGINES)
				{
					stage1EngineMap = loadEngineData(presetName, parameter);
				}
				else if(parameter == Parameter.STAGE_2_ENGINES)
				{
					stage2EngineMap = loadEngineData(presetName, parameter);
				}
				String stripped = parameter.name.replace(" ", ".");
				ROIRatios ratio = loadROIRatios(presetName, stripped);
				ratios.put(parameter, ratio);
			}
		}

		return new Preset(ratios, stage1EngineMap, stage2EngineMap, speedTranslation, altitudeTranslation, usesComma, timePrefix);
	}

	private static ROIRatios loadROIRatios(String preset, String keyPrefix)
	{
		try
		{
			String request = "ROI." + keyPrefix;
			Double xRatio = Settings.getPropertyAsDouble(preset, request + ".xRatio");
			Double yRatio = Settings.getPropertyAsDouble(preset, request + ".yRatio");
			Double widthRatio = Settings.getPropertyAsDouble(preset, request + ".widthRatio");
			Double heightRatio = Settings.getPropertyAsDouble(preset, request + ".heightRatio");
			if (xRatio != null && yRatio != null && widthRatio != null && heightRatio != null)
			{
				return new ROIRatios(xRatio, yRatio, widthRatio, heightRatio);
			}
		}
		catch (Exception ignored)
		{

		}
		return null;
	}

	private static Map<Integer, EngineLocation> loadEngineData(String group, Parameter parameter)
	{
		Map<Integer, EngineLocation> engineLocationMap = new LinkedHashMap<>();
		try
		{
			int engineNumber = 1;
			while (engineNumber != 0)
			{
				String value = Settings.getProperty(group, parameter.name + "-" + engineNumber);
				if (value == null)
				{
					engineNumber = 0;
				}
				else
				{
					String[] split = value.split(",", -1);
					if (split.length != 4)
					{
						throw new IllegalArgumentException("Expected 3 Arguments. Found " + split.length + ".");
					}
					Point engineLocation = new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
					int radius = Integer.parseInt(split[2]);
					engineLocationMap.put(engineNumber, new EngineLocation(engineLocation, radius, Integer.parseInt(split[3])));
					engineNumber++;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return engineLocationMap;
	}

	public static void copyPreset(String currentPreset, String newPreset)
	{
		String prefix = currentPreset + ".";

		for (String key : properties.stringPropertyNames())
		{
			if (key.startsWith(prefix))
			{
				String suffix = key.substring(prefix.length());
				String value = properties.getProperty(key);
				String newKey = newPreset + "." + suffix;
				properties.setProperty(newKey, value);
			}
		}

		try(FileOutputStream fos = new FileOutputStream(ROI_FILE))
		{
			properties.store(fos, "ROI Properties");
		}
		catch (IOException e)
		{
			System.out.println("Failed to store properties while copying preset: " + e.getMessage());
		}
	}

	public static String[] collectPresets()
	{
		Set<String> presetSet = new LinkedHashSet<>();

		presetSet.add("Custom");

		for (String key : properties.stringPropertyNames())
		{
			if(!key.startsWith("setting") && !key.startsWith("sliderSetting"))
			{
				int dotIndex = key.indexOf('.');
				if (dotIndex < 0)
				{
					continue;
				}

				String potentialPreset = key.substring(0, dotIndex);

				presetSet.add(potentialPreset);
			}
		}

		return presetSet.toArray(String[]::new);
	}

	public static void deletePreset(String preset)
	{
		deleteByPrefix(preset + ".");
	}

	private static void deleteByPrefix(String prefix)
	{
		List<String> toRemove = new ArrayList<>();

		for (String key : properties.stringPropertyNames())
		{
			if (key.startsWith(prefix))
			{
				toRemove.add(key);
			}
		}

		for (String key : toRemove)
		{
			properties.remove(key);
		}

		try(FileOutputStream fos = new FileOutputStream(ROI_FILE))
		{
			properties.store(fos, "ROI Properties");
		}
		catch (IOException e)
		{
			System.out.println("Failed to delete properties for preset: " + prefix + " due to: " + e.getMessage());
		}
	}
}
