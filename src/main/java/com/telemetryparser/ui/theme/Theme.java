package com.telemetryparser.ui.theme;

import com.formdev.flatlaf.IntelliJTheme;
import java.awt.Color;

public class Theme
{
	public Class<? extends IntelliJTheme.ThemeLaf> clazz;
	public Color background;
	public Color foreground;
	public Color accent;
	public ThemeType themeType;

	public Theme(Class<? extends IntelliJTheme.ThemeLaf> clazz, Color background, Color foreground, Color accent, ThemeType themeType)
	{
		this.clazz = clazz;
		this.background = background;
		this.foreground = foreground;
		this.accent = accent;
		this.themeType = themeType;
	}
}
