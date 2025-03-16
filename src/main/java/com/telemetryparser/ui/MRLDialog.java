package com.telemetryparser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class MRLDialog extends JDialog
{
	private JTextArea textArea;
	private JTable table;
	private DefaultTableModel tableModel;
	MainWindow parent;
	private String result = "";

	public MRLDialog(MainWindow parent, String url)
	{
		super(parent, true);
		setTitle("M3U8 Finder");
		this.parent = parent;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		initComponents();
		pack();
		setLocationRelativeTo(parent);
		setSize(600, 400);
		new Thread(() -> showOptions(url)).start();
		setVisible(true);
	}

	private void initComponents()
	{
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new BorderLayout(5, 5));
		topPanel.add(new JLabel("This window does the same as X Broadcast URL but allows you to use a different format (yt-dlp -F)"));

		textArea = new JTextArea();
		textArea.setEditable(false);


		tableModel = new DefaultTableModel(new Object[]{"Action", "Value", "Empty"}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return column == 0;
			}
		};
		table = new JTable(tableModel);

		TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);

		table.setDefaultRenderer(Object.class, (tbl, value, isSelected, hasFocus, row, column) ->
		{
			Component c = defaultRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

			if (c instanceof JComponent jc)
			{
				if (row == tbl.getRowCount() - 1)
				{
					if (!isSelected)
					{
						Color accent = UIManager.getColor("Component.accentColor");
						if (accent != null)
						{
							Color accent30 = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 77);
							jc.setOpaque(true);
							jc.setBackground(accent30);
							jc.setForeground(UIManager.getColor("Component.accentColor"));
						}
					}
				}
				else
				{
					if (!isSelected)
					{
						jc.setBackground(tbl.getBackground());
					}
				}
			}
			return c;
		});

		table.setTableHeader(null);
		table.setRowHeight(30);

		table.getColumnModel().getColumn(0).setMaxWidth(70);
		table.getColumnModel().getColumn(2).setMaxWidth(200);
		table.getColumnModel().getColumn(2).setMinWidth(120);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

		table.getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer());
		table.getColumnModel().getColumn(0).setCellEditor(new ButtonEditor(new JCheckBox()));

		JScrollPane tableScrollPane = new JScrollPane(table);

		JScrollPane textScroll = new JScrollPane(textArea);

		JPanel splitPanel = new JPanel(new GridLayout(2, 1));
		splitPanel.add(textScroll);
		splitPanel.add(tableScrollPane);

		add(topPanel, BorderLayout.NORTH);
		add(splitPanel, BorderLayout.CENTER);
	}

	public String getResult()
	{
		return result;
	}

	private String url = "";

	public void showOptions(String url)
	{
		this.url = url;
		tableModel.setRowCount(0);

		textArea.setText("");

		try
		{
			ProcessBuilder processBuilder = new ProcessBuilder(parent.getDLLocation(), "-F", url);
			Process process = processBuilder.start();

			try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				boolean found = false;
				String line;
				while ((line = stdOutReader.readLine()) != null)
				{
					if (line.contains("------"))
					{
						found = true;
					}
					else if (found)
					{
						tableModel.addRow(new Object[]{"Select", line, ""});
					}
					else
					{
						textArea.append(line + "\n");
					}
				}
			}

			if (table.getRowCount() > 0)
			{
				tableModel.setValueAt("Recommended", tableModel.getRowCount() - 1, tableModel.getColumnCount() - 1);
			}

			int exitCode = process.waitFor();
			textArea.append("Process exited with code: " + exitCode);

		}
		catch (Exception e)
		{
			System.out.println("Failed to run yt-dl");
		}
	}

	private void selectRow(String item)
	{
		String format = item.split(" ", -1)[0];
		textArea.setText("Loading stream from format please wait...");

		try
		{
			ProcessBuilder processBuilder = new ProcessBuilder(parent.getDLLocation(), "-f", format, "-g", url);
			Process process = processBuilder.start();

			try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				String line;
				while ((line = stdOutReader.readLine()) != null)
				{
					result = line;
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		dispose();
	}

	private static class ButtonRenderer extends JButton implements TableCellRenderer
	{
		public ButtonRenderer()
		{
			setOpaque(true);
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{

			if (isSelected)
			{
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else
			{
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	private class ButtonEditor extends DefaultCellEditor
	{
		private final JButton button;
		private String label;
		private int row;
		private JTable tableRef;

		public ButtonEditor(JCheckBox checkBox)
		{
			super(checkBox);
			button = new JButton();
			button.setOpaque(true);

			button.addActionListener(e ->
			{
				fireEditingStopped();
				String itemValue = (String) tableRef.getValueAt(row, 1);
				new Thread(() -> selectRow(itemValue)).start();
			});
		}

		@Override
		public java.awt.Component getTableCellEditorComponent(
			JTable table, Object value, boolean isSelected, int row, int column)
		{
			this.tableRef = table;
			this.row = row;
			this.label = (value == null) ? "" : value.toString();
			button.setText(label);
			return button;
		}

		@Override
		public Object getCellEditorValue()
		{
			return label;
		}

		@Override
		public boolean stopCellEditing()
		{
			return super.stopCellEditing();
		}

		@Override
		protected void fireEditingStopped()
		{
			super.fireEditingStopped();
		}
	}
}
