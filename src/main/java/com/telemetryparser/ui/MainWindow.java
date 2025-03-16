package com.telemetryparser.ui;

import com.telemetryparser.core.Parameter;
import static com.telemetryparser.core.Parameter.*;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.core.fixed.FrameCompletedEvent;
import com.telemetryparser.core.fixed.FrameData;
import com.telemetryparser.core.fixed.FrameManager;
import com.telemetryparser.settings.Preset;
import com.telemetryparser.settings.Settings;
import com.telemetryparser.datatransformation.TransformData;
import com.telemetryparser.dataserver.DataServer;
import com.telemetryparser.dataserver.DataServerListener;
import com.telemetryparser.ui.theme.ThemeMenu;
import com.telemetryparser.ui.components.AdaptiveGridPanel;
import com.telemetryparser.ui.components.CircularStatDisplay;
import com.telemetryparser.ui.components.SimpleLineChart;
import com.telemetryparser.ui.videoplayer.VideoPlayer;
import com.telemetryparser.util.EngineData;
import com.telemetryparser.util.EngineLocation;
import com.telemetryparser.util.ImageUtil;
import static com.telemetryparser.util.ImageUtil.computeEngineBounds;
import static com.telemetryparser.util.ImageUtil.computeFuelRegion;
import com.telemetryparser.util.ROIRatios;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import javax.management.ObjectName;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;

public class MainWindow extends JFrame
{
	public static Map<Parameter, ROIRatios> regionMap = new HashMap<>();
	public static boolean debug = false;
	public static DebugWindow debugWindow = null;
	private final JTextField urlField = new JTextField();
	private final JComboBox<String> urlType = new JComboBox<>(new String[]{"X Broadcast URL", "Raw m3u8 URL", "Alternate Format"});
	private final TelemetryOverlayWindow overlayWindow;
	private final List<SimpleLineChart> charts = new ArrayList<>();
	private final SimpleLineChart stage1GeodesicChart = new SimpleLineChart("Stage 1");
	private final SimpleLineChart stage2GeodesicChart = new SimpleLineChart("Stage 2");
	private final JLabel serverConnectionsLabel = new JLabel("OFFLINE");
	private final JButton serverStartStop = new JButton("Start");
	private final FrameManager timelineManager;
	private final PresetPanel presetPanel = new PresetPanel(this);
	private final VideoPlayer videoPlayer = new VideoPlayer();
	private final DataServer dataServer = new DataServer();
	CircularStatDisplay ocrSpeed = new CircularStatDisplay("N/A");
	CircularStatDisplay ocrRate = new CircularStatDisplay("N/A");
	String lastSelection = null;
	private JCheckBoxMenuItem showDebugItem;
	private Map<Integer, EngineLocation> stage1EngineMap;
	private Map<Integer, EngineLocation> stage2EngineMap;
	private String downloadLocation = Settings.getProperty("yt", "yt-dl", "yt-dlp.exe");

	public MainWindow()
	{
		setJMenuBar(createMenuBar());
		setTitle("Live Telemetry Parser");
		setSize(1267, 675);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JPanel controlPanel = new JPanel(new BorderLayout());
		controlPanel.add(urlField, BorderLayout.CENTER);

		controlPanel.add(getSubControls(), BorderLayout.EAST);
		for (Parameter parameter : Parameter.values())
		{
			if (parameter.include)
			{
				charts.add(new SimpleLineChart(parameter.name));
			}
		}


		timelineManager = new FrameManager();
		timelineManager.addFrameCompletedListener(this::handleFrameCompleted);

		videoPlayer.addVideoPlayerFrameChangedListener((image, playerTime, systemTime) ->
		{
			timelineManager.queueFrame(new FrameData(image, playerTime, systemTime, getPreset()));
		});

		videoPlayer.addMediaDimensionChangedListener(this::handleOverlaySizing);
		videoPlayer.addTimeChangedListener(timelineManager::timeChanged);

		overlayWindow = new TelemetryOverlayWindow(this);
		overlayWindow.setVisible(true);
		timelineManager.addDataListener(overlayWindow::addNewTelemetry);
		addWindowListener(getWindowAdapter());

		dataServer.addDataServerListener(getDataServerListener());

		serverStartStop.addActionListener(al -> toggleServerState());

		AdaptiveGridPanel liveConnectionPanel = new AdaptiveGridPanel(0, 2);

		liveConnectionPanel.addComponent("OCR Rate", ocrRate);
		liveConnectionPanel.addComponent("OCR Speed", ocrSpeed);

		int index = 0;
		for (Parameter parameter : Parameter.values())
		{
			if (parameter.include)
			{
				liveConnectionPanel.addComponent(parameter.name, charts.get(index));
				if (parameter == STAGE_1_MIDDLE_ENGINES)
				{
					liveConnectionPanel.addComponent("", new JLabel());
				}
				index++;
			}
		}

		JPanel bottomRightPanel = new JPanel();
		bottomRightPanel.setLayout(new BorderLayout());
		bottomRightPanel.setPreferredSize(new Dimension(140, 0));

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Simple", liveConnectionPanel);
		AdaptiveGridPanel geodesicCharts = new AdaptiveGridPanel();
		geodesicCharts.setCols(1);
		geodesicCharts.addComponent("Stage 1", stage1GeodesicChart);
		geodesicCharts.addComponent("Stage 2", stage2GeodesicChart);
		tabbedPane.addTab("Advanced", geodesicCharts);

		bottomRightPanel.add(tabbedPane, BorderLayout.CENTER);

		JPanel mergedHolder = new JPanel();
		mergedHolder.setLayout(new BorderLayout());

		JPanel splitMergedBottom = new JPanel(new GridLayout(2, 1));

		JPanel dropDownPanel = new JPanel(new GridLayout(0, 2));
		dropDownPanel.setBorder(BorderFactory.createTitledBorder("Preset"));

		splitMergedBottom.add(getTopPanel());
		splitMergedBottom.add(getLiveDataPanel());

		mergedHolder.add(splitMergedBottom, BorderLayout.CENTER);

		JPanel rightSide = new JPanel();
		rightSide.setLayout(new BorderLayout());
		rightSide.add(mergedHolder, BorderLayout.SOUTH);

		rightSide.add(presetPanel, BorderLayout.NORTH);
		rightSide.add(bottomRightPanel, BorderLayout.CENTER);

		JPanel testPanel = new JPanel();
		testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
		testPanel.setPreferredSize(new Dimension(200, 0));

		final int MAX = 100000;
		JSlider xSlider = new JSlider(0, MAX, 0);
		JSlider ySlider = new JSlider(0, MAX, 0);
		JSlider scaleSlider = new JSlider(0, MAX, 0);

		xSlider.setSnapToTicks(true);
		xSlider.setMinorTickSpacing(1);
		ySlider.setSnapToTicks(true);
		ySlider.setMinorTickSpacing(1);
		scaleSlider.setSnapToTicks(true);
		scaleSlider.setMinorTickSpacing(1);

		JTextField xTextField = new JTextField("0.0000");
		JTextField yTextField = new JTextField("0.0000");
		JTextField scaleTextField = new JTextField("0.0000");

		ActionListener textFieldActionListener = e ->
		{
			Object src = e.getSource();

			if (src == xTextField)
			{
				updateSliderFromText(xTextField, xSlider);
			}
			else if (src == yTextField)
			{
				updateSliderFromText(yTextField, ySlider);
			}
			else if (src == scaleTextField)
			{
				updateSliderFromText(scaleTextField, scaleSlider);
			}
		};
		xTextField.addActionListener(textFieldActionListener);
		yTextField.addActionListener(textFieldActionListener);
		scaleTextField.addActionListener(textFieldActionListener);

		ChangeListener sliderListener = e ->
		{
			double xVal = xSlider.getValue() / (double) MAX;
			double yVal = ySlider.getValue() / (double) MAX;
			double sVal = scaleSlider.getValue() / (double) MAX;

			xTextField.setText(String.format("%.4f", xVal));
			yTextField.setText(String.format("%.4f", yVal));
			scaleTextField.setText(String.format("%.4f", sVal));
			overlayWindow.setCustomValues(xVal, yVal, sVal);
		};

		xSlider.addChangeListener(sliderListener);
		ySlider.addChangeListener(sliderListener);
		scaleSlider.addChangeListener(sliderListener);

		testPanel.add(new JLabel("X Position:"));
		testPanel.add(xTextField);
		testPanel.add(xSlider);

		testPanel.add(new JLabel("Y Position:"));
		testPanel.add(yTextField);
		testPanel.add(ySlider);

		testPanel.add(new JLabel("Scale:"));
		testPanel.add(scaleTextField);
		testPanel.add(scaleSlider);


		getContentPane().setLayout(new BorderLayout());
		//getContentPane().add(testPanel, BorderLayout.WEST);
		getContentPane().add(controlPanel, BorderLayout.NORTH);
		getContentPane().add(videoPlayer, BorderLayout.CENTER);
		getContentPane().add(rightSide, BorderLayout.EAST);

		addComponentListener(getComponentAdapter());
		if (debug)
		{
			debugWindow.setVisible(true);
		}
		switchROIs();
		setVisible(true);
	}

	private static void updateSliderFromText(JTextField textField, JSlider slider)
	{
		String text = textField.getText().trim();
		double val;
		try
		{
			val = Double.parseDouble(text);
		}
		catch (NumberFormatException ex)
		{
			val = slider.getValue() / 100000.0;
		}

		if (val < 0.0)
		{
			val = 0.0;
		}
		if (val > 1.0)
		{
			val = 1.0;
		}

		int sliderVal = (int) Math.round(val * 100000);
		slider.setValue(sliderVal);
	}

	private JPanel getSubControls()
	{
		JPanel subControls = new JPanel();
		JButton loadButton = new JButton("Load URL");
		loadButton.addActionListener(e -> loadStreamAsync());
		subControls.setLayout(new BoxLayout(subControls, BoxLayout.X_AXIS));
		subControls.setPreferredSize(new Dimension(285, 0));
		subControls.add(urlType);
		subControls.add(loadButton);
		return subControls;
	}

	private JPanel getTopPanel()
	{
		JPanel topPanel = new JPanel();
		topPanel.setBorder(BorderFactory.createTitledBorder("Data"));
		topPanel.setLayout(new FlowLayout());
		JButton writeData = new JButton("Write Data");
		JButton clearData = new JButton("Clear Data");
		writeData.addActionListener(al ->
		{
			timelineManager.writeData();
		});
		clearData.addActionListener(al -> clearData());
		topPanel.add(writeData);
		topPanel.add(clearData);
		return topPanel;
	}

	private JPanel getLiveDataPanel()
	{
		JPanel liveDataPanel = new JPanel();
		liveDataPanel.setBorder(BorderFactory.createTitledBorder("Outgoing Data Server"));
		serverStartStop.setPreferredSize(new Dimension(90, 30));
		serverConnectionsLabel.setPreferredSize(new Dimension(90, 30));
		serverConnectionsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
		liveDataPanel.add(serverStartStop);
		liveDataPanel.add(serverConnectionsLabel);
		return liveDataPanel;
	}

	public void clearPreset()
	{
		regionMap.clear();
		stage1EngineMap.clear();
		stage2EngineMap.clear();
		Settings.deletePreset(presetPanel.getActivePreset());
		overlayWindow.repaint();
	}

	private ComponentAdapter getComponentAdapter()
	{
		return new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent evt)
			{
				handleOverlaySizing();
			}

			@Override
			public void componentMoved(ComponentEvent evt)
			{
				handleOverlaySizing();
			}
		};
	}

	private void handleOverlaySizing()
	{
		Dimension videoDimension = videoPlayer.getVideoDimensions();
		if (videoDimension != null)
		{
			Rectangle bounds = videoPlayer.videoSurface().getDrawDimension(videoDimension.width, videoDimension.height);
			Point panelOnScreen = videoPlayer.videoSurface().getLocationOnScreen();

			int x = panelOnScreen.x + bounds.x;
			int y = panelOnScreen.y + bounds.y;

			overlayWindow.setBounds(x, y, bounds.width, bounds.height);
		}
	}

	private void toggleServerState()
	{
		if (dataServer.isRunning())
		{
			serverStartStop.setText("Stopping...");
			dataServer.stop();
		}
		else
		{
			serverStartStop.setText("Starting...");
			dataServer.start();
		}
	}

	private DataServerListener getDataServerListener()
	{
		return new DataServerListener()
		{
			@Override
			public void serverStarted()
			{
				serverStartStop.setText("Stop");
				serverConnectionsLabel.setText("Connected: 0");
			}

			@Override
			public void serverStopped()
			{
				serverStartStop.setText("Start");
				serverConnectionsLabel.setText("OFFLINE");
			}

			@Override
			public void connectionsChanged(String type, String clientHost, int totalConnections)
			{
				serverConnectionsLabel.setText("Connected: " + totalConnections);
			}
		};
	}

	private void clearData()
	{
		timelineManager.reset();
		ocrRate.setText("N/A");
		ocrRate.setAltText("N/A");
		ocrRate.setCompleted(0f);
		ocrSpeed.setCompleted(0f);
		ocrSpeed.setText("N/A");
		for (SimpleLineChart chart : charts)
		{
			chart.clearData();
		}
	}

	private WindowAdapter getWindowAdapter()
	{
		return new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				overlayWindow.dispose();
			}
		};
	}

	private void handleFrameCompleted(FrameCompletedEvent fl)
	{
		if (debugWindow != null)
		{
			debugWindow.updateFrameData(fl.frameAnalysisResult().results());
		}
		try
		{
			overlayWindow.updateTelemetryTime(fl.time());
			try
			{
				Double xValue = overlayWindow.getStage1VerticalSpeed(fl.time() - 10);
				Double yValue = fl.time() - 10;
				if (xValue != null)
				{
					stage1GeodesicChart.addData(xValue.intValue(), yValue.intValue());
				}

				Double xValue3 = overlayWindow.getStage1HorizontalSpeed(fl.time() - 10);
				Double yValue3 = fl.time() - 10;
				if (xValue3 != null)
				{
					stage2GeodesicChart.addData(xValue3.intValue(), yValue3.intValue());
				}
				/*Double xValue1 = overlayWindow.getStage1ComputedDownrange(fl.time()-10);
				Double yValue1 = overlayWindow.getStage1ComputedAltitude(fl.time()-10);
				if(xValue1 != null && yValue1 != null)
				{
					stage1GeodesicChart.addData(xValue1.intValue(), yValue1.intValue());
					System.out.println("Adding: " + xValue1.intValue() + "," + yValue1.intValue());
				}

				Double xValue2 = overlayWindow.getStage2ComputedDownrange(fl.time()-10);
				Double yValue2 = overlayWindow.getStage2ComputedAltitude(fl.time()-10);
				if(xValue2 != null && yValue2 != null)
				{
					stage2GeodesicChart.addData(xValue2.intValue(), yValue2.intValue());
					System.out.println("Adding: " + xValue2.intValue() + "," + yValue2.intValue());
				}*/
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			ocrSpeed.setText(fl.elapsedMs() + "ms");
			ocrSpeed.setCompleted(fl.elapsedMs() / 50f);
			long successfulFrames = fl.totalFrameCount() - fl.flaggedFrameCount();
			ocrRate.setText(successfulFrames + "/" + fl.totalFrameCount());
			float successfulFrameRate = (float) successfulFrames / fl.totalFrameCount();
			ocrRate.setCompleted(successfulFrameRate);
			ocrRate.setAltText(String.format("%.2f", 100 * successfulFrameRate));
			if (fl.frameAnalysisResult().results() == null || fl.flagged())
			{
				return;
			}
			int time = fl.time().intValue();
			int index = 0;
			for (Parameter parameter : Parameter.values())
			{
				if (parameter.include)
				{
					try
					{
						charts.get(index).addData(time, fl.data().get(parameter).intValue());
					}
					catch (Exception ignored)
					{
					}
					index++;
				}
			}
		}
		catch (Exception ignored)
		{
		}
	}

	private void loadStreamAsync()
	{
		SwingWorker<String, Void> worker = new SwingWorker<>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				SwingUtilities.invokeLater(() -> videoPlayer.setText("Loading..."));

				String url = urlField.getText().trim();

				if (Objects.equals(urlType.getSelectedItem(), "Alternate Format"))
				{
					MRLDialog MRLDialog = new MRLDialog(MainWindow.this, url);
					url = MRLDialog.getResult();
				}
				else if (Objects.equals(urlType.getSelectedItem(), "X Broadcast URL"))
				{
					SwingUtilities.invokeLater(() -> videoPlayer.setText("Finding URL..."));

					ProcessBuilder processBuilder = new ProcessBuilder(getDLLocation(), "-g", url);
					Process process = processBuilder.start();

					try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream())))
					{
						String line;
						while ((line = stdOutReader.readLine()) != null)
						{
							url = line;
						}
					}
				}
				return url;
			}

			@Override
			protected void done()
			{
				try
				{
					String url = get();
					videoPlayer.loadStream(url);
					if (videoPlayer.isMediaValid())
					{
						handleOverlaySizing();
					}
				}
				catch (Exception e)
				{
					videoPlayer.setText("Failed to load URL");
				}
			}
		};
		worker.execute();
	}

	public void setROI(Parameter parameter)
	{
		if (videoPlayer.isPlaying())
		{
			videoPlayer.togglePlay();
		}
		overlayWindow.beginDrawingRegion(parameter, roi ->
		{
			if (parameter.type == ParameterType.FUEL)
			{
				SwingWorker<ROIRatios, Void> worker = getFuelBoundWorker(parameter, roi);
				worker.execute();
			}
			else if (parameter.type == ParameterType.ENGINES)
			{
				final ROIRatios originalRoi = roi;

				SwingWorker<EngineData, Void> worker = new SwingWorker<>()
				{
					@Override
					protected EngineData doInBackground()
					{
						return computeEngineROIs(originalRoi);
					}

					@Override
					protected void done()
					{
						try
						{
							EngineData engineData = get();
							if (engineData != null)
							{
								saveROIs(parameter, engineData.ratios());
								saveEngineData(parameter, engineData.engines());
								overlayWindow.repaint();
							}

						}
						catch (InterruptedException | ExecutionException e)
						{
							System.out.println("Could not compute " + parameter.type.name + " region.");
						}
					}
				};
				worker.execute();
			}
			else if (roi != null)
			{
				saveROIs(parameter, roi);
			}
			overlayWindow.repaint();
		});
	}

	private EngineData computeEngineROIs(ROIRatios roi)
	{
		BufferedImage currentImage = videoPlayer.getImage();

		if (currentImage != null)
		{
			BufferedImage engineRegionImage = ImageUtil.extractROI(currentImage, roi);
			long initialX = Math.round(roi.xRatio * currentImage.getWidth());
			long initialY = Math.round(roi.yRatio * currentImage.getHeight());
			return computeEngineBounds(engineRegionImage, currentImage.getWidth(), currentImage.getHeight(), initialX, initialY);
		}
		else
		{
			System.out.println("Could not get Image");
		}
		return null;
	}

	private SwingWorker<ROIRatios, Void> getFuelBoundWorker(Parameter parameter, ROIRatios roi)
	{
		final ROIRatios originalRoi = roi;

		return new SwingWorker<>()
		{
			@Override
			protected ROIRatios doInBackground()
			{
				return computeFuelROI(originalRoi);
			}

			@Override
			protected void done()
			{
				try
				{
					ROIRatios computedRoi = get();
					if (computedRoi != null)
					{
						saveROIs(parameter, computedRoi);
						overlayWindow.repaint();
					}

				}
				catch (InterruptedException | ExecutionException e)
				{
					System.out.println("Could not compute " + parameter.type.name + " region.");
				}
			}
		};
	}

	private ROIRatios computeFuelROI(ROIRatios roi)
	{
		BufferedImage currentImage = videoPlayer.getImage();

		if (currentImage != null)
		{
			BufferedImage fuelRegionImage = ImageUtil.extractROI(currentImage, roi);
			long initialX = Math.round(roi.xRatio * currentImage.getWidth());
			long initialY = Math.round(roi.yRatio * currentImage.getHeight());
			return computeFuelRegion(fuelRegionImage, currentImage.getWidth(), currentImage.getHeight(), initialX, initialY);
		}
		else
		{
			System.out.println("Could not get Image");
		}
		return null;
	}

	public void toggleOverlay(boolean state)
	{
		overlayWindow.setOverlayEnabled(state);
	}

	private JMenuBar createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenuItem setLocationItem = new JMenuItem("Set yt-dlp location...");
		setLocationItem.addActionListener(e -> setDLLocation());
		fileMenu.add(setLocationItem);
		JMenuItem transformFilesItem = new JMenuItem("Transform Files");
		transformFilesItem.addActionListener(e -> new TransformData());
		fileMenu.add(transformFilesItem);

		JMenu debugMenu = new JMenu("Debug");
		showDebugItem = new JCheckBoxMenuItem("Show OCR Debug");
		showDebugItem.setState(debug);
		showDebugItem.addActionListener(e -> toggleDebug());

		JMenuItem heapDumpItem = new JMenuItem("Dump Heap");
		heapDumpItem.addActionListener(e -> dumpHeap());

		debugMenu.add(showDebugItem);
		debugMenu.add(heapDumpItem);

		menuBar.add(fileMenu);
		menuBar.add(new ThemeMenu());
		menuBar.add(debugMenu);

		return menuBar;
	}

	private void dumpHeap()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Save heap dump");
		fc.setSelectedFile(new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "dump.hprof"));
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File fi = fc.getSelectedFile();
		fi.delete();
		String filename = fi.getAbsoluteFile().getPath();

		JFrame frame = new JFrame("Heap dump");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JLabel l = new JLabel("<html>Taking heap dump<br>This may take a while...");
		l.setBorder(new EmptyBorder(15, 15, 15, 15));
		frame.add(l);
		frame.setType(Window.Type.POPUP);
		frame.pack();
		frame.setVisible(true);
		frame.toFront();

		Timer t = new Timer(300, v ->
		{
			try
			{
				ManagementFactory.getPlatformMBeanServer().invoke(
					new ObjectName("com.sun.management:type=HotSpotDiagnostic"),
					"dumpHeap",
					new Object[]{filename, true},
					new String[]{String.class.getName(), boolean.class.getName()}
				);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this, e.toString(), "Heap dump error", JOptionPane.ERROR_MESSAGE);
			}

			frame.setVisible(false);
		});
		t.setRepeats(false);
		t.start();
	}

	private void saveEngineData(Parameter parameter, Map<Integer, EngineLocation> engines)
	{
		if (parameter == STAGE_1_ENGINES)
		{
			stage1EngineMap = engines;
			debugWindow.setEngineMap(parameter, stage1EngineMap);
			Settings.deleteEngines("Custom", 1);
		}
		else if (parameter == STAGE_2_ENGINES)
		{
			stage2EngineMap = engines;
			debugWindow.setEngineMap(parameter, stage2EngineMap);
			Settings.deleteEngines("Custom", 2);

		}

		for (Integer engineNumber : engines.keySet())
		{
			EngineLocation engineLocation = engines.get(engineNumber);
			Settings.setProperty("Custom", parameter.name + "-" + engineNumber, engineLocation.middle().x + "," + engineLocation.middle().y + "," + engineLocation.radius() + "," + engineLocation.ring());
		}
	}

	private void saveROIs(Parameter parameter, ROIRatios roiRatios)
	{
		Settings.setProperty("Custom", "ROI." + parameter.name.replace(" ", ".") + ".xRatio", String.valueOf(roiRatios.xRatio));
		Settings.setProperty("Custom", "ROI." + parameter.name.replace(" ", ".") + ".yRatio", String.valueOf(roiRatios.yRatio));
		Settings.setProperty("Custom", "ROI." + parameter.name.replace(" ", ".") + ".widthRatio", String.valueOf(roiRatios.widthRatio));
		Settings.setProperty("Custom", "ROI." + parameter.name.replace(" ", ".") + ".heightRatio", String.valueOf(roiRatios.heightRatio));

		if (Objects.equals(presetPanel.getActivePreset(), "Custom"))
		{
			regionMap.put(parameter, roiRatios);
		}
	}

	public void loadROIs(String presetName)
	{
		Preset preset = Settings.loadPreset(presetName);

		regionMap = preset.ratios();
		stage1EngineMap = preset.stage1EngineMap();
		stage2EngineMap = preset.stage2EngineMap();

		if (debugWindow != null)
		{
			debugWindow.setEngineMap(STAGE_1_ENGINES, stage1EngineMap);
			debugWindow.setEngineMap(STAGE_2_ENGINES, stage2EngineMap);
		}


		if (overlayWindow != null)
		{
			overlayWindow.repaint();
		}
	}

	public void switchROIs()
	{
		if (presetPanel != null) //todo you can kill this eventually
		{
			if (Objects.equals(presetPanel.getActivePreset(), "Custom"))
			{
				if (lastSelection != null)
				{
					Settings.deletePreset("Custom");
					Settings.copyPreset(lastSelection, "Custom");
				}
			}
			loadROIs(presetPanel.getActivePreset());

			overlayWindow.repaint();
			lastSelection = presetPanel.getActivePreset();
		}
	}

	public String getDLLocation()
	{
		return downloadLocation;
	}

	private void setDLLocation()
	{
		JFileChooser fileChooser = new JFileChooser();
		int userSelection = fileChooser.showOpenDialog(this);

		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			String filePath = selectedFile.getAbsolutePath();
			Settings.setProperty("setting", "yt-dl", filePath);
			downloadLocation = filePath;
		}
	}

	private void toggleDebug()
	{
		if (!debug)
		{
			debugWindow = new DebugWindow(this);
			debugWindow.setEngineMap(STAGE_1_ENGINES, stage1EngineMap);
			debugWindow.setEngineMap(STAGE_2_ENGINES, stage2EngineMap);
			debugWindow.setVisible(true);
		}
		else
		{
			debugWindow.setVisible(false);
			debugWindow = null;
		}
		debug = !debug;
	}

	public Map<Parameter, ROIRatios> getRegions()
	{
		return regionMap;
	}

	public void closeDebug()
	{
		debug = false;
		debugWindow = null;
		showDebugItem.setState(false);
	}

	public Map<Integer, EngineLocation> getStage1Engines()
	{
		return stage1EngineMap;
	}

	public Map<Integer, EngineLocation> getStage2Engines()
	{
		return stage2EngineMap;
	}

	public Dimension getVideoDimensions()
	{
		return videoPlayer.getVideoDimensions();
	}

	public Preset getPreset()
	{
		return presetPanel.getPreset();
	}

	public void setAdditionalTelemetryEnabled(boolean selected)
	{
		overlayWindow.setAdditionalTelemetryEnabled(selected);
		if (selected)
		{
			videoPlayer.enableBuffer(10000);
		}
		else
		{
			videoPlayer.disableBuffer();
		}
	}
}
