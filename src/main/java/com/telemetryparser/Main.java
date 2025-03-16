package com.telemetryparser;

import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme;
import com.telemetryparser.ui.MainWindow;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main
{

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(new FlatGruvboxDarkHardIJTheme());
			SwingUtilities.invokeLater(MainWindow::new);
		}
		catch (Exception e)
		{
			System.out.println("Could not initialize application");
		}
	}
}
