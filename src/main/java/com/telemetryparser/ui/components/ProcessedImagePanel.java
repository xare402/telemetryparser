package com.telemetryparser.ui.components;

import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.UIManager;

public class ProcessedImagePanel extends ImagePanel
{
	private String ocrLabel = "No text";

	public void setOcrLabel(String text)
	{
		if (text == null || text.isEmpty())
		{
			text = "No text";
		}
		ocrLabel = text;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		drawCenteredText(g, ocrLabel, getWidth());
	}

	private void drawCenteredText(Graphics g, String text, int width)
	{
		int accent = 5;
		int margin = 9;
		FontMetrics fm = g.getFontMetrics();
		int textWidth = fm.stringWidth(text);
		int textHeight = fm.getHeight();

		int boxWidth = Math.max(textWidth + margin, 50);

		int x = (width - boxWidth) / 2;
		int textX = x + (boxWidth - textWidth) / 2;
		int textY = accent + fm.getAscent() - 1;

		g.setColor(UIManager.getColor("Label.background"));
		g.fillRoundRect(x, accent, boxWidth, textHeight, 5, 5);

		g.setColor(UIManager.getColor("Component.accentColor"));
		g.drawRoundRect(x, accent, boxWidth, textHeight, 5, 5);

		g.setColor(UIManager.getColor("Label.foreground"));
		g.drawString(text, textX, textY);
	}
}
