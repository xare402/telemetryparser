package com.telemetryparser.dataserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataServer
{
	private final String host;
	private final int port;
	private final List<Socket> clients = new CopyOnWriteArrayList<>();
	private final List<DataServerListener> listeners = new CopyOnWriteArrayList<>();
	private ServerSocket serverSocket;
	private boolean running = false;
	private ExecutorService clientHandlingPool;
	private ScheduledExecutorService periodicScheduler;
	private volatile String lastMessage = "";
	private volatile String periodicMessage = null;

	public DataServer()
	{
		this("localhost", 30263);
	}

	public DataServer(String host, int port)
	{
		this.host = host;
		this.port = port;
	}

	public void addDataServerListener(DataServerListener listener)
	{
		this.listeners.add(listener);
	}

	public boolean isRunning()
	{
		return running;
	}

	public void start()
	{
		if (running)
		{
			System.out.println("Server is already running.");
			return;
		}

		if (clientHandlingPool == null || clientHandlingPool.isShutdown() || clientHandlingPool.isTerminated())
		{
			clientHandlingPool = Executors.newCachedThreadPool();
		}
		if (periodicScheduler == null || periodicScheduler.isShutdown() || periodicScheduler.isTerminated())
		{
			periodicScheduler = Executors.newSingleThreadScheduledExecutor();
		}

		try
		{
			serverSocket = new ServerSocket(port);
			running = true;
			System.out.println("Server started on " + host + ":" + port);

			for (DataServerListener l : listeners)
			{
				l.serverStarted();
			}

			clientHandlingPool.submit(() ->
			{
				while (running)
				{
					try
					{
						Socket clientSocket = serverSocket.accept();
						clients.add(clientSocket);

						System.out.println("Client connected from " + clientSocket.getRemoteSocketAddress());

						for (DataServerListener l : listeners)
						{
							l.connectionsChanged("join", clientSocket.getRemoteSocketAddress().toString(), clients.size());
						}

						clientHandlingPool.submit(() -> handleClient(clientSocket));

					}
					catch (IOException e)
					{
						if (running)
						{
							System.out.println("An error has occurred while the server was running. " + e.getMessage());
						}
					}
				}
			});

			periodicScheduler.scheduleAtFixedRate(() ->
			{
				if (periodicMessage != null && !lastMessage.isEmpty() && running)
				{
					String combined = "{" + periodicMessage + "}" + lastMessage;
					broadcastMessage(combined);
				}
			}, 30, 30, TimeUnit.SECONDS);

		}
		catch (IOException e)
		{
			System.out.println("The server was unable to start. " + e.getMessage());
		}
	}

	@SuppressWarnings({"StatementWithEmptyBody"})
	private void handleClient(Socket clientSocket)
	{
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))
		{
			while (in.readLine() != null)
			{
			}
		}
		catch (IOException e)
		{
			System.out.println("Connection forcibly lost by client. " + e.getMessage());
		}
		finally
		{
			System.out.println(clientSocket.getRemoteSocketAddress() + " has disconnected");
			clients.remove(clientSocket);
			try
			{
				clientSocket.close();
			}
			catch (IOException e)
			{
				System.out.println("Unable to close client socket");
			}
			for (DataServerListener l : listeners)
			{
				l.connectionsChanged("loss", clientSocket.getRemoteSocketAddress().toString(), clients.size());
			}
		}
	}

	public void stop()
	{
		running = false;
		System.out.println("Stopping server...");

		if (serverSocket != null && !serverSocket.isClosed())
		{
			try
			{
				serverSocket.close();
			}
			catch (IOException e)
			{
				System.out.println("An error was encountered while attempting to shutdown the server. " + e.getMessage());
			}
		}

		for (Socket socket : clients)
		{
			try
			{
				socket.close();
				for (DataServerListener l : listeners)
				{
					l.connectionsChanged("loss", socket.getRemoteSocketAddress().toString(), clients.size() - 1);
				}
			}
			catch (IOException e)
			{
				System.out.println("An error was encountered while attempting to close client connection with " + socket.getRemoteSocketAddress() + ". " + e.getMessage());
			}
		}
		clients.clear();

		if (clientHandlingPool != null)
		{
			clientHandlingPool.shutdownNow();
		}
		if (periodicScheduler != null)
		{
			periodicScheduler.shutdownNow();
		}

		for (DataServerListener l : listeners)
		{
			l.serverStopped();
		}
	}

	public void sendMessage(String message)
	{
		lastMessage = message;
		broadcastMessage(message);
	}

	public void setPeriodicMessage(String periodicMessage)
	{
		this.periodicMessage = periodicMessage;
	}

	private void broadcastMessage(String message)
	{
		for (Socket socket : clients)
		{
			try
			{
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(message);
			}
			catch (IOException e)
			{
				System.out.println("An error was encountered while trying to broadcast \"" + message + "\" to client " + socket.getRemoteSocketAddress());
			}
		}
	}
}
