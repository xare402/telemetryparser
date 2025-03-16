package com.telemetryparser.ui.videoplayer;

import java.awt.image.BufferedImage;

public interface VideoPlayerFrameChangedListener
{
	void onFrameChanged(BufferedImage image, long playerTime, long systemTime);
}
