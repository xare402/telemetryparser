package com.telemetryparser.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("UnusedDeclaration")
public class SimpleLineChart extends JPanel
{
	private static final int MARGIN = 3;
	private final Map<Integer, Integer> data = new TreeMap<>();
	private Color dataColor = null;
	private Color boxColor = null;
	private Color bgColor = null;

	private Color deriveColor(Color base, int opacity)
	{
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), opacity);
	}

	public SimpleLineChart(String name)
	{
		setToolTipText(name);
		setBackground(bgColor);
	}

	public void addData(int x, int y)
	{
		data.put(x, y);
		repaint();
	}

	public void clearData()
	{
		data.clear();
		repaint();
	}

	public void setDataColor(Color color)
	{
		this.dataColor = color;
		repaint();
	}

	public void setBackgroundColor(Color color)
	{
		this.bgColor = color;
		repaint();
	}

	public void setBoxColor(Color color)
	{
		this.boxColor = color;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Color boxColorToUse = boxColor;
		Color dataColorToUse = dataColor;

		if (boxColor == null)
		{
			boxColorToUse = deriveColor(UIManager.getColor("Label.foreground"), 150);
		}
		if (dataColor == null)
		{
			dataColorToUse = deriveColor(UIManager.getColor("Component.accentColor"), 230);
		}

		int width = getWidth() - 2 * MARGIN - 2;
		int height = getHeight() - 2 * MARGIN - 2;

		if (width <= 0 || height <= 0)
		{
			return;
		}

		Graphics2D g2 = (Graphics2D) g.create();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(boxColorToUse);
		g2.drawRect(MARGIN-1, MARGIN-1, width+2, height+2);

		if (data.isEmpty())
		{
			g2.dispose();
			return;
		}

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (Map.Entry<Integer, Integer> entry : data.entrySet())
		{
			int x = entry.getKey();
			int y = entry.getValue();
			if (x < minX)
			{
				minX = x;
			}
			if (x > maxX)
			{
				maxX = x;
			}
			if (y < minY)
			{
				minY = y;
			}
			if (y > maxY)
			{
				maxY = y;
			}
		}

		if (minX == maxX)
		{
			minX = 0;
			maxX = 1;
		}
		if (minY == maxY)
		{
			minY = 0;
			maxY = 1;
		}

		g2.setColor(dataColorToUse);

		Integer prevX = null, prevY = null;
		for (Map.Entry<Integer, Integer> entry : data.entrySet())
		{
			int xVal = entry.getKey();
			int yVal = entry.getValue();

			double xPixel = MARGIN + (double) (xVal - minX) / (maxX - minX) * width;
			double yPixel = MARGIN + height - (double) (yVal - minY) / (maxY - minY) * height;

			if (prevX != null)
			{
				double prevXPixel = MARGIN + (double) (prevX - minX) / (maxX - minX) * width;
				double prevYPixel = MARGIN + height - (double) (prevY - minY) / (maxY - minY) * height;

				g2.drawLine((int) prevXPixel, (int) prevYPixel, (int) xPixel, (int) yPixel);
			}

			prevX = xVal;
			prevY = yVal;
		}

		g2.dispose();
	}
}
