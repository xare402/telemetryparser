package com.telemetryparser.core.fixed;

import com.telemetryparser.ui.DebugWindow;
import com.telemetryparser.ui.components.slider.SliderSetting;
import com.telemetryparser.util.ImageUtil;
import java.awt.image.BufferedImage;

public abstract class CVResult
{
	private final BufferedImage original;
	private final BufferedImage processed;
	public CVResult(BufferedImage original, BufferedImage processed)
	{
		this.original = original;
		this.processed = processed;
	}

	public BufferedImage getOriginalImage()
	{
		return original;
	}
	public BufferedImage getProcessedImage()
	{
		return processed;
	}

	static BufferedImage preprocessImage(BufferedImage img, boolean applyScale)
	{
		if (img == null)
		{
			return null;
		}

		BufferedImage scaled = (applyScale) ? ImageUtil.scaleUpImage(img, DebugWindow.getSettingValue(SliderSetting.SCALE)) : img;

		double radius = DebugWindow.getSettingValue(SliderSetting.RADIUS);
		double amount = DebugWindow.getSettingValue(SliderSetting.AMOUNT);
		int thresh = (int) Math.round(DebugWindow.getSettingValue(SliderSetting.UNSHARP_THRESHOLD) * 255);
		BufferedImage sharpened = ImageUtil.unsharpMask(scaled, radius, amount, thresh);

		return ImageUtil.thresholdBlackWhite(sharpened,(int) DebugWindow.getSettingValue(SliderSetting.BW_THRESHOLD));
	}
}
