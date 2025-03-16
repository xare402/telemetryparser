package com.telemetryparser.core.fixed;

import java.util.Map;

public record CVEngineData(Map<Integer, EngineState> engineStates)
{
	public int getEnginesActive()
	{
		return getEnginesActiveForRing(-1);
	}

	public int getEnginesActiveForRing(int ring)
	{
		int enginesActive = 0;
		for(EngineState engineState : engineStates.values())
		{
			if((ring == -1 || engineState.engineLocation().ring() == ring) && engineState.active())
			{
				enginesActive++;
			}
		}
		return enginesActive;
	}
}
