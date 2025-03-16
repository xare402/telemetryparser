package com.telemetryparser.dataserver;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.util.math.Polynomial;

public interface DataAccumulatorListener
{
	void onCubicSplineCompleted(Parameter parameter, double firstTime, double lastTime, Polynomial spline);
}
