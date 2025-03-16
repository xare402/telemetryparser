package com.telemetryparser.ui.icon;

public enum Icon
{
	PLAY("play"),
	PAUSE("pause"),
	UNMUTED("unmuted"),
	MUTED("muted"),
	FILE("File"),
	OPEN("Open"),
	REMOVE("Remove"),
	EDIT("Edit"),
	VISIBLE("Visible"),
	INVISIBLE("Invisible"),
	ADD("Add"),
	RESET("Reset");


	public final String filePath;
	public final int size;
	Icon(String filePath)
	{
		this(filePath, 20);
	}

	Icon(String filePath, int size)
	{
		this.filePath = filePath;
		this.size = size;
	}
}
