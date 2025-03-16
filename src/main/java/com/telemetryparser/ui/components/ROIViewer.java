package com.telemetryparser.ui.components;

import com.telemetryparser.core.fixed.CVTextResult;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class ROIViewer extends JPanel
{
	private final ImagePanel originalPanel;
	private final ProcessedImagePanel processedPanel;

	public ROIViewer(String name)
	{
		super();
		setBorder(BorderFactory.createTitledBorder(name));
		setLayout(new GridLayout(2, 1));

		originalPanel = new ImagePanel();
		processedPanel = new ProcessedImagePanel();

		add(originalPanel);
		add(processedPanel);
	}

	public void setImagesAndText(CVTextResult cvTextResult)
	{
		BufferedImage originalImage = cvTextResult.getOriginalImage();
		BufferedImage processedImage = cvTextResult.getProcessedImage();
		String text = cvTextResult.getResult();
		if (text == null || text.isEmpty())
		{
			text = "No text";
		}
		String ocrLabel = text;

		originalPanel.setImage(originalImage);
		processedPanel.setImage(processedImage);
		processedPanel.setOcrLabel(ocrLabel);
		repaint();
	}
}
