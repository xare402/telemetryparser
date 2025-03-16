package com.telemetryparser.util;

import java.awt.*;
import java.io.*;

public class FontLoader
{
	public static void loadCustomFonts()
	{
		try
		{
			File fontsDir = new File("fonts");
			if (fontsDir.exists() && fontsDir.isDirectory())
			{
				for (File fontFile : fontsDir.listFiles())
				{
					if (fontFile.getName().toLowerCase().endsWith(".otf") || fontFile.getName().toLowerCase().endsWith(".ttf"))
					{
						Font font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(fontFile));
						GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
						ge.registerFont(font);
						System.out.println("Loaded font: " + fontFile.getName());
					}
				}
			}
		} catch (IOException | FontFormatException e)
		{
			e.printStackTrace();
		}
	}
}
