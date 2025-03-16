package com.telemetryparser.ui.components;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.fixed.CVFuelResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class FuelPanel extends JPanel
{
	private static final int SCALE_FACTOR = 1;
	private static final int BAR_HEIGHT = 10;
	private static final int V_GAP = 5;
	private static final int MIDDLE_COUNT = 4;
	private final JLabel infoLabel;
	private BufferedImage topScaled;
	private BufferedImage bottomScaled;
	private boolean[] columnsInRange;
	private int totalColumns;

	public FuelPanel(Parameter parameter)
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(parameter.name));

		JPanel drawingPanel = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (topScaled == null || bottomScaled == null)
				{
					return;
				}

				int yPos = 0;

				g.drawImage(topScaled, 0, yPos, null);
				yPos += topScaled.getHeight() + V_GAP;

				g.drawImage(bottomScaled, 0, yPos, null);
				yPos += bottomScaled.getHeight() + V_GAP;

				if (columnsInRange != null && totalColumns > 0)
				{
					boolean foundBlock = false;
					boolean endedBlock = false;

					for (int x = totalColumns - 1; x >= 0; x--)
					{
						Color barColor;
						if (endedBlock)
						{
							barColor = Color.GREEN;
						}
						else if (!foundBlock)
						{
							if (columnsInRange[x])
							{
								foundBlock = true;
								barColor = Color.RED;
							}
							else
							{
								barColor = Color.GREEN;
							}
						}
						else
						{
							if (!columnsInRange[x])
							{
								endedBlock = true;
								barColor = Color.GREEN;
							}
							else
							{
								barColor = Color.RED;
							}
						}

						int barX = x * SCALE_FACTOR;
						g.setColor(barColor);
						for (int dx = 0; dx < SCALE_FACTOR; dx++)
						{
							for (int dy = 0; dy < BAR_HEIGHT; dy++)
							{
								g.drawLine(barX + dx, yPos + dy, barX + dx, yPos + dy);
							}
						}
					}
				}
			}

		};

		infoLabel = new JLabel("No image yet");
		infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

		add(drawingPanel, BorderLayout.CENTER);
		add(infoLabel, BorderLayout.SOUTH);
	}

	private static Color alphaBlend(Color original, Color overlay, float alpha)
	{
		float r = original.getRed() * (1 - alpha) + overlay.getRed() * alpha;
		float g = original.getGreen() * (1 - alpha) + overlay.getGreen() * alpha;
		float b = original.getBlue() * (1 - alpha) + overlay.getBlue() * alpha;

		return new Color(clamp(r), clamp(g), clamp(b));
	}

	private static int clamp(float val)
	{
		return Math.max(0, Math.min(Math.round(val), 255));
	}

	public void updateFuelResult(CVFuelResult result)
	{
		BufferedImage image = result.getOriginalImage();
		int columnsWithinRange;
		if (image == null)
		{
			topScaled = null;
			bottomScaled = null;
			columnsInRange = null;
			totalColumns = 0;
			infoLabel.setText("No image");
			repaint();
			return;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		totalColumns = width;

		columnsWithinRange = result.getColumnsInRange();

		topScaled = new BufferedImage(width * SCALE_FACTOR, height * SCALE_FACTOR, BufferedImage.TYPE_INT_RGB);
		bottomScaled = new BufferedImage(width * SCALE_FACTOR, height * SCALE_FACTOR, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int rgb = image.getRGB(x, y);
				Color orig = new Color(rgb, true);

				for (int dy = 0; dy < SCALE_FACTOR; dy++)
				{
					for (int dx2 = 0; dx2 < SCALE_FACTOR; dx2++)
					{
						topScaled.setRGB(x * SCALE_FACTOR + dx2, y * SCALE_FACTOR + dy, rgb);

						if (orig.getRed() < 70 && orig.getGreen() < 70 && orig.getBlue() < 70)
						{
							bottomScaled.setRGB(x * SCALE_FACTOR + dx2, y * SCALE_FACTOR + dy, rgb);
						}
						else
						{
							Color blended = alphaBlend(orig, Color.GREEN, 0.5f);
							bottomScaled.setRGB(x * SCALE_FACTOR + dx2, y * SCALE_FACTOR + dy, blended.getRGB());
						}
					}
				}
			}
		}

		double fraction = (totalColumns == 0) ? 0.0 : (double) columnsWithinRange / totalColumns;
		double flipped = 100.0 * (1.0 - fraction);
		String pct = new DecimalFormat("0.00").format(flipped);
		infoLabel.setText(columnsWithinRange + "/" + totalColumns + " (" + pct + "%)");
		repaint();
	}
}