package com.telemetryparser.dataserver;

public interface DataServerListener
{
	void serverStarted();

	void serverStopped();

	void connectionsChanged(String type, String clientHost, int totalConnections);
}
