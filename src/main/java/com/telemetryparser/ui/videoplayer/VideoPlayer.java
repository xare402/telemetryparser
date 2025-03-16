package com.telemetryparser.ui.videoplayer;

import static com.telemetryparser.ui.icon.Icon.MUTED;
import static com.telemetryparser.ui.icon.Icon.PAUSE;
import static com.telemetryparser.ui.icon.Icon.PLAY;
import static com.telemetryparser.ui.icon.Icon.UNMUTED;
import com.telemetryparser.ui.icon.IconManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPlayer extends JPanel
{
	private final VideoSurface videoSurface;
	private final CallbackMediaPlayerComponent mediaPlayerComponent;
	private final JButton playButton = new JButton(IconManager.get(PLAY));
	private final JButton muteButton = new JButton(IconManager.get(UNMUTED));
	private JSlider timeSlider;
	private final JLabel currentTimeLabel = new JLabel("00:00:00");
	private final JLabel totalTimeLabel = new JLabel("00:00:00");
	private final AtomicBoolean isPlaying = new AtomicBoolean(false);
	private final AtomicBoolean isSeeking = new AtomicBoolean(false);
	private Boolean muted = false;
	private final List<VideoPlayerChangedListener> dimensionChangedListeners = new ArrayList<>();
	private final List<VideoPlayerChangedListener> timeChangedListeners = new ArrayList<>();
	private final List<VideoPlayerFrameChangedListener> videoPlayerFrameChangedListeners = new ArrayList<>();

	public VideoPlayer()
	{

		setLayout(new BorderLayout());

		DefaultRenderCallBack renderCallBack = new DefaultRenderCallBack(this);
		BufferFormatCallback bufferFormatCallback = new BufferFormatCallback(renderCallBack);
		renderCallBack.setBufferFormatCallback(bufferFormatCallback);

		//We use custom arguments so that taking a screenshot doesn't show the temp file name overlaid on the player
		String[] modifiedArguments = new String[]{"--video-title=vlcj video output", "--no-snapshot-preview", "--quiet", "--intf=dummy", "--no-osd"};
		mediaPlayerComponent = new CallbackMediaPlayerComponent(new MediaPlayerFactory(modifiedArguments), null, null, true, null, renderCallBack, bufferFormatCallback, null);
		mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(getMediaPlayerEventAdapter());

		videoSurface = new VideoSurface(bufferFormatCallback, this);
		videoSurface.addMouseListener(getMouseAdapter());
		renderCallBack.addSurface(videoSurface);

		JPanel timePanel = getTimePanel();
		JPanel buttonControlsPanel = getButtonControlsPanel();

		JPanel controlsPanel = new JPanel(new BorderLayout());
		controlsPanel.add(timePanel, BorderLayout.CENTER);
		controlsPanel.add(buttonControlsPanel, BorderLayout.EAST);

		playButton.addActionListener(e -> togglePlay());
		timeSlider.addChangeListener(e -> updateTimeSlider());

		add(controlsPanel, BorderLayout.SOUTH);
		add(videoSurface, BorderLayout.CENTER);
	}

	private JPanel getButtonControlsPanel()
	{
		JPanel buttonControlsPanel = new JPanel(new GridLayout(1, 2));
		muteButton.addActionListener(al ->
		{
			try
			{
				toggleMute();
			}
			catch (Exception ignored)
			{
			}
		});

		buttonControlsPanel.add(playButton);
		buttonControlsPanel.add(muteButton);
		return buttonControlsPanel;
	}

	private JPanel getTimePanel()
	{
		JPanel timePanel = new JPanel(new BorderLayout());
		timeSlider = getBaseTimeSlider();
		timePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		timePanel.add(currentTimeLabel, BorderLayout.WEST);
		timePanel.add(timeSlider, BorderLayout.CENTER);
		timePanel.add(totalTimeLabel, BorderLayout.EAST);
		return timePanel;
	}


	private JSlider getBaseTimeSlider()
	{
		final JSlider timeSlider;
		timeSlider = new JSlider();
		timeSlider.setMinimum(0);
		timeSlider.setValue(0);
		timeSlider.setEnabled(false);
		return timeSlider;
	}

	private MediaPlayerEventAdapter getMediaPlayerEventAdapter()
	{
		return new MediaPlayerEventAdapter()
		{
			@Override
			public void finished(MediaPlayer mediaPlayer)
			{
				SwingUtilities.invokeLater(() ->
				{
					timeSlider.setValue(timeSlider.getMinimum());
					currentTimeLabel.setText("00:00:00");
				});
			}

			@Override
			public void timeChanged(MediaPlayer mediaPlayer, long newTime)
			{
				SwingUtilities.invokeLater(() ->
				{
					if (!isSeeking.get())
					{
						timeSlider.setValue((int) newTime);
						currentTimeLabel.setText(formatTime(newTime));
					}
				});
			}

			@Override
			public void lengthChanged(MediaPlayer mediaPlayer, long newLength)
			{
				SwingUtilities.invokeLater(() ->
				{
					timeSlider.setMaximum((int) newLength);
					totalTimeLabel.setText(formatTime(newLength));
					timeSlider.setEnabled(true);
				});
			}
		};
	}

	private MouseAdapter getMouseAdapter()
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (mediaPlayerComponent.mediaPlayer().media().isValid())
				{
					togglePlay();
				}
			}
		};
	}

	private void updateTimeSlider()
	{
		if (timeSlider.getValueIsAdjusting())
		{
			isSeeking.set(true);
		}
		else if (isSeeking.get())
		{
			long newTime = timeSlider.getValue();
			mediaPlayerComponent.mediaPlayer().controls().setTime(newTime);
			isSeeking.set(false);
			notifyTimeChanged();
		}
	}

	public void setText(String text)
	{
		videoSurface.setText(text);
	}

	public BufferedImage getImage()
	{
		return mediaPlayerComponent.mediaPlayer().snapshots().get();
	}

	public void loadStream(String url)
	{
		if (!url.isEmpty())
		{
			setText("URL Found. Loading video...");
			try
			{
				mediaPlayerComponent.mediaPlayer().media().play(url);
				if (mediaPlayerComponent.mediaPlayer().media().isValid())
				{
					setPlaying();
				}
				else
				{
					JOptionPane.showMessageDialog(this, "Please load a valid url", "SpaceX Stream Parser", JOptionPane.ERROR_MESSAGE);
				}
			}
			catch (Exception e)
			{
				System.out.println("Failed to play URL: " + url);
			}
		}
	}

	public boolean isMediaValid()
	{
		return mediaPlayerComponent.mediaPlayer().media().isValid();
	}

	public void togglePlay()
	{
		if (mediaPlayerComponent.mediaPlayer().media().isValid())
		{
			if (isPlaying.get())
			{
				pauseStream();
			}
			else
			{
				mediaPlayerComponent.mediaPlayer().controls().setPause(false);
				setPlaying();
			}
		}
		else
		{
			JOptionPane.showMessageDialog(this, "Please load a url first", "SpaceX Stream Parser", JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean isPlaying()
	{
		return isPlaying.get();
	}

	private void pauseStream()
	{
		if (mediaPlayerComponent != null)
		{
			mediaPlayerComponent.mediaPlayer().controls().setPause(true);
			setPaused();
		}
	}

	private void toggleMute()
	{
		muted = !muted;
		mediaPlayerComponent.mediaPlayer().audio().setVolume((muted ? 0 : 100));
		muteButton.setIcon(muted ? IconManager.get(MUTED) : IconManager.get(UNMUTED));
	}

	private void setPlaying()
	{
		isPlaying.set(true);
		playButton.setIcon(IconManager.get(PAUSE));
	}

	private void setPaused()
	{
		isPlaying.set(false);
		playButton.setIcon(IconManager.get(PLAY));
	}

	public static String formatTime(long timeInMillis)
	{
		long totalSeconds = timeInMillis / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	public void enableBuffer(int ms)
	{
		videoSurface.enableBuffer(ms);
	}

	public void disableBuffer()
	{
		videoSurface.disableBuffer();
	}

	public Dimension getVideoDimensions()
	{
		return videoSurface.getLastBufferDimension();
	}

	public VideoSurface videoSurface()
	{
		return videoSurface;
	}

	public void notifyDimensionsChanged()
	{
		for(VideoPlayerChangedListener listener : dimensionChangedListeners)
		{
			listener.notifyVideoPlayerChanged();
		}
	}

	public void notifyTimeChanged()
	{
		for(VideoPlayerChangedListener listener : timeChangedListeners)
		{
			listener.notifyVideoPlayerChanged();
		}
	}

	public void addMediaDimensionChangedListener(VideoPlayerChangedListener listener)
	{
		dimensionChangedListeners.add(listener);
	}

	public void removeTimeChangedListener(VideoPlayerChangedListener listener)
	{
		dimensionChangedListeners.remove(listener);
	}

	public void addTimeChangedListener(VideoPlayerChangedListener listener)
	{
		timeChangedListeners.add(listener);
	}

	public void removeMediaDimensionChangedListener(VideoPlayerChangedListener listener)
	{
		timeChangedListeners.remove(listener);
	}

	public void notifyFrameChanged(BufferedImage imageCopy, long playerTime, long systemTime)
	{
		for(VideoPlayerFrameChangedListener listener : videoPlayerFrameChangedListeners)
		{
			listener.onFrameChanged(imageCopy, playerTime, systemTime);
		}
	}

	public void addVideoPlayerFrameChangedListener(VideoPlayerFrameChangedListener listener)
	{
		videoPlayerFrameChangedListeners.add(listener);
	}

	public void removeVideoPlayerFrameChangedListener(VideoPlayerFrameChangedListener listener)
	{
		videoPlayerFrameChangedListeners.remove(listener);
	}
}
