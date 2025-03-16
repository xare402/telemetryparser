package com.telemetryparser.core.fixed;

import com.telemetryparser.core.FrameStatus;
import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.core.TelemetrySnapshot;
import com.telemetryparser.dataserver.DataAccumulator;
import com.telemetryparser.dataserver.DataAccumulatorListener;
import com.telemetryparser.settings.Settings;
import com.telemetryparser.timeline.BranchResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FrameManager
{
	private final PriorityBlockingQueue<QueuedFrame> frameQueue = new PriorityBlockingQueue<>(100, Comparator.comparingLong(q -> q.frameData().systemTime()));
	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicLong frameCounter = new AtomicLong(0);
	private final ConcurrentMap<Long, FrameAnalysisResult> completedResults = new ConcurrentHashMap<>();
	private long nextToDispatch = 0;
	private final AtomicLong lastJump = new AtomicLong(-3);
	private final List<FrameCompletedListener> frameCompletedListeners = new CopyOnWriteArrayList<>();
	private final FrameBranchManager branchManager = new FrameBranchManager();
	private final DataAccumulator dataAccumulator = new DataAccumulator(1, 30, ParameterType.ENGINES, ParameterType.ENGINES_VARIANT);
	private long lastFrameSystemTime = -3;
	private long totalProcessedFrames = 0;
	private long totalFlaggedFrames = 0;


	public void addDataListener(DataAccumulatorListener dl)
	{
		dataAccumulator.addListener(dl);
	}

	public FrameManager()
	{
		Thread dispatcherThread = new Thread(this::frameProcessingLoop, "FrameProcessingLoop");
		dispatcherThread.start();
	}

	public void queueFrame(FrameData frameData)
	{
		long frameNumber = frameCounter.getAndIncrement();
		frameQueue.offer(new QueuedFrame(frameNumber, frameData));
	}

	private void frameProcessingLoop()
	{
		try
		{
			while (!Thread.currentThread().isInterrupted())
			{
				QueuedFrame queuedFrame = frameQueue.take();

				executor.submit(() ->
				{
					FrameProcessor frameProcessor = new FrameProcessor(queuedFrame.frameData());
					FrameAnalysisResult result = frameProcessor.compute();

					completedResults.put(queuedFrame.frameNumber(), result);
					dispatchCompletedFrameInOrder();
				});
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	private void dispatchCompletedFrameInOrder()
	{
		synchronized (this)
		{
			while (true)
			{
				FrameAnalysisResult nextResult = completedResults.remove(nextToDispatch);
				if (nextResult == null)
				{
					break;
				}
				handleFrameCompleted(nextResult);
				nextToDispatch++;
			}
		}
	}

	private boolean videoPlayerSeekedRecently()
	{
		return System.currentTimeMillis() - lastJump.get() < 1000;
	}

	private void handleFrameCompleted(FrameAnalysisResult frameResult)
	{
		Integer seconds = frameResult.getTimeAsSeconds();
		FrameStatus status = FrameStatus.SUCCESS;
		boolean seekedRecently = videoPlayerSeekedRecently();
		if (seekedRecently || seconds == null)
		{
			status = seekedRecently ? FrameStatus.RECENT_JUMP : FrameStatus.TIME_PARSE_FAILURE;
			notifyListeners(new FrameCompletedEvent(frameResult, status));
			return;
		}

		long elapsedMs = lastFrameSystemTime < 0 ? 0 : frameResult.frameData().systemTime() - lastFrameSystemTime;
		lastFrameSystemTime = frameResult.frameData().systemTime();

		TelemetrySnapshot snapshot = frameResult.createSnapshot(seconds);
		BranchResult branchResult = branchManager.handleFrame(snapshot);
		try
		{
			dataAccumulator.addSnapshot(snapshot);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		totalProcessedFrames++;
		if (branchResult.outlierFound())
		{
			totalFlaggedFrames++;
			status = branchResult.accepted() ? FrameStatus.SUCCESS_BUT_OUTLIER : FrameStatus.SUCCESS_BUT_FLAGGED_AND_OUTLIER;
		}
		notifyListeners(new FrameCompletedEvent(elapsedMs, !branchResult.accepted(), totalFlaggedFrames, totalProcessedFrames, branchResult.time(), frameResult, branchResult.finalParameterMap(), status));
	}

	public void addFrameCompletedListener(FrameCompletedListener listener)
	{
		frameCompletedListeners.add(listener);
	}

	private void notifyListeners(FrameCompletedEvent frameCompletedEvent)
	{
		for(FrameCompletedListener listener : frameCompletedListeners)
		{
			listener.onFrameCompleted(frameCompletedEvent);
		}
	}

	public void timeChanged()
	{
		lastJump.set(System.currentTimeMillis());
	}

	public void reset()
	{
		totalProcessedFrames = 0;
		totalFlaggedFrames = 0;
		lastFrameSystemTime = -3;
		lastJump.set(-3);
		branchManager.clearData();
	}

	public void writeData()
	{
		String existingFileLocation = Settings.getProperty("setting", "saveFileLocation");
		File defaultLocation = (existingFileLocation != null) ? new File(existingFileLocation) : null;

		JFileChooser fileChooser = (defaultLocation != null && defaultLocation.exists()) ? new JFileChooser(defaultLocation) : new JFileChooser();
		fileChooser.setDialogTitle("Choose where to save the file");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		int userSelection = fileChooser.showSaveDialog(null);

		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();

			if (selectedFile.exists())
			{
				JOptionPane.showMessageDialog(null,
					"File already exists. Please choose a different name or location.", "File Exists", JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				try
				{
					if (selectedFile.createNewFile())
					{
						String filePath = selectedFile.getAbsolutePath();
						writeDataToFile(filePath);
						Settings.setProperty("setting", "saveFileLocation", selectedFile.getParentFile().getAbsolutePath());

					}
					else
					{
						JOptionPane.showMessageDialog(null, "Could not create the file.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null,
						"An I/O error occurred while creating the file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					System.out.println("An I/O error occurred while creating the file: " + e.getMessage());
				}
			}
		}
	}

	public void writeDataToFile(String filepath)
	{
		String header = Parameter.getAsHeader();
		List<TelemetrySnapshot> snapshots = branchManager.getMergedBranches();
		try
		{
			File file = new File(filepath);
			File directory = file.getParentFile();
			if (directory != null && !directory.exists())
			{
				System.out.println(directory.mkdirs());
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
			{
				writer.write(header);
				writer.newLine();
				synchronized (snapshots)
				{
					for (TelemetrySnapshot telemetrySnapshot : snapshots)
					{
						String line = telemetrySnapshot.toCSVTranslated(ParameterType.TIME, ParameterType.SPEED, ParameterType.ALTITUDE, ParameterType.FUEL, ParameterType.PITCH, ParameterType.ENGINES, ParameterType.ENGINES_VARIANT);
						writer.write(line);
						writer.newLine();
					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Error writing to file: " + e.getMessage());
		}
	}
}
