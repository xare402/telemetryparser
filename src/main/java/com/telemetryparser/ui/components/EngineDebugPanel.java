package com.telemetryparser.ui.components;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.fixed.CVEngineData;
import com.telemetryparser.util.EngineLocation;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class EngineDebugPanel extends JPanel
{
	private final ImagePanel originalImagePanel;
	private final ImagePanel processedImagePanel;
	private final JLabel engineLabel;
	public Map<Integer, EngineLocation> engineMap = null;
	private BufferedImage originalImage;
	private BufferedImage preprocessedImage;
	private CVEngineData engineStates = null;

	public EngineDebugPanel(Parameter parameter)
	{
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(parameter.name));
		setLayout(new GridLayout(1, 2));

		engineLabel = new JLabel("Engines: 0", SwingConstants.CENTER);

		JPanel gridPanel = new JPanel(new GridLayout(2, 1));

		originalImagePanel = new ImagePanel(originalImage, 0);
		processedImagePanel = new ImagePanel(preprocessedImage, 0);

		gridPanel.add(originalImagePanel);
		gridPanel.add(processedImagePanel);

		add(gridPanel);
		EngineCountPanel engineCountPanel = new EngineCountPanel(this);
		add(engineCountPanel);
	}

	public void setImage(BufferedImage image)
	{
		this.originalImage = image;

		originalImagePanel.setImage(image);

	}

	public void setProcessedImage(BufferedImage image)
	{
		this.preprocessedImage = image;
		processedImagePanel.setImage(image);
	}

	public void enginesChanged(Map<Integer, EngineLocation> engineLocationMap)
	{
		this.engineMap = engineLocationMap;
		if (engineMap != null)
		{
			engineLabel.setText("Engines: " + engineMap.size());
			repaint();
		}
	}

	public void setEngineStates(CVEngineData engineStates)
	{
		this.engineStates = engineStates;
		repaint();
	}

	public CVEngineData getEngineStates()
	{
		return engineStates;
	}
}