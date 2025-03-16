package com.telemetryparser.ui.icon;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

public class IconManager
{
	private static final Map<Icon, FlatSVGIcon> iconMap = new HashMap<>();
	static
	{
		for(Icon icon : Icon.values())
		{
			iconMap.put(icon, new FlatSVGIcon("com/telemetryparser/" + icon.filePath + ".svg").derive(icon.size, icon.size));
		}
		updateIcons();
	}

	public static FlatSVGIcon get(Icon icon)
	{
		return iconMap.get(icon);
	}

	public static void updateIcons()
	{
		FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter((Color color) -> UIManager.getColor("Label.foreground"));
		for(FlatSVGIcon icon : iconMap.values())
		{
			icon.setColorFilter(filter);
		}
	}
}
