package com.telemetryparser.datatransformation.uicomponents;

import com.telemetryparser.datatransformation.steps.ComputePerEngineAcceleration;
import com.telemetryparser.datatransformation.steps.RestrictListsStep;
import com.telemetryparser.datatransformation.steps.ComputeECEFColumnsStep;
import com.telemetryparser.datatransformation.steps.InterpolateAndDerive2xStep;
import com.telemetryparser.datatransformation.steps.RemoveDuplicatesStep;
import com.telemetryparser.datatransformation.steps.TransformationStep;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.List;

public class TransformationListPanel extends JPanel
{
	private final DefaultListModel<TransformationStep> listModel;
	private final JList<TransformationStep> stepList;
	private final JButton addButton;
	private final Runnable listChangedCallback;
	private final DataTransformerPanel dataTransformerPanel;

	public TransformationListPanel(Runnable onListChanged, DataTransformerPanel dataTransformerPanel)
	{
		this.listChangedCallback = onListChanged;
		this.dataTransformerPanel = dataTransformerPanel;
		setLayout(new BorderLayout());
		listModel = new DefaultListModel<>();
		stepList = new JList<>(listModel);

		TransformationCellRenderer renderer = new TransformationCellRenderer();
		stepList.setCellRenderer(renderer);

		stepList.setDragEnabled(true);
		stepList.setDropMode(DropMode.INSERT);
		stepList.setTransferHandler(new ListItemTransferHandler(stepList));

		stepList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int index = stepList.locationToIndex(e.getPoint());
				if (index >= 0)
				{
					Rectangle cellBounds = stepList.getCellBounds(index, index);
					if (cellBounds != null && cellBounds.contains(e.getPoint()))
					{
						TransformationStep step = listModel.get(index);
						Rectangle xRect = renderer.getRemoveRect(step);
						int cellX = cellBounds.x;
						int cellY = cellBounds.y;
						Rectangle actualXRect = new Rectangle(cellX + xRect.x, cellY + xRect.y, xRect.width, xRect.height);

						if (actualXRect.contains(e.getPoint()))
						{
							listModel.remove(index);
							listChangedCallback.run();
						}
					}
				}
			}
		});

		addButton = new JButton(" + ");
		addButton.addActionListener(e -> showAddMenu());

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Transformations"));
		topPanel.add(addButton);

		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(stepList), BorderLayout.CENTER);
	}

	public void clearSteps()
	{
		listModel.clear();
	}

	public List<TransformationStep> getSteps()
	{
		return Collections.list(listModel.elements());
	}

	private void showAddMenu()
	{
		JPopupMenu menu = new JPopupMenu();

		menu.add(getRemoveDupItem());
		menu.add(getRestrictItem());
		menu.add(getInterpolateItem());
		menu.add(getComputeECEFItem());
		menu.add(getComputeEngineAccelerationItem());

		menu.show(addButton, 0, addButton.getHeight());
	}

	private JMenuItem getComputeEngineAccelerationItem()
	{
		JMenuItem computeEngineAccelerationItem = new JMenuItem("Compute per engine acceleration");
		computeEngineAccelerationItem.addActionListener(e ->
		{
			List<String> columns = dataTransformerPanel.getAllColumns();
			ComputeEngineAccelerationDialog dlg = new ComputeEngineAccelerationDialog(SwingUtilities.getWindowAncestor(this), columns);
			boolean confirmed = dlg.showDialog();
			if (confirmed)
			{
				listModel.addElement(new ComputePerEngineAcceleration(
					dlg.getTimeColumn(),
					dlg.getAccelerationColumn(),
					dlg.getEngineColumn(),
					dlg.getOutputColumn()
				));
				listChangedCallback.run();
			}
		});
		return computeEngineAccelerationItem;
	}

	private JMenuItem getComputeECEFItem()
	{
		JMenuItem computeECEFItem = new JMenuItem("Compute ECEF Columns...");
		computeECEFItem.addActionListener(e ->
		{
			List<String> columns = dataTransformerPanel.getAllColumns();
			ComputeECEFColumnsDialog dlg = new ComputeECEFColumnsDialog(SwingUtilities.getWindowAncestor(this), columns);
			boolean confirmed = dlg.showDialog();
			if (confirmed)
			{
				listModel.addElement(new ComputeECEFColumnsStep(
					dlg.getInitialLatitude(),
					dlg.getInitialLongitude(),
					dlg.getInitialAltitude(),
					dlg.getInitialAzimuth(),
					dlg.getTimeColumn(),
					dlg.getAltitudeChangeColumn(),
					dlg.getSpeedMagnitudeColumn(),
					dlg.getEcefXColumn(),
					dlg.getEcefYColumn(),
					dlg.getEcefZColumn(),
					dlg.getDeltaXColumn(),
					dlg.getDeltaYColumn(),
					dlg.getDeltaZColumn()
				));
				listChangedCallback.run();
			}
		});
		return computeECEFItem;
	}

	private JMenuItem getInterpolateItem()
	{
		JMenuItem interpolateItem = new JMenuItem("Interpolate & Derive 2x...");
		interpolateItem.addActionListener(e ->
		{
			List<String> columns = dataTransformerPanel.getAllColumns();
			InterpolateAndDerive2xDialog dlg = new InterpolateAndDerive2xDialog(SwingUtilities.getWindowAncestor(this), columns);
			boolean confirmed = dlg.showDialog();
			if (confirmed)
			{
				listModel.addElement(new InterpolateAndDerive2xStep(
					dlg.getInColumn(),
					dlg.getInterpolatedColumn(),
					dlg.getFirstDerivativeColumn(),
					dlg.getSecondDerivativeColumn()
				));
				listChangedCallback.run();
			}
		});
		return interpolateItem;
	}

	private JMenuItem getRestrictItem()
	{
		JMenuItem restrictItem = new JMenuItem("Restrict Lists...");
		restrictItem.addActionListener(e ->
		{
			RestrictListDialog dialog = new RestrictListDialog(SwingUtilities.getWindowAncestor(this), dataTransformerPanel.getAllColumns());
			RestrictListResult param = dialog.showDialog("Restrict Lists", 10);
			if (param != null)
			{
				listModel.addElement(new RestrictListsStep(param));
				listChangedCallback.run();
			}
		});
		return restrictItem;
	}

	private JMenuItem getRemoveDupItem()
	{
		JMenuItem removeDupItem = new JMenuItem("Remove Duplicates");
		removeDupItem.addActionListener(e ->
		{
			listModel.addElement(new RemoveDuplicatesStep());
			listChangedCallback.run();
		});
		return removeDupItem;
	}

	private static class ListItemTransferHandler extends TransferHandler
	{
		private final JList<TransformationStep> list;
		private final DataFlavor dataFlavor = new DataFlavor(List.class, "List of Steps");

		public ListItemTransferHandler(JList<TransformationStep> list)
		{
			this.list = list;
		}

		@Override
		public boolean importData(TransferSupport support)
		{
			if (!canImport(support))
			{
				return false;
			}
			try
			{
				Transferable t = support.getTransferable();
				@SuppressWarnings("unchecked")
				List<TransformationStep> data = (List<TransformationStep>) t.getTransferData(dataFlavor);
				JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
				int index = dropLocation.getIndex();
				DefaultListModel<TransformationStep> model =
					(DefaultListModel<TransformationStep>) list.getModel();
				for (TransformationStep step : data)
				{
					model.add(index++, step);
				}
				return true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean canImport(TransferSupport info)
		{
			return info.isDataFlavorSupported(dataFlavor);
		}

		@Override
		public int getSourceActions(JComponent c)
		{
			return MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			List<TransformationStep> values = list.getSelectedValuesList();
			return new StepTransferable(values);
		}

		@Override
		public void exportDone(JComponent c, Transferable t, int action)
		{
			if (action == MOVE)
			{
				for (TransformationStep val : list.getSelectedValuesList())
				{
					((DefaultListModel<TransformationStep>) list.getModel()).removeElement(val);
				}
			}
		}

		private class StepTransferable implements Transferable
		{
			private final List<TransformationStep> data;

			public StepTransferable(List<TransformationStep> data)
			{
				this.data = data;
			}

			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[]{dataFlavor};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return flavor.equals(dataFlavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor)
			{
				return data;
			}
		}
	}
}
