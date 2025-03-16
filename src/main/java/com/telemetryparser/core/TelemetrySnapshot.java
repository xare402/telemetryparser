package com.telemetryparser.core;

import static com.telemetryparser.core.Parameter.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;

public class TelemetrySnapshot
{
	public double timeOccured;
	private final long systemTime;
	private final long playerTime;

	public Map<Parameter, Double> parameterMap;

	public TelemetrySnapshot(double timeOccured, Map<Parameter, Double> parameterMap, long systemTime, long playerTime)
	{
		this.timeOccured = timeOccured;
		this.parameterMap = parameterMap;
		this.systemTime = systemTime;
		this.playerTime = playerTime;
		nullifyInvalidValues();
	}

	public long getSystemTime()
	{
		return systemTime;
	}

	public long getPlayerTime()
	{
		return playerTime;
	}

	public boolean hasStage1()
	{
		return parameterMap.get(STAGE_1_SPEED) != null;
	}

	public boolean hasStage2()
	{
		return parameterMap.get(STAGE_2_SPEED) != null;
	}

	public Double get(Parameter parameter)
	{
		return parameterMap.get(parameter);
	}

	public Double getWithPrecision(Parameter parameter)
	{
		return withPrecision(parameter, parameterMap.get(parameter));
	}

	public Double withPrecision(Parameter parameter, Double value)
	{
		if (value == null)
		{
			return null;
		}
		double scale = Math.pow(10, parameter.precision);
		return Math.round(value * scale) / scale;
	}

	public void set(Parameter parameter, double value)
	{
		parameterMap.put(parameter, value);
	}

	private String nullOr(Number val)
	{
		if (val == null)
		{
			return "";
		}
		else
		{
			if (val instanceof Float || val instanceof Double)
			{
				DecimalFormat df = new DecimalFormat("#.00");
				return df.format(val);
			}
			else
			{
				return String.valueOf(val);
			}
		}
	}

	public String getTimeAsString()
	{
		return String.format("%.2f", timeOccured);
	}

	public double getTime()
	{
		return timeOccured;
	}

	public String getStage1String()
	{
		String stage1 = "";
		if (hasStage1())
		{
			stage1 += "Stage 1 is at " + getWithPrecision(STAGE_1_ALTITUDE) + " KM traveling at " + getWithPrecision(STAGE_1_SPEED) + " KM/h";
		}
		return stage1;
	}

	public String getStage2String()
	{
		String stage2 = "";
		if (hasStage2())
		{
			stage2 += "Stage 2 is at " + getWithPrecision(STAGE_2_ALTITUDE) + " KM traveling at " + getWithPrecision(STAGE_2_SPEED) + " KM/h";
		}
		return stage2;
	}

	public String toString()
	{
		return "T" + getTimeAsString() + "s, " + getStage1String() + ", " + getStage2String();
	}

	public String toCSVTranslated(ParameterType... parameterTypes)
	{
		StringBuilder csv = new StringBuilder();
		csv.append(getTimeAsString());
		for(Parameter parameter : Parameter.values())
		{
			for(ParameterType parameterType : parameterTypes)
			{
				if(parameter.type == parameterType)
				{
					parameterMap.putIfAbsent(parameter, null);
				}
			}
		}
		for (Parameter parameter : parameterMap.keySet())
		{
			if (Arrays.stream(parameterTypes).anyMatch(param -> param == parameter.type))
			{
				csv.append(',');
				Double paramValue = get(parameter);
				if (paramValue != null)
				{
					Double value = getWithPrecision(parameter);
					csv.append(nullOr(withPrecision(parameter, value)));
				}
			}
		}
		return csv.toString();
	}


	public void nullifyInvalidValues()
	{
		nullifyStageValues(STAGE_1_SPEED, STAGE_1_ALTITUDE, STAGE_1_LOX, STAGE_1_CH4, STAGE_1_ORIENTATION, STAGE_1_ENGINES, STAGE_1_CENTER_ENGINES, STAGE_1_MIDDLE_ENGINES, STAGE_1_OUTER_ENGINES);
		nullifyStageValues(STAGE_2_SPEED, STAGE_2_ALTITUDE, STAGE_2_LOX, STAGE_2_CH4, STAGE_2_ORIENTATION, STAGE_2_ENGINES, STAGE_2_CENTER_ENGINES, STAGE_2_OUTER_ENGINES);
	}

	private void nullifyStageValues(Parameter speed, Parameter altitude, Parameter... parameters)
	{
		if (parameterMap.get(speed) == null || parameterMap.get(altitude) == null)
		{
			for (Parameter p : parameters)
			{
				if (parameterMap.containsKey(p))
				{
					parameterMap.put(p, null);
				}
			}
		}
	}
}