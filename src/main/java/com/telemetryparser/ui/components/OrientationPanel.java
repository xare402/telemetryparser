package com.telemetryparser.ui.components;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.fixed.CVOrientationResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class OrientationPanel extends JPanel
{
	private final ImagePanel originalPanel;
	private final ImagePanel processedPanel;
	private final ImagePanel finalProcessedPanel;
	private final JLabel orientationLabel = new JLabel();
	private final JLabel eigenValueLabel = new JLabel();
	private final JLabel totalLabel = new JLabel();

	public OrientationPanel(Parameter parameter)
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(parameter.name));

		originalPanel = new ImagePanel();
		processedPanel = new ImagePanel();
		finalProcessedPanel = new ImagePanel();

		JPanel centerPanel = new JPanel(new GridLayout(0, 2));
		centerPanel.add(originalPanel);
		centerPanel.add(processedPanel);
		centerPanel.add(finalProcessedPanel);

		add(centerPanel, BorderLayout.CENTER);

		JPanel labelPanel = new JPanel();

		labelPanel.add(orientationLabel);
		labelPanel.add(eigenValueLabel);
		labelPanel.add(totalLabel);
		add(labelPanel, BorderLayout.SOUTH);
	}

	public void setImage(BufferedImage image)
	{
		originalPanel.setImage(image);
	}

	public void setProcessedImage(CVOrientationResult result, double orientationStrength, double orientationSymmetry)
	{
		processedPanel.setImage(result.getProcessedImage());

		finalProcessedPanel.setImage(result.getFilteredImage());

		double value = ((int)(result.getOrientationStrength() * 10))/10d;
		double total = ((int)(result.getOrientationSymmetry() * 10))/10d;

		boolean strengthAcceptable = result.getOrientationStrength() >= orientationStrength;
		boolean symmetryAcceptable = result.getOrientationSymmetry() >= orientationSymmetry;


		processedPanel.setLineColor((strengthAcceptable && symmetryAcceptable) ? Color.GREEN : Color.RED);
		processedPanel.setLineOrientation(result.getComputedOrientation());



		orientationLabel.setText("Angle: " + result.getOrientation());
		eigenValueLabel.setForeground(strengthAcceptable ? Color.GREEN : Color.RED);
		totalLabel.setForeground(symmetryAcceptable ? Color.GREEN : Color.RED);
		eigenValueLabel.setText("Correlation: " + value);
		totalLabel.setText("Symmetry: " + total);

		repaint();
	}
}