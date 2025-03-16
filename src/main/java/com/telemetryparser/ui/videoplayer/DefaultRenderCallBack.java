package com.telemetryparser.ui.videoplayer;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;

public class DefaultRenderCallBack extends RenderCallbackAdapter
{
	List<JPanel> children = new ArrayList<>();
	private BufferFormatCallback bufferFormatCallback;
	private final VideoPlayer videoPlayer;
	public DefaultRenderCallBack(VideoPlayer videoPlayer)
	{
		super(null);
		this.videoPlayer = videoPlayer;
	}

	public void setBufferFormatCallback(BufferFormatCallback bufferFormatCallback)
	{
		this.bufferFormatCallback = bufferFormatCallback;
	}

	@Override
	public void onDisplay(MediaPlayer mediaPlayer, int[] array)
	{
		if (mediaPlayer.status().isPlaying())
		{
			long systemTime = System.currentTimeMillis();
			long playerTime = mediaPlayer.status().time();
			BufferedImage image = bufferFormatCallback.getImage();
			if (image != null)
			{
				BufferedImage imageCopy = deepCopy(bufferFormatCallback.getImage());
				videoPlayer.notifyFrameChanged(imageCopy, playerTime, systemTime);
			}
			children.forEach(Component::repaint);
		}
	}

	private BufferedImage deepCopy(BufferedImage source)
	{
		BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		Graphics2D g2d = copy.createGraphics();
		g2d.drawImage(source, 0, 0, null);
		g2d.dispose();
		return copy;
	}

	public void setImageBuffer(BufferedImage image)
	{
		setBuffer(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
	}

	public void addSurface(VideoSurface videoSurface)
	{
		children.add(videoSurface);
	}
}
