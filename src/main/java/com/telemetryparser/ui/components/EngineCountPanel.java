package com.telemetryparser.ui.components;

import com.telemetryparser.core.fixed.CVEngineData;
import com.telemetryparser.core.fixed.EngineState;
import com.telemetryparser.util.EngineLocation;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Map;
import javax.swing.JPanel;

public class EngineCountPanel extends JPanel
{
	private final EngineDebugPanel parent;

	public EngineCountPanel(EngineDebugPanel parent)
	{
		super();
		this.parent = parent;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		g.setFont(g.getFont().deriveFont(10.0f));

		Map<Integer, EngineLocation> engineMap = parent.engineMap;
		if (engineMap == null || engineMap.isEmpty())
		{
			return;
		}

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

		for (EngineLocation location : engineMap.values())
		{
			Point middle = location.middle();
			int r = location.radius();

			int left   = middle.x - r;
			int right  = middle.x + r;
			int top    = middle.y - r;
			int bottom = middle.y + r;

			if (left   < minX) minX = left;
			if (right  > maxX) maxX = right;
			if (top    < minY) minY = top;
			if (bottom > maxY) maxY = bottom;
		}

		int boundingWidth  = maxX - minX;
		int boundingHeight = maxY - minY;

		if (boundingWidth <= 0 || boundingHeight <= 0)
		{
			return;
		}

		int panelWidth = getWidth();
		int panelHeight = getHeight();

		double scaleX = (double) panelWidth / boundingWidth;
		double scaleY = (double) panelHeight / boundingHeight;

		double scale = Math.min(scaleX, scaleY);

		double scaledWidth = boundingWidth * scale;
		double scaledHeight = boundingHeight * scale;

		double offsetX = (panelWidth - scaledWidth) / 2.0;
		double offsetY = (panelHeight - scaledHeight) / 2.0;

		Graphics2D g2d = (Graphics2D) g.create();
		try
		{
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			FontMetrics fm = g2d.getFontMetrics();
			int ascent = fm.getAscent();
			int descent = fm.getDescent();

			CVEngineData engineData = parent.getEngineStates();
			if(engineData != null)
			{
				for(Map.Entry<Integer, EngineState> entry : engineData.engineStates().entrySet())
				{
					Integer engineNumber = entry.getKey();
					EngineLocation location = entry.getValue().engineLocation();
					Point middle = location.middle();
					int r = location.radius();

					double cx = offsetX + (middle.x - minX) * scale;
					double cy = offsetY + (middle.y - minY) * scale;
					double scaledRadius = r * scale;

					boolean active = entry.getValue().active();

					g2d.setColor(active ? Color.GREEN : Color.RED);

					int ovalX = (int) Math.round (cx - scaledRadius);
					int ovalY = (int) Math.round(cy - scaledRadius);
					int ovalW = (int) Math.round(scaledRadius * 2);
					int ovalH = (int) Math.round(scaledRadius * 2);
					g2d.fillOval(ovalX, ovalY, ovalW, ovalH);

					String text = engineNumber.toString();
					int textWidth = fm.stringWidth(text);

					float textX = (float) (cx - textWidth / 2.0);
					float textY = (float) (cy + (ascent - descent) / 2.0);

					g2d.setColor(active ? Color.BLACK : Color.WHITE);
					g2d.drawString(text, textX, textY);
				}
			}

		}
		finally
		{
			g2d.dispose();
		}
	}
}
