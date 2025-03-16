package com.telemetryparser.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

@SuppressWarnings("UnusedDeclaration")
public class CircularStatDisplay extends JComponent
{

	private String text;
	private String altText = "";
	private Color incompleteColor = null;
	private Color completeColor = null;
	private Color textColor = null;
	private Color outlineColor = null;

	private Color deriveColor(Color base, int opacity)
	{
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), opacity);
	}

	private float start = 0.75f;

	private float completed = 0.0f;

	private float outerRadius = 0.95f;

	private float innerRadius = 0.82f;

	public CircularStatDisplay(String text)
	{
		setPreferredSize(new Dimension(50, 50));
		this.text = text;
	}

	public void setText(String text)
	{
		this.text = text;
		repaint();
	}

	public void setAltText(String altText)
	{
		this.altText = altText;
		repaint();
	}

	public void setIncompleteColor(Color incomplete)
	{
		this.incompleteColor = incomplete;
		repaint();
	}

	public void setCompleteColor(Color complete)
	{
		this.completeColor = complete;
		repaint();
	}

	public void setTextColor(Color text)
	{
		this.textColor = text;
		repaint();
	}

	public void setOutlineColor(Color outline)
	{
		this.outlineColor = outline;
		repaint();
	}

	public void setStart(float start)
	{
		this.start = start % 1.0f;
		if (this.start < 0f)
		{
			this.start += 1.0f;
		}
		repaint();
	}

	public void setCompleted(float completed)
	{
		if (completed < 0f)
		{
			completed = 0f;
		}
		else if (completed > 1f)
		{
			completed = 1f;
		}
		this.completed = completed;
		repaint();
	}


	public void setRadius(float r)
	{
		this.outerRadius = Math.max(Math.min(r, 1.0f), 0.0f);
		repaint();
	}

	public void setInnerRadius(float r)
	{
		this.innerRadius = Math.max(Math.min(r, 1.0f), 0.0f);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Color incompleteColorToUse = incompleteColor;
		Color completeColorToUse = completeColor;
		Color textColorToUse = textColor;
		Color outlineColorToUse = outlineColor;

		if (incompleteColor == null)
		{
			incompleteColorToUse = UIManager.getColor("Label.background");
		}

		if (completeColor == null)
		{
			completeColorToUse = deriveColor(UIManager.getColor("Component.accentColor"), 220);
		}

		if (textColor == null)
		{
			textColorToUse = UIManager.getColor("Label.foreground");
		}

		if (outlineColor == null)
		{
			outlineColorToUse = deriveColor(UIManager.getColor("Label.foreground"), 150);
		}

		int w = getWidth();
		int h = getHeight();
		int size = Math.min(w, h);

		int cx = w / 2;
		int cy = h / 2;

		float outerR = outerRadius * (size / 2.0f);
		float innerR = innerRadius * (size / 2.0f);

		if (innerR > outerR)
		{
			float tmp = outerR;
			outerR = innerR;
			innerR = tmp;
		}

		float startAngle = -360f * start;
		float extentComplete = 360f * completed;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		float topLeftOuterX = cx - outerR;
		float topLeftOuterY = cy - outerR;
		float outerDiameter = outerR * 2;

		Area donut = new Area(new Ellipse2D.Float(topLeftOuterX, topLeftOuterY, outerDiameter, outerDiameter));
		donut.subtract(new Area(new Ellipse2D.Float(cx - innerR, cy - innerR, innerR * 2, innerR * 2)));

		g2.setClip(donut);

		g2.setColor(incompleteColorToUse);
		g2.fillArc((int) topLeftOuterX, (int) topLeftOuterY, (int) outerDiameter, (int) outerDiameter, (int) startAngle, 360);

		g2.setColor(completeColorToUse);
		g2.fillArc((int) topLeftOuterX, (int) topLeftOuterY, (int) outerDiameter, (int) outerDiameter, (int) startAngle, (int) extentComplete);

		g2.setClip(null);

		g2.setColor(outlineColorToUse);
		g2.draw(new Ellipse2D.Float(topLeftOuterX, topLeftOuterY, outerDiameter, outerDiameter));
		g2.draw(new Ellipse2D.Float(cx - innerR, cy - innerR, innerR * 2, innerR * 2));

		if ((text != null && !text.isEmpty()) || (altText != null && !altText.isEmpty()))
		{
			g2.setColor(textColorToUse);
			FontMetrics fm = g2.getFontMetrics();

			boolean hasAltText = (altText != null && !altText.isEmpty());
			int lineCount = 0;
			if (text != null && !text.isEmpty())
			{
				lineCount++;
			}
			if (hasAltText)
			{
				lineCount++;
			}

			int lineSpacing = 2;

			int totalTextHeight = lineCount * fm.getHeight() + (lineCount - 1) * lineSpacing;

			int baseline = cy - totalTextHeight / 2 + fm.getAscent();

			if (text != null && !text.isEmpty())
			{
				int textWidth = fm.stringWidth(text);
				int textX = cx - textWidth / 2;
				g2.drawString(text, textX, baseline);

				if (hasAltText)
				{
					baseline += fm.getHeight() + lineSpacing;
				}
			}

			if (hasAltText)
			{
				int altTextWidth = fm.stringWidth(altText);
				int altTextX = cx - altTextWidth / 2;
				g2.drawString(altText, altTextX, baseline);
			}
		}

		g2.dispose();
	}
}
