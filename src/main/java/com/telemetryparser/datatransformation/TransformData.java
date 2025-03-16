package com.telemetryparser.datatransformation;

import com.telemetryparser.datatransformation.uicomponents.DataTransformerPanel;
import com.telemetryparser.datatransformation.util.CsvImportUtil;
import com.telemetryparser.datatransformation.util.DataModel;
import com.telemetryparser.settings.Settings;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.*;

public class TransformData extends JFrame
{
	private final JTextField primaryField;
	private final DataTransformerPanel dataTransformerPanel;

	public TransformData()
	{
		super("Transform Data");
		setLayout(new BorderLayout());

		JPanel topPanel = new JPanel(new FlowLayout());
		primaryField = new JTextField("Time", 15);
		JButton fileButton = new JButton("Choose File/Folder");
		topPanel.add(primaryField);
		topPanel.add(fileButton);

		dataTransformerPanel = new DataTransformerPanel();
		add(topPanel, BorderLayout.NORTH);
		add(dataTransformerPanel, BorderLayout.CENTER);

		fileButton.addActionListener(this::chooseFileOrFolder);

		setSize(1000, 600);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void chooseFileOrFolder(ActionEvent e)
	{
		String existingFileLocation = Settings.getProperty("setting", "transformFileLocation");
		File defaultLocation = (existingFileLocation != null) ? new File(existingFileLocation) : null;

		JFileChooser chooser = (defaultLocation != null && defaultLocation.exists()) ? new JFileChooser(defaultLocation) : new JFileChooser();

		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File f = chooser.getSelectedFile();
			DataModel model;
			if (f.isDirectory())
			{
				model = CsvImportUtil.importFolder(f, primaryField.getText());
			}
			else
			{
				model = CsvImportUtil.importCsv(f, primaryField.getText());
			}
			Map<String, List<Double>> map = convertModelToMap(model);
			dataTransformerPanel.loadData(map, primaryField.getText());
			Settings.setProperty("setting", "transformFileLocation", f.getParentFile().getAbsolutePath());
		}
	}

	private Map<String, List<Double>> convertModelToMap(DataModel model)
	{
		Map<String, List<Double>> result = new LinkedHashMap<>();
		List<Double> keys = new ArrayList<>(model.filterColumns(model.getColumns()).toTableData(model.getColumnsInOrder()).length);

		List<Double> rowKeys = new ArrayList<>(model.toTableData(model.getColumnsInOrder()).length);
		Object[][] dataArray = model.toTableData(model.getColumnsInOrder());
		for (Object[] row : dataArray)
		{
			rowKeys.add((Double) row[0]);
		}

		List<String> columns = model.getColumnsInOrder();
		for (int colIdx = 0; colIdx < columns.size(); colIdx++)
		{
			String colName = columns.get(colIdx);
			List<Double> colValues = new ArrayList<>();
			for (Object[] objects : dataArray)
			{
				if (colIdx == 0)
				{
					colValues.add((Double) objects[0]);
				}
				else
				{
					colValues.add((Double) objects[colIdx]);
				}
			}
			result.put(colName, colValues);
		}
		return result;
	}
}
