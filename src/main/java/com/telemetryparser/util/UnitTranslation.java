package com.telemetryparser.util;

import java.util.Objects;

public enum UnitTranslation
{
	Kilometers("Kilometers", 1.0),
	Miles("Miles", 0.621371),
	Feet("Feet", 3280.84),
	Mixed("Feet & Miles", 3280.84);

	private final String name;
	private final double amount;

	UnitTranslation(String name, double amount)
	{
		this.name = name;
		this.amount = amount;
	}

	public double getTranslationAmount()
	{
		return amount;
	}

	public static UnitTranslation fromString(String name)
	{
		for(UnitTranslation unitTranslation : UnitTranslation.values())
		{
			if(Objects.equals(unitTranslation.name, name))
			{
				return unitTranslation;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
