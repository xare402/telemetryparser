package com.telemetryparser.datatransformation.util;

import java.io.*;
import java.util.*;

public class CsvImportUtil
{
	public static DataModel importCsv(File file, String primaryColumn)
	{
		DataModel model = new DataModel();
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String header = br.readLine();
			if (header == null)
			{
				return model;
			}

			List<String> columns = Arrays.asList(header.split(",", -1));
			int primaryIndex = columns.indexOf(primaryColumn);
			model.addColumns(columns);

			String line;
			while ((line = br.readLine()) != null)
			{
				List<String> parts = Arrays.asList(line.split(",", -1));
				if (primaryIndex >= parts.size() || primaryIndex < 0)
				{
					continue;
				}

				Double primaryKey = parseDoubleOrNull(parts.get(primaryIndex));
				if (primaryKey == null)
				{
					continue;
				}

				Map<String, Double> row = new HashMap<>();
				for (int i = 0; i < columns.size(); i++)
				{
					if (i == primaryIndex)
					{
						continue;
					}
					String val = (i < parts.size()) ? parts.get(i) : "";
					row.put(columns.get(i), parseDoubleOrNull(val));
				}
				model.addRow(primaryKey, row);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return model;
	}

	public static DataModel importFolder(File folder, String primaryColumn)
	{
		DataModel merged = new DataModel();
		File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
		if (files == null)
		{
			return merged;
		}
		for (File f : files)
		{
			DataModel dm = importCsv(f, primaryColumn);
			merged.merge(dm);
		}
		return merged;
	}

	private static Double parseDoubleOrNull(String s)
	{
		try
		{
			return (s == null || s.trim().isEmpty()) ? null : Double.parseDouble(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
