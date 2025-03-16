package com.telemetryparser.datatransformation.uicomponents;

import com.telemetryparser.datatransformation.steps.TransformationStep;
import com.telemetryparser.datatransformation.util.MalleableData;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class DataTransformerPanel extends JPanel
{
	private final JTable previewTable;
	private final ColumnSelectionPanel columnSelectionPanel;
	private final TransformationListPanel transformationListPanel;
	private MalleableData originalData;
	private MalleableData workingData;
	private TransformWorker currentWorker;

	public DataTransformerPanel()
	{
		setLayout(new BorderLayout());

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		transformationListPanel = new TransformationListPanel(this::onTransformationListChanged, this);
		columnSelectionPanel = new ColumnSelectionPanel();
		columnSelectionPanel.addColumnSelectionListener(cols -> scheduleTransform());

		leftPanel.add(transformationListPanel);
		leftPanel.add(columnSelectionPanel);

		previewTable = new JTable();
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, new JScrollPane(previewTable));
		split.setDividerLocation(300);
		add(split, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton writeButton = new JButton("Write Data");
		bottomPanel.add(writeButton);
		add(bottomPanel, BorderLayout.SOUTH);

		writeButton.addActionListener(e -> writeDataAction());
	}

	public void loadData(Map<String, List<Double>> dataMap, String primaryCol)
	{
		originalData = new MalleableData(dataMap, primaryCol);
		workingData = new MalleableData(copyData(dataMap), primaryCol);

		columnSelectionPanel.setColumns(dataMap.keySet());
		refreshTable(dataMap);
		transformationListPanel.clearSteps();
	}

	public List<String> getAllColumns()
	{
		if (workingData == null)
		{
			return new ArrayList<>();
		}
		return new ArrayList<>(workingData.getCurrentData().keySet());
	}

	private void onTransformationListChanged()
	{
		scheduleTransform();
	}

	private void scheduleTransform()
	{
		if (originalData == null)
		{
			return;
		}
		if (currentWorker != null && !currentWorker.isDone())
		{
			currentWorker.cancel(true);
		}
		currentWorker = new TransformWorker();
		currentWorker.execute();
	}

	private void refreshTable(Map<String, List<Double>> data)
	{
		Set<String> selected = columnSelectionPanel.getSelectedColumns();
		if (selected.isEmpty())
		{
			previewTable.setModel(new javax.swing.table.DefaultTableModel());
			return;
		}
		String[] colNames = selected.toArray(new String[0]);
		int rowCount = data.get(colNames[0]).size();
		Object[][] tableData = new Object[rowCount][colNames.length];

		for (int c = 0; c < colNames.length; c++)
		{
			List<Double> colList = data.get(colNames[c]);
			for (int r = 0; r < rowCount; r++)
			{
				tableData[r][c] = colList.get(r);
			}
		}
		previewTable.setModel(new javax.swing.table.DefaultTableModel(tableData, colNames));
	}

	private Map<String, List<Double>> copyData(Map<String, List<Double>> original)
	{
		Map<String, List<Double>> copied = new java.util.LinkedHashMap<>();
		for (Map.Entry<String, List<Double>> e : original.entrySet())
		{
			copied.put(e.getKey(), new java.util.ArrayList<>(e.getValue()));
		}
		return copied;
	}

	private void writeDataAction()
	{
		if (workingData == null)
		{
			JOptionPane.showMessageDialog(this, "No data to write.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select Location to Save Transformed Data");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File file = chooser.getSelectedFile();
			writeDataToFile(file);
		}
	}

	private void writeDataToFile(File file)
	{
		Map<String, List<Double>> data = workingData.getCurrentData();
		if (data.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "No data available to write.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int rowCount = data.values().iterator().next().size();
		try (PrintWriter pw = new PrintWriter(new FileWriter(file)))
		{
			String header = String.join(",", data.keySet());
			pw.println(header);
			for (int i = 0; i < rowCount; i++)
			{
				StringBuilder row = new StringBuilder();
				for (String col : data.keySet())
				{
					if (!row.isEmpty())
					{
						row.append(",");
					}
					Double val = data.get(col).get(i);
					row.append(val != null ? val.toString() : "");
				}
				pw.println(row);
			}
			JOptionPane.showMessageDialog(this, "Data successfully written to:\n" + file.getAbsolutePath(),
				"Success", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private class TransformWorker extends SwingWorker<Map<String, List<Double>>, Void>
	{
		@Override
		protected Map<String, List<Double>> doInBackground()
		{
			workingData = new MalleableData(copyData(originalData.getCurrentData()), originalData.getTimeColumn());
			for (TransformationStep step : transformationListPanel.getSteps())
			{
				if (isCancelled())
				{
					break;
				}
				step.apply(workingData);
			}
			return workingData.getCurrentData();
		}

		@Override
		protected void done()
		{
			if (isCancelled())
			{
				return;
			}
			try
			{
				Map<String, List<Double>> transformed = get();
				Set<String> oldSelection = columnSelectionPanel.getSelectedColumns();
				Set<String> newColumns = transformed.keySet();
				if (!newColumns.equals(columnSelectionPanel.getAllColumns()))
				{
					columnSelectionPanel.setColumns(newColumns);
					columnSelectionPanel.setSelectedColumns(oldSelection);
				}
				refreshTable(transformed);
			}
			catch (ExecutionException | InterruptedException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
