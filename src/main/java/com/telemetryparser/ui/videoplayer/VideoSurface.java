package com.telemetryparser.ui.videoplayer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class VideoSurface extends JPanel
{
	private final BufferFormatCallback bufferCallback;
	private final VideoPlayer videoPlayer;

	// Navigable map to store frames keyed by player time
	private final NavigableMap<Long, BufferedImage> frameBuffer = new TreeMap<>();
	private final ReentrantReadWriteLock bufferLock = new ReentrantReadWriteLock();

	// Dynamic buffer delay (in milliseconds). 0 means no buffering (realtime).
	private volatile long bufferDelayMs = 0;

	private long lastPlayerTime = -1;
	private String text = "Please load a video.";
	private Dimension bufferDimension = null;
	private BufferedImage lastImage = null;

	public VideoSurface(BufferFormatCallback bufferCallback, VideoPlayer videoPlayer)
	{
		this.bufferCallback = bufferCallback;
		this.videoPlayer = videoPlayer;

		this.bufferDelayMs = 0;

		videoPlayer.addVideoPlayerFrameChangedListener((image, systemTime, playerTime) ->
		{
			bufferLock.writeLock().lock();
			try
			{
				boolean isSeek = lastPlayerTime != -1 &&
					(playerTime < lastPlayerTime ||
						playerTime > lastPlayerTime + 500);
				if (isSeek)
				{
					frameBuffer.clear();
				}

				lastPlayerTime = playerTime;

				if (bufferDelayMs > 0)
				{
					frameBuffer.put(playerTime, image);

					long targetTime = playerTime - bufferDelayMs;
					Map.Entry<Long, BufferedImage> frameEntry = frameBuffer.floorEntry(targetTime);

					if (frameEntry != null)
					{
						lastImage = frameEntry.getValue();

						if (frameBuffer.size() > 1)
						{
							frameBuffer.headMap(frameEntry.getKey(), false).clear();
						}
					}
					else if (!frameBuffer.isEmpty())
					{
						long bufferingProgress = playerTime - frameBuffer.firstKey();
						double secondsBuffered = bufferingProgress / 1000.0;
						double totalBufferSeconds = bufferDelayMs / 1000.0;

						setText(String.format("Additional telemetry requires a 10 second buffer to function. Buffering: %.1f of %.1f seconds",
							Math.min(secondsBuffered, totalBufferSeconds),
							totalBufferSeconds));
					}

					while (frameBuffer.size() > 300)
					{
						frameBuffer.pollFirstEntry();
					}
				}
				else
				{
					frameBuffer.clear();
					lastImage = image;
				}
			}
			finally
			{
				bufferLock.writeLock().unlock();
			}

			repaint();
		});
	}

	public Rectangle getDrawDimension(int videoWidth, int videoHeight)
	{
		int width = getWidth();
		int height = getHeight();

		float sx = (float) width / videoWidth;
		float sy = (float) height / videoHeight;
		float scale = Math.min(sx, sy);

		int drawWidth = (int) (videoWidth * scale);
		int drawHeight = (int) (videoHeight * scale);

		int x = (width - drawWidth) / 2;
		int y = (height - drawHeight) / 2;
		return new Rectangle(x, y, drawWidth, drawHeight);
	}

	public Dimension getLastBufferDimension()
	{
		return bufferDimension;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		BufferedImage image = lastImage;
		int width = getWidth();
		int height = getHeight();
		g.setColor(UIManager.getColor("Label.background"));
		g.fillRect(0, 0, width, height);

		if (image != null)
		{
			Dimension newBufferDimension = new Dimension(image.getWidth(), image.getHeight());
			if (bufferDimension == null ||
				bufferDimension.width != newBufferDimension.width ||
				bufferDimension.height != newBufferDimension.height)
			{
				bufferDimension = newBufferDimension;
				videoPlayer.notifyDimensionsChanged();
			}
			Rectangle bounds = getDrawDimension(image.getWidth(), image.getHeight());
			g.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
		}
		else
		{
			g.setColor(UIManager.getColor("Label.foreground"));
			FontMetrics fm = g.getFontMetrics();
			int textWidth = fm.stringWidth(text);
			int textHeight = fm.getHeight();

			int xText = (width - textWidth) / 2;
			int yText = (height - textHeight) / 2 + fm.getAscent();

			g.drawString(text, xText, yText);

			Color baseColor = UIManager.getColor("Component.accentColor");
			g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 128));
			g.drawRect(5, 5, width - 10, height - 10);
		}
	}

	public void setText(String text)
	{
		this.text = text;
		repaint();
	}

	public void clearBuffer()
	{
		bufferLock.writeLock().lock();
		try
		{
			frameBuffer.clear();
			lastImage = null;
			lastPlayerTime = -1;
			setText("Please load a video.");
		}
		finally
		{
			bufferLock.writeLock().unlock();
		}
		repaint();
	}

	public void enableBuffer(int ms)
	{
		if (ms <= 0)
		{
			disableBuffer();
			return;
		}

		bufferLock.writeLock().lock();
		try
		{
			frameBuffer.clear();
			lastImage = null;
			lastPlayerTime = -1;
			bufferDelayMs = ms;
			setText("Buffering...");
		}
		finally
		{
			bufferLock.writeLock().unlock();
		}
		repaint();
	}

	public void disableBuffer()
	{
		bufferLock.writeLock().lock();
		try
		{
			bufferDelayMs = 0;
			frameBuffer.clear();
			lastImage = null;
			lastPlayerTime = -1;
			setText("Real-time video (no buffer).");
		}
		finally
		{
			bufferLock.writeLock().unlock();
		}
		repaint();
	}
}
