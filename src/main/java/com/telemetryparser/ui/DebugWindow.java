package com.telemetryparser.ui;

import com.telemetryparser.core.Parameter;
import static com.telemetryparser.core.Parameter.STAGE_1_ENGINES;
import static com.telemetryparser.core.Parameter.STAGE_1_ORIENTATION;
import static com.telemetryparser.core.Parameter.STAGE_2_ENGINES;
import static com.telemetryparser.core.Parameter.STAGE_2_ORIENTATION;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.core.fixed.CVEngineResult;
import com.telemetryparser.core.fixed.CVFuelResult;
import com.telemetryparser.core.fixed.CVOrientationResult;
import com.telemetryparser.core.fixed.CVResult;
import com.telemetryparser.core.fixed.CVTextResult;
import com.telemetryparser.ui.components.ROIViewer;
import com.telemetryparser.settings.Settings;
import com.telemetryparser.ui.components.AdaptiveGridPanel;
import com.telemetryparser.ui.components.EngineDebugPanel;
import com.telemetryparser.ui.components.FuelPanel;
import com.telemetryparser.ui.components.slider.NamedScaledSlider;
import com.telemetryparser.ui.components.OrientationPanel;
import com.telemetryparser.ui.components.slider.SliderSetting;
import com.telemetryparser.ui.icon.Icon;
import com.telemetryparser.ui.icon.IconManager;
import com.telemetryparser.util.EngineLocation;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class DebugWindow extends JFrame
{
	private final Map<Parameter, ROIViewer> roiViewerMap = new LinkedHashMap<>();
	private static final Map<SliderSetting, NamedScaledSlider> sliderMap = new LinkedHashMap<>();

	static
	{
		for (SliderSetting setting : SliderSetting.values())
		{
			NamedScaledSlider slider = setting.createSlider();
			slider.setResetButton(IconManager.get(Icon.RESET));
			Double sliderValue = Settings.getPropertyAsDouble("sliderSetting", setting.getDisplayName());
			if(sliderValue != null)
			{
				slider.setValue(sliderValue);
			}
			slider.addChangeListener(cl -> settingChanged());
			sliderMap.put(setting, slider);
		}
	}

	private static void settingChanged()
	{
		for(SliderSetting setting : SliderSetting.values())
		{
			Settings.setProperty("sliderSetting", setting.getDisplayName(), String.valueOf(getSettingValue(setting)));
		}
	}

	private final Map<Parameter, FuelPanel> fuelPanels = new HashMap<>();
	private final Map<Parameter, EngineDebugPanel> enginePanels = new HashMap<>();

	private final Map<Parameter, OrientationPanel> orientationPanels = new HashMap<>();

	public DebugWindow(MainWindow parent)
	{
		super("Debug Window");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		AdaptiveGridPanel adaptiveGridPanel = new AdaptiveGridPanel();

		for (Parameter parameter : Parameter.values())
		{
			if (parameter.type == ParameterType.FUEL)
			{
				JPanel fuelPanel = createFuelPanel(parameter);
				adaptiveGridPanel.addComponent(parameter.name, fuelPanel);
			}
			else if(parameter.type == ParameterType.TIME || parameter.type == ParameterType.SPEED || parameter.type == ParameterType.ALTITUDE)
			{
				ROIViewer roiViewer = new ROIViewer(parameter.name);
				roiViewerMap.put(parameter, roiViewer);
				adaptiveGridPanel.addComponent(parameter.name, roiViewer);
			}
		}


		JPanel eastPanel = new JPanel(new BorderLayout());
		eastPanel.setPreferredSize(new Dimension(250, 0));

		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridLayout(0, 1));

		for(NamedScaledSlider slider : sliderMap.values())
		{
			sliderPanel.add(slider);
		}

		eastPanel.add(sliderPanel, BorderLayout.NORTH);

		JPanel resetButtonPanel = new JPanel(new BorderLayout());

		JButton resetButton = new JButton("Reset All");
		resetButton.addActionListener(al ->
		{
			for (NamedScaledSlider slider : sliderMap.values())
			{
				slider.resetToDefaultValue();
			}
		});

		resetButtonPanel.add(resetButton, BorderLayout.WEST);

		eastPanel.add(resetButtonPanel, BorderLayout.SOUTH);


		adaptiveGridPanel.addComponent("Stage 1 Engines", createEnginePanel(STAGE_1_ENGINES));
		adaptiveGridPanel.addComponent("Stage 2 Engines", createEnginePanel(STAGE_2_ENGINES));
		adaptiveGridPanel.addComponent("Stage 1 Orientation", createOrientationPanel(STAGE_1_ORIENTATION));
		adaptiveGridPanel.addComponent("Stage 2 Orientation", createOrientationPanel(STAGE_2_ORIENTATION));


		add(adaptiveGridPanel, BorderLayout.CENTER);
		add(eastPanel, BorderLayout.EAST);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				parent.closeDebug();
			}
		});

		pack();
		setSize(1000, 600);
		setLocationRelativeTo(null);
	}


	private JComponent createOrientationPanel(Parameter parameter)
	{
		OrientationPanel panel = new OrientationPanel(parameter);
		orientationPanels.put(parameter, panel);
		return panel;
	}

	private JComponent createEnginePanel(Parameter parameter)
	{
		EngineDebugPanel panel = new EngineDebugPanel(parameter);
		enginePanels.put(parameter, panel);
		return panel;
	}

	private JPanel createFuelPanel(Parameter parameter)
	{
		FuelPanel panel = new FuelPanel(parameter);
		fuelPanels.put(parameter, panel);
		return panel;
	}


	public static double getSettingValue(SliderSetting setting)
	{
		return sliderMap.get(setting).getScaledValue();
	}

	public void setEngineMap(Parameter parameter, Map<Integer, EngineLocation> engineMap)
	{
		EngineDebugPanel panel = enginePanels.get(parameter);
		if (panel != null)
		{
			panel.enginesChanged(engineMap);
		}
	}

	public void updateFrameData(Map<Parameter, CVResult> results)
	{
		for(Parameter parameter : results.keySet())
		{
			switch(results.get(parameter))
			{
				case CVTextResult textResult -> roiViewerMap.get(parameter).setImagesAndText(textResult);
				case CVFuelResult fuelResult -> fuelPanels.get(parameter).updateFuelResult(fuelResult);
				case CVEngineResult engineResult ->
				{
					enginePanels.get(parameter).setImage(engineResult.getOriginalImage());
					enginePanels.get(parameter).setProcessedImage(engineResult.getProcessedImage());
					enginePanels.get(parameter).setEngineStates(engineResult.getEngineStates());
				}
				case CVOrientationResult orientationResult ->
				{
					orientationPanels.get(parameter).setImage(orientationResult.getOriginalImage());
					orientationPanels.get(parameter).setProcessedImage(orientationResult, getSettingValue(SliderSetting.ORIENTATION_STRENGTH), getSettingValue(SliderSetting.ORIENTATION_SYMMETRY));
				}

				default -> {}
			}
		}
	}
}
