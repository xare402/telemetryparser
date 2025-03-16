package com.telemetryparser.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel
{
	private BufferedImage image;
	private final double scale;
	private Double lineOrientation = null;
	private Color lineColor = Color.GREEN;

	public ImagePanel()
	{
		this(null, 0.0);
	}

	public ImagePanel(BufferedImage image, double scale)
	{
		super();
		this.image = image;
		this.scale = scale;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		if (image != null)
		{
			double currentScale;
			if (scale == 0)
			{
				int panelWidth = getWidth();
				int panelHeight = getHeight();

				double xRatio = panelWidth / (double) image.getWidth();
				double yRatio = panelHeight / (double) image.getHeight();

				currentScale = Math.min(xRatio, yRatio);
			}
			else
			{
				currentScale = scale;
			}

			int newWidth = (int) (image.getWidth() * currentScale);
			int newHeight = (int) (image.getHeight() * currentScale);

			int x = (getWidth() - newWidth) / 2;
			int y = (getHeight() - newHeight) / 2;

			g.drawImage(image, x, y, newWidth, newHeight, null);

			if (lineOrientation != null)
			{
				double angle = Math.toRadians(lineOrientation);

				int centerX = x + (newWidth / 2);
				int centerY = y + (newHeight / 2);

				double lineLength = Math.hypot(newWidth, newHeight);

				int x1 = (int) (centerX - lineLength * Math.sin(angle));
				int y1 = (int) (centerY - lineLength * Math.cos(angle));
				int x2 = (int) (centerX + lineLength * Math.sin(angle));
				int y2 = (int) (centerY + lineLength * Math.cos(angle));

				g.setColor(lineColor);
				g.drawLine(x1, y1, x2, y2);

			}
		}
	}

	public void setImage(BufferedImage newImage)
	{
		this.image = newImage;
		repaint();
	}

	public void setLineOrientation(Double orientation)
	{
		if(orientation != null)
		{
			double originalAngle = orientation + 180;
			if (originalAngle < 0)
			{
				originalAngle += 360;
			}
			else if (originalAngle >= 360)
			{
				originalAngle -= 360;
			}
			originalAngle *= -1;
			this.lineOrientation = originalAngle;
			repaint();
		}
	}

	public void setLineColor(Color color)
	{
		lineColor = color;
	}
}
