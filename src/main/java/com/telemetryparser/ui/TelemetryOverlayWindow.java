package com.telemetryparser.ui;

import com.telemetryparser.core.Parameter;
import static com.telemetryparser.core.Parameter.*;
import com.telemetryparser.util.EngineLocation;
import com.telemetryparser.util.ROIRatios;
import com.telemetryparser.util.math.Polynomial;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JWindow;
import javax.swing.UIManager;

public class TelemetryOverlayWindow extends JWindow
{
	private final MainWindow mainWindow;
	private RegionSelectionCallback regionSelectionCallback;
	private boolean isDrawingCustomRegion = false;
	private int dragStartX;
	private int dragStartY;
	private int dragEndX;
	private int dragEndY;
	private String overlayName = "";
	private final SegmentManager segmentManager = new SegmentManager();

	public TelemetryOverlayWindow(MainWindow mainWindow)
	{
		super(mainWindow);
		this.mainWindow = mainWindow;
		setBackground(new Color(0, 0, 0, 0));
		setBounds(new Rectangle(0, 0, 0, 0));
		setVisible(false);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (isDrawingCustomRegion)
				{
					dragStartX = e.getX();
					dragStartY = e.getY();
					dragEndX = dragStartX;
					dragEndY = dragStartY;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (isDrawingCustomRegion)
				{
					dragEndX = e.getX();
					dragEndY = e.getY();
					computeAndNotifyROI();
					isDrawingCustomRegion = false;
					setBackground(new Color(0, 0, 0, 0));
				}
			}
		});

		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (isDrawingCustomRegion)
				{
					dragEndX = e.getX();
					dragEndY = e.getY();
					repaint();
				}
			}
		});
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		if (isDrawingCustomRegion)
		{
			drawDrag(g, w, h);
			drawDragRectangle(g);
		}
		else
		{
			if(overlayEnabled)
			{
				drawRegions(g, w, h);
			}
			if(additionalTelemetryEnabled)
			{
				drawTelemetry(g, w, h);
			}
		}
	}

	private void drawRegions(Graphics g, int w, int h)
	{
		Map<Parameter, ROIRatios> ratios = mainWindow.getRegions();
		for (Parameter parameter : ratios.keySet())
		{
			ROIRatios roiRatios = ratios.get(parameter);
			if (roiRatios != null)
			{
				drawROI(g, roiRatios, w, h);
				if(parameter == Parameter.STAGE_1_ENGINES)
				{
					drawEngines(g, roiRatios, w, h, mainWindow.getStage1Engines().values());
				}
				else if(parameter == Parameter.STAGE_2_ENGINES)
				{
					drawEngines(g, roiRatios, w, h, mainWindow.getStage2Engines().values());
				}
			}
		}
	}

	private void drawDrag(Graphics g, int panelWidth, int panelHeight)
	{
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		String text = "Begin dragging " + overlayName + " region";
		text =text.toUpperCase();
		g2d.setFont(new Font("DIN Engschrift Std", Font.PLAIN, 24));

		FontMetrics metrics = g2d.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		int textHeight = metrics.getHeight();

		int x = (panelWidth - textWidth) / 2;
		int y = (panelHeight - textHeight) / 2 + metrics.getAscent();

		int padding = 20;
		int boxX = x - padding;
		int boxY = y - metrics.getAscent() - padding / 2;
		int boxWidth = textWidth + (padding * 2);
		int boxHeight = textHeight + padding;

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
		g2d.setColor(UIManager.getColor("Label.background"));
		g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2d.setColor(UIManager.getColor("Component.accentColor"));
		g2d.setStroke(new BasicStroke(2));
		g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

		g2d.setColor(UIManager.getColor("Label.foreground"));
		g2d.drawString(text, x, y);
	}


	private void drawDragRectangle(Graphics g)
	{
		g.setColor(UIManager.getColor("Component.accentColor"));

		int x = Math.min(dragStartX, dragEndX);
		int y = Math.min(dragStartY, dragEndY);
		int width = Math.abs(dragEndX - dragStartX);
		int height = Math.abs(dragEndY - dragStartY);

		g.drawRect(x, y, width, height);
		g.setColor(new Color(0, 0, 0, 50));
		g.fillRect(x, y, width, height);
	}

	private void drawEngines(Graphics g, ROIRatios roi, int w, int h, Collection<EngineLocation> values)
	{
		int x = (int) Math.round(roi.xRatio * w);
		int y = (int) Math.round(roi.yRatio * h);

		Dimension videoDimension = mainWindow.getVideoDimensions();

		float xScale = (float) w/videoDimension.width;
		float yScale = (float) h/videoDimension.height;

		g.setColor(Color.GREEN);
		for(EngineLocation engineLocation : values)
		{
			int xOffset = (int) (xScale * engineLocation.middle().x);
			int yOffset = (int) (yScale * engineLocation.middle().y);
			g.fillOval(x+xOffset-2, y+yOffset-2, 5, 5);
		}
	}

	private void drawROI(Graphics g, ROIRatios roi, int panelWidth, int panelHeight)
	{
		int x = (int) Math.round(roi.xRatio * panelWidth);
		int y = (int) Math.round(roi.yRatio * panelHeight);
		int width = (int) Math.round(roi.widthRatio * panelWidth);
		int height = (int) Math.round(roi.heightRatio * panelHeight);

		g.setColor(Color.WHITE);
		g.drawRect(x - 1, y - 1, width + 2, height + 2);

		g.setColor(new Color(255, 255, 255, 30));
		g.fillRect(x-1, y-1, width+2, height+2);
	}

	public void beginDrawingRegion(Parameter parameter, RegionSelectionCallback callback)
	{
		this.overlayName = parameter.name;
		this.regionSelectionCallback = callback;
		setBackground(new Color(0, 0, 0, 100));
		isDrawingCustomRegion = true;
		setVisible(true);
		repaint();
	}

	private void computeAndNotifyROI()
	{
		int panelWidth = getWidth();
		int panelHeight = getHeight();

		int x = Math.min(dragStartX, dragEndX);
		int y = Math.min(dragStartY, dragEndY);
		int width = Math.abs(dragEndX - dragStartX);
		int height = Math.abs(dragEndY - dragStartY);

		dragStartX = 0;
		dragStartY = 0;
		dragEndX = 0;
		dragEndY = 0;
		overlayName = "";

		isDrawingCustomRegion = false;

		double xRatio = (double) x / (double) panelWidth;
		double yRatio = (double) y / (double) panelHeight;
		double widthRatio = (double) width / (double) panelWidth;
		double heightRatio = (double) height / (double) panelHeight;

		ROIRatios roi = new ROIRatios(xRatio, yRatio, widthRatio, heightRatio);

		if (regionSelectionCallback != null)
		{
			regionSelectionCallback.onRegionSelected(roi);
		}
	}

	Double lastTelemetryTime = null;

	public void updateTelemetryTime(double time)
	{
		lastTelemetryTime = time-9.8;
		repaint();
	}


	public void addNewTelemetry(Parameter parameter, double firstTime, double lastTime, Polynomial polynomial)
	{
		segmentManager.addSegment(parameter, firstTime, lastTime, polynomial);
	}

	public boolean additionalTelemetryEnabled = false;
	public boolean overlayEnabled = false;

	public void setOverlayEnabled(boolean selected)
	{
		overlayEnabled = selected;
		repaint();
	}

	public void setAdditionalTelemetryEnabled(boolean selected)
	{
		additionalTelemetryEnabled = selected;
		repaint();
	}

	private double xPosition = 0.5;
	private double yPosition = 0.5;
	private double scale = 0.8;
	public void setCustomValues(double xPosition, double yPosition, double scale)
	{
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.scale = scale;
		repaint();
	}

	public Double getStage1ComputedDownrange(double time)
	{
		return segmentManager.evaluateStage1HorizontalSpeedIntegral(null, time);
	}

	public Double getStage1ComputedAltitude(double time)
	{
		return segmentManager.evaluateIndefiniteIntegralOfDerivative(STAGE_1_ALTITUDE, null, time);
	}

	public Double getStage2ComputedDownrange(double time)
	{
		return segmentManager.evaluateIndefiniteIntegralFromMagnitudeAndVertical(STAGE_2_SPEED, STAGE_2_ALTITUDE, null, time);
	}

	public Double getStage2ComputedAltitude(double time)
	{
		return segmentManager.evaluateIndefiniteIntegralOfDerivative(STAGE_2_ALTITUDE, null, time);
	}

	private void drawTelemetry(Graphics g, int w, int h)
	{
		g.setFont(new Font("DIN Mittelschrift Std", Font.BOLD, 15));
		g.setColor(Color.WHITE);

		Double stage1SpeedHorizontal = segmentManager.getHorizontalSpeedFromMagnitudeAndAltitude(STAGE_1_SPEED, STAGE_1_ALTITUDE, lastTelemetryTime);
		String stage1SpeedHorizontalString = (stage1SpeedHorizontal != null) ? String.valueOf(stage1SpeedHorizontal.intValue()) : "";

		Double stage1SpeedVertical = segmentManager.evaluateDerivative(STAGE_1_ALTITUDE, lastTelemetryTime);
		String stage1SpeedVerticalString = (stage1SpeedVertical != null) ? String.valueOf((int)(stage1SpeedVertical*3600)) : "";

		Double stage2SpeedHorizontal = segmentManager.getHorizontalSpeedFromMagnitudeAndAltitude(STAGE_2_SPEED, STAGE_2_ALTITUDE, lastTelemetryTime);
		String stage2SpeedHorizontalString = (stage2SpeedHorizontal != null) ? String.valueOf(stage2SpeedHorizontal.intValue()) : "";

		Double stage2SpeedVertical = segmentManager.evaluateDerivative(STAGE_2_ALTITUDE, lastTelemetryTime);
		String stage2SpeedVerticalString = (stage2SpeedVertical != null) ? String.valueOf((int)(stage2SpeedVertical*3600)) : "";

		drawPositionedAndScaledText(g, w, h, .1172, .83, .5444, "SPEED (H)", false);
		drawPositionedAndScaledText(g, w, h, .1172, .81, .5444, "SPEED (V)", false);
		drawPositionedAndScaledText(g, w, h, .1172, .904, .5444, "RANGE", false);

		drawPositionedAndScaledText(g, w, h, .2370, .83, .47, "KM/H", false);
		drawPositionedAndScaledText(g, w, h, .2370, .81, .47, "KM/H", false);
		drawPositionedAndScaledText(g, w, h, .2370, .904, .47, "KM", false);

		drawPositionedAndScaledText(g, w, h, .732, .83, .5444, "SPEED (H)", false);
		drawPositionedAndScaledText(g, w, h, .732, .81, .5444, "SPEED (V)", false);
		drawPositionedAndScaledText(g, w, h, .732, .904, .5444, "RANGE", false);

		drawPositionedAndScaledText(g, w, h, .852, .83, .47, "KM/H", false);
		drawPositionedAndScaledText(g, w, h, .852, .81, .47, "KM/H", false);
		drawPositionedAndScaledText(g, w, h, .852, .904, .47, "KM", false);

		drawPositionedAndScaledText(g, w, h, .2305, .8270, .5444, stage1SpeedHorizontalString, true);
		drawPositionedAndScaledText(g, w, h, .2305, .8070, .5444, stage1SpeedVerticalString, true);

		drawPositionedAndScaledText(g, w, h, 0.8445, .8270, .5444, stage2SpeedHorizontalString, true);
		drawPositionedAndScaledText(g, w, h, 0.8445, .8070, .5444, stage2SpeedVerticalString, true);


		Double stage1Downrange = segmentManager.evaluateStage1HorizontalSpeedIntegral(null, lastTelemetryTime);
		String stage1DownrangeString = (stage1Downrange == null) ? "null" : String.valueOf(stage1Downrange.intValue());

		Double stage1AltitudeReconstructed = segmentManager.evaluateIndefiniteIntegralOfDerivative(STAGE_1_ALTITUDE, null, lastTelemetryTime);
		String stage1AltitudeReconstructedString = (stage1AltitudeReconstructed == null) ? "null" : String.valueOf(stage1AltitudeReconstructed.intValue());
		drawPositionedAndScaledText(g, w, h, .2305, 0.9030, .5444, stage1DownrangeString, true);


		Double stage1LOX = segmentManager.evaluate(STAGE_1_LOX, lastTelemetryTime);
		String stage1LOXString = (stage1LOX != null) ? stage1LOX.intValue() +"%" : "%";

		Double stage1CH4 = segmentManager.evaluate(STAGE_1_CH4, lastTelemetryTime);
		String stage1CH4String = (stage1CH4 != null) ? stage1CH4.intValue() +"%" : "%";

		drawPositionedAndScaledText(g, w, h, .2889, .927, .4, stage1LOXString, true);
		drawPositionedAndScaledText(g, w, h, .2889, .96, .4, stage1CH4String, true);

		Double stage2LOX = segmentManager.evaluate(STAGE_2_LOX, lastTelemetryTime);
		String stage2LOXString = (stage2LOX != null) ? stage2LOX.intValue() +"%" : "%";

		Double stage2CH4 = segmentManager.evaluate(STAGE_2_CH4, lastTelemetryTime);
		String stage2CH4String = (stage2CH4 != null) ? stage2CH4.intValue() +"%" : "%";

		drawPositionedAndScaledText(g, w, h, .9056, .927, .4, stage2LOXString, true);
		drawPositionedAndScaledText(g, w, h, .9056, .96, .4, stage2CH4String, true);

		Double stage1Orientation = segmentManager.evaluate(STAGE_1_ORIENTATION, lastTelemetryTime);
		String stage1OrientationString = (stage1Orientation != null) ? stage1Orientation.intValue()+"" : "";
		drawPositionedAndScaledText(g, w, h, .325, .83, .5444, stage1OrientationString, false);

		Double stage2Orientation = segmentManager.evaluate(STAGE_2_ORIENTATION, lastTelemetryTime);
		String stage2OrientationString = (stage2Orientation != null) ? stage2Orientation.intValue()+"" : "";
		drawPositionedAndScaledText(g, w, h, .647, .83, .5444, stage2OrientationString, false);


		Double stage1Engines = segmentManager.evaluate(STAGE_1_ENGINES, lastTelemetryTime);
		String stage1EnginesString = (stage1Engines != null) ? stage1Engines.intValue()+"" : "";
		drawPositionedAndScaledText(g, w, h, .075, .827, .5444, stage1EnginesString, false);


		Double stage1AltitudeReconstructed2 = segmentManager.getHorizontalSpeed(lastTelemetryTime);
		String stage1AltitudeReconstructedString2 = (stage1AltitudeReconstructed2 == null) ? "null" : String.valueOf(stage1AltitudeReconstructed2.intValue());
		//drawPositionedAndScaledText(g, w, h, xPosition, yPosition, scale, stage1AltitudeReconstructedString, true);
		drawPositionedAndScaledText(g, w, h, xPosition, yPosition, scale, stage1AltitudeReconstructedString2, true);

	}

	private void drawPositionedAndScaledText(Graphics g, int w, int h, double xPosition, double yPosition,
											 double additionalScale, String text, boolean isRightAligned)
	{
		double xRatio = w/1920.0;
		double yRatio = h/1080.0;

		Font font = new Font("DIN Mittelschrift Std", Font.BOLD, 48);
		FontMetrics metrics = g.getFontMetrics(font);
		int textWidth = metrics.stringWidth(text);
		int textHeight = metrics.getHeight();

		BufferedImage textImage = new BufferedImage(textWidth + 4, textHeight + 4, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = textImage.createGraphics();

		g2d.setFont(font);
		g2d.setColor(Color.WHITE);
		g2d.drawString(text, 2, metrics.getAscent() + 2);
		g2d.dispose();

		int scaledWidth = (int) (textWidth * xRatio * additionalScale);
		int scaledHeight = (int) (textHeight * yRatio * additionalScale);

		int x;
		if (isRightAligned)
		{
			x = (int) (1920*xPosition * xRatio) - scaledWidth;
		}
		else
		{
			x = (int) (1920*xPosition * xRatio);
		}

		int y = (int) (1080*yPosition * yRatio);

		g.drawImage(textImage, x, y, scaledWidth, scaledHeight, null);
	}

	public Double getStage1HorizontalSpeed(double v)
	{
		return segmentManager.getHorizontalSpeed(v);
	}

	public Double getStage1VerticalSpeed(double v)
	{
		return segmentManager.evaluateDerivative(STAGE_1_ALTITUDE, v);
	}
}
