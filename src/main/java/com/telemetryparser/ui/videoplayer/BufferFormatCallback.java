package com.telemetryparser.ui.videoplayer;

import java.awt.image.BufferedImage;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

public class BufferFormatCallback extends BufferFormatCallbackAdapter
{
	private BufferedImage image;
	private final DefaultRenderCallBack renderCallBack;

	public BufferFormatCallback(DefaultRenderCallBack renderCallBack)
	{
		this.renderCallBack = renderCallBack;
	}

	@Override
	public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight)
	{
		image = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);
		renderCallBack.setImageBuffer(image);
		return new RV32BufferFormat(sourceWidth, sourceHeight);
	}

	public BufferedImage getImage()
	{
		return image;
	}
}
