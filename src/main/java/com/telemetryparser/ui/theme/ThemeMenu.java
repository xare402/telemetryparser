package com.telemetryparser.ui.theme;

import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlIJTheme;
import com.telemetryparser.ui.icon.IconManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicMenuItemUI;

public class ThemeMenu extends JMenu
{
	private static final Map<String, Theme> themes = new LinkedHashMap<>()
	{{
		//Light

		put("Arc", new Theme(FlatArcIJTheme.class, new Color(255, 255, 255), new Color(0, 0, 0), new Color(38, 121, 219), ThemeType.LIGHT));
		put("Arc - Orange", new Theme(FlatArcOrangeIJTheme.class, new Color(255, 255, 255), new Color(0, 0, 0), new Color(245, 121, 0), ThemeType.LIGHT));
		put("Cyan light", new Theme(FlatCyanLightIJTheme.class, new Color(228, 230, 235), new Color(29, 29, 29), new Color(43, 156, 184), ThemeType.LIGHT));
		put("Gray", new Theme(FlatGrayIJTheme.class, new Color(242, 243, 245), new Color(31, 31, 31), new Color(30, 130, 230), ThemeType.LIGHT));
		put("Light Flat", new Theme(FlatLightFlatIJTheme.class, new Color(238, 238, 242), new Color(51, 51, 51), new Color(83, 151, 230), ThemeType.LIGHT));
		put("Solarized Light", new Theme(FlatSolarizedLightIJTheme.class, new Color(238, 232, 213), new Color(46, 78, 88), new Color(61, 126, 192), ThemeType.LIGHT));

		//Material Light

		put("Atom One Light", new Theme(FlatAtomOneLightIJTheme.class, new Color(244, 244, 244), new Color(35, 35, 36), new Color(41, 121, 255), ThemeType.MATERIAL_LIGHT));
		put("GitHub", new Theme(FlatGitHubIJTheme.class, new Color(247, 248, 250), new Color(91, 97, 104), new Color(121, 203, 96), ThemeType.MATERIAL_LIGHT));
		put("Light Owl", new Theme(FlatLightOwlIJTheme.class, new Color(240, 240, 240), new Color(64, 63, 83), new Color(42, 162, 152), ThemeType.MATERIAL_LIGHT));
		put("Material Lighter", new Theme(FlatMaterialLighterIJTheme.class, new Color(250, 250, 250), new Color(84, 110, 122), new Color(0, 188, 212), ThemeType.MATERIAL_LIGHT));

		//Dark

		put("Arc Dark", new Theme(FlatArcDarkIJTheme.class, new Color(56, 60, 74), new Color(211, 218, 227), new Color(38, 121, 219), ThemeType.DARK));
		put("Arc Dark - Orange", new Theme(FlatArcDarkOrangeIJTheme.class, new Color(56, 60, 74), new Color(211, 218, 227), new Color(245, 121, 0), ThemeType.DARK));
		put("Carbon", new Theme(FlatCarbonIJTheme.class, new Color(23, 32, 48), new Color(186, 200, 203), new Color(2, 144, 152), ThemeType.DARK));
		put("Cobalt 2", new Theme(FlatCobalt2IJTheme.class, new Color(0, 27, 54), new Color(255, 240, 199), new Color(60, 98, 140), ThemeType.DARK));
		put("Dark Flat", new Theme(FlatDarkFlatIJTheme.class, new Color(56, 56, 56), new Color(204, 204, 204), new Color(42, 82, 133), ThemeType.DARK));
		put("Dark purple", new Theme(FlatDarkPurpleIJTheme.class, new Color(44, 44, 59), new Color(208, 208, 217), new Color(168, 94, 214), ThemeType.DARK));
		put("Dracula", new Theme(FlatDraculaIJTheme.class, new Color(65, 68, 80), new Color(187, 187, 187), new Color(189, 147, 249), ThemeType.DARK));
		put("Gradianto Dark Fuchsia", new Theme(FlatGradiantoDarkFuchsiaIJTheme.class, new Color(61, 33, 78), new Color(200, 204, 208), new Color(136, 83, 142), ThemeType.DARK));
		put("Gradianto Deep Ocean", new Theme(FlatGradiantoDeepOceanIJTheme.class, new Color(28, 39, 57), new Color(193, 193, 193), new Color(96, 143, 207), ThemeType.DARK));
		put("Gradianto Midnight Blue", new Theme(FlatGradiantoMidnightBlueIJTheme.class, new Color(40, 40, 57), new Color(212, 212, 212), new Color(135, 105, 207), ThemeType.DARK));
		put("Gradianto Nature Green", new Theme(FlatGradiantoNatureGreenIJTheme.class, new Color(32, 64, 63), new Color(204, 206, 206), new Color(96, 207, 146), ThemeType.DARK));
		put("Gruvbox Dark Hard", new Theme(FlatGruvboxDarkHardIJTheme.class, new Color(29, 32, 33), new Color(235, 219, 178), new Color(76, 135, 200), ThemeType.DARK));
		put("Gruvbox Dark Medium", new Theme(FlatGruvboxDarkMediumIJTheme.class, new Color(40, 40, 40), new Color(235, 219, 178), new Color(76, 135, 200), ThemeType.DARK));
		put("Gruvbox Dark Soft", new Theme(FlatGruvboxDarkSoftIJTheme.class, new Color(50, 48, 47), new Color(235, 219, 178), new Color(76, 135, 200), ThemeType.DARK));
		put("Hiberbee Dark", new Theme(FlatHiberbeeDarkIJTheme.class, new Color(55, 54, 53), new Color(191, 190, 189), new Color(255, 185, 0), ThemeType.DARK));
		put("High contrast", new Theme(FlatHighContrastIJTheme.class, new Color(0, 0, 0), new Color(255, 255, 255), new Color(26, 235, 255), ThemeType.DARK));
		put("Material Design Dark", new Theme(FlatMaterialDesignDarkIJTheme.class, new Color(31, 41, 46), new Color(187, 187, 187), new Color(255, 102, 111), ThemeType.DARK));
		put("Monocai", new Theme(FlatMonocaiIJTheme.class, new Color(45, 42, 47), new Color(252, 252, 251), new Color(80, 163, 237), ThemeType.DARK));
		put("Monokai Pro", new Theme(FlatMonokaiProIJTheme.class, new Color(45, 42, 46), new Color(147, 146, 147), new Color(255, 216, 102), ThemeType.DARK));
		put("Nord", new Theme(FlatNordIJTheme.class, new Color(46, 52, 64), new Color(236, 239, 244), new Color(136, 192, 208), ThemeType.DARK));
		put("One Dark", new Theme(FlatOneDarkIJTheme.class, new Color(33, 37, 43), new Color(171, 178, 191), new Color(86, 138, 242), ThemeType.DARK));
		put("Solarized Dark", new Theme(FlatSolarizedDarkIJTheme.class, new Color(14, 60, 74), new Color(169, 179, 174), new Color(57, 133, 199), ThemeType.DARK));
		put("Spacegray", new Theme(FlatSpacegrayIJTheme.class, new Color(35, 40, 48), new Color(167, 173, 186), new Color(191, 97, 106), ThemeType.DARK));
		put("Vuesion", new Theme(FlatVuesionIJTheme.class, new Color(32, 40, 49), new Color(201, 203, 207), new Color(205, 9, 64), ThemeType.DARK));
		put("XCode-Dark", new Theme(FlatXcodeDarkIJTheme.class, new Color(50, 51, 51), new Color(223, 223, 224), new Color(205, 204, 204), ThemeType.DARK));

		//Material Dark

		put("Atom One Dark", new Theme(FlatAtomOneDarkIJTheme.class, new Color(40, 44, 52), new Color(151, 159, 173), new Color(41, 121, 255), ThemeType.MATERIAL_DARK));
		put("GitHub Dark", new Theme(FlatGitHubDarkIJTheme.class, new Color(36, 41, 46), new Color(225, 228, 232), new Color(249, 130, 108), ThemeType.MATERIAL_DARK));
		put("Material Darker", new Theme(FlatMaterialDarkerIJTheme.class, new Color(33, 33, 33), new Color(176, 190, 197), new Color(255, 152, 0), ThemeType.MATERIAL_DARK));
		put("Material Deep Ocean", new Theme(FlatMaterialDeepOceanIJTheme.class, new Color(15, 17, 26), new Color(143, 147, 162), new Color(132, 255, 255), ThemeType.MATERIAL_DARK));
		put("Material Oceanic", new Theme(FlatMaterialOceanicIJTheme.class, new Color(38, 50, 56), new Color(176, 190, 197), new Color(0, 150, 136), ThemeType.MATERIAL_DARK));
		put("Material Palenight", new Theme(FlatMaterialPalenightIJTheme.class, new Color(41, 45, 62), new Color(166, 172, 205), new Color(171, 71, 188), ThemeType.MATERIAL_DARK));
		put("Moonlight", new Theme(FlatMoonlightIJTheme.class, new Color(34, 36, 54), new Color(200, 211, 245), new Color(116, 160, 241), ThemeType.MATERIAL_DARK));
		put("Night Owl", new Theme(FlatNightOwlIJTheme.class, new Color(1, 22, 39), new Color(214, 222, 235), new Color(126, 87, 194), ThemeType.MATERIAL_DARK));
	}};


	public ThemeMenu()
	{
		super("Theme");


		JMenu lightMenu = new JMenu("Light");
		JMenu materialLightMenu = new JMenu("Material Light");
		JMenu darkMenu = new JMenu("Dark");
		JMenu materialDarkMenu = new JMenu("Material Dark");

		themes.forEach((name, themeObj) ->
		{
			JMenuItem themeItem = createRightAlignedIconMenuItem(name, themeObj);

			themeItem.addActionListener(e ->
			{
				try
				{
					IntelliJTheme.ThemeLaf themeInstance = themeObj.clazz.getDeclaredConstructor().newInstance();
					UIManager.setLookAndFeel(themeInstance);
					SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
					IconManager.updateIcons();

				}
				catch (Exception ex)
				{
					System.out.println("Failed to apply theme: " + themeObj.clazz.getSimpleName());
				}
			});

			switch (themeObj.themeType)
			{
				case LIGHT:
					lightMenu.add(themeItem);
					break;
				case MATERIAL_LIGHT:
					materialLightMenu.add(themeItem);
					break;
				case DARK:
					darkMenu.add(themeItem);
					break;
				case MATERIAL_DARK:
					materialDarkMenu.add(themeItem);
					break;
			}
		});
		add(lightMenu);
		add(materialLightMenu);
		add(darkMenu);
		add(materialDarkMenu);
	}

	private static JMenuItem createRightAlignedIconMenuItem(String name, Theme themeObj)
	{
		JMenuItem themeItem = new JMenuItem(name);
		themeItem.setIcon(new ThemeColorIcon(themeObj.background, themeObj.foreground, themeObj.accent));
		themeItem.setHorizontalTextPosition(SwingUtilities.LEFT);
		themeItem.setUI(new BasicMenuItemUI()
		{
			@Override
			protected void paintMenuItem(Graphics g, JComponent c, Icon checkIcon, Icon arrowIcon, Color background, Color foreground, int defaultTextIconGap)
			{
				Icon realIcon = menuItem.getIcon();
				menuItem.setIcon(null);
				super.paintMenuItem(g, c, checkIcon, arrowIcon, background, foreground, defaultTextIconGap);
				if (realIcon != null)
				{
					Insets insets = menuItem.getInsets();
					int ICON_PADDING_RIGHT = 8;
					int x = menuItem.getWidth() - insets.right - ICON_PADDING_RIGHT - realIcon.getIconWidth();
					int y = (menuItem.getHeight() - realIcon.getIconHeight()) / 2;
					realIcon.paintIcon(c, g, x, y);
				}
				menuItem.setIcon(realIcon);
			}
		});
		return themeItem;
	}


	private static class ThemeColorIcon implements Icon
	{
		private final Color background;
		private final Color foreground;
		private final Color accent;
		private final int size = 10;
		private final int gap = 2;

		public ThemeColorIcon(Color background, Color foreground, Color accent)
		{
			this.background = background;
			this.foreground = foreground;
			this.accent = accent;
		}

		@Override
		public int getIconWidth()
		{
			return 3 * size + 2 * gap;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2 = (Graphics2D) g.create();

			g2.setColor(background);
			g2.fillRect(x, y, size, size);

			g2.setColor(foreground);
			g2.fillRect(x + size + gap, y, size, size);

			g2.setColor(accent);
			g2.fillRect(x + 2 * (size + gap), y, size, size);

			g2.dispose();
		}
	}
}
