package com.telemetryparser.ui;

import com.telemetryparser.util.ROIRatios;

@FunctionalInterface
public interface RegionSelectionCallback
{
	void onRegionSelected(ROIRatios roi);
}
