package com.telemetryparser.core.fixed;

import com.telemetryparser.settings.Preset;
import java.awt.image.BufferedImage;

public record FrameData(BufferedImage frame, long playerTime, long systemTime, Preset preset) {}
