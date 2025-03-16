package com.telemetryparser.datatransformation.util;

import java.util.*;

public class DataModel
{
	private final Set<String> columns = new LinkedHashSet<>();
	private final Map<Double, Map<String, Double>> data = new TreeMap<>();

	public void clear()
	{
		columns.clear();
		data.clear();
	}

	public void addColumns(List<String> cols)
	{
		columns.addAll(cols);
	}

	public void addRow(Double key, Map<String, Double> rowData)
	{
		if (!data.containsKey(key))
		{
			data.put(key, new HashMap<>());
		}
		data.get(key).putAll(rowData);
	}

	public void merge(DataModel other)
	{
		addColumns(new ArrayList<>(other.columns));
		for (Double key : other.data.keySet())
		{
			addRow(key, other.data.get(key));
		}
	}

	public Set<String> getColumns()
	{
		return columns;
	}

	public List<String> getColumnsInOrder()
	{
		return new ArrayList<>(columns);
	}

	public Object[][] toTableData(List<String> cols)
	{
		List<Double> keys = new ArrayList<>(data.keySet());
		Object[][] tableData = new Object[keys.size()][cols.size()];
		for (int i = 0; i < keys.size(); i++)
		{
			Double key = keys.get(i);
			Map<String, Double> row = data.get(key);
			for (int j = 0; j < cols.size(); j++)
			{
				String col = cols.get(j);
				tableData[i][j] = (j == 0) ? key : row.get(col);
			}
		}
		return tableData;
	}

	public DataModel filterColumns(Set<String> selectedCols)
	{
		DataModel filtered = new DataModel();
		filtered.addColumns(new ArrayList<>(selectedCols));
		for (Double key : data.keySet())
		{
			Map<String, Double> row = data.get(key);
			Map<String, Double> filteredRow = new HashMap<>();
			for (String col : selectedCols)
			{
				if (row.containsKey(col))
				{
					filteredRow.put(col, row.get(col));
				}
			}
			filtered.addRow(key, filteredRow);
		}
		return filtered;
	}
}

