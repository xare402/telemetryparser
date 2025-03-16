package com.telemetryparser.util;

import java.awt.image.BufferedImage;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class TesseractManager
{
	private static final String DEFAULT_WHITELIST = "0123456789+-:T.";
	private static final int poolSize = 128;
	private static final String dataPath = "tessdata";
	private static final String language = "eng";
	private static final ReentrantLock reinitLock = new ReentrantLock();
	private static final Condition canBorrow = reinitLock.newCondition();
	private static final Condition canReinit = reinitLock.newCondition();
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static BlockingQueue<ITesseract> pool;
	private static String whitelist = DEFAULT_WHITELIST;
	private static int borrowedCount = 0;
	private static boolean reinitInProgress = false;

	static
	{
		initializeManager(poolSize, dataPath, language, whitelist);
	}

	public static void initializeManager(int size, String datapath, String lang, String wl)
	{
		try
		{
			pool = new ArrayBlockingQueue<>(size);

			for (int i = 0; i < size; i++)
			{
				ITesseract tesseract = createTesseractInstance(datapath, lang, wl);
				pool.put(tesseract);
			}

			System.out.println("Tesseract Manager initialized with whitelist: " + wl);

		}
		catch (Exception e)
		{
			System.err.println("Tesseract manager failed to initialize: " + e.getMessage());
		}
	}

	private static ITesseract createTesseractInstance(String datapath, String lang, String wl)
	{
		Tesseract tesseract = new Tesseract();

		tesseract.setVariable("debug_file", "/dev/null");
		tesseract.setDatapath(datapath);
		tesseract.setLanguage(lang);
		tesseract.setPageSegMode(7);
		tesseract.setOcrEngineMode(1);

		tesseract.setVariable("enable_new_segsearch", "0");
		tesseract.setVariable("load_system_dawg", "0");
		tesseract.setVariable("load_freq_dawg", "0");
		tesseract.setVariable("tessedit_char_whitelist", wl);

		return tesseract;
	}

	private static ITesseract borrow() throws InterruptedException
	{
		reinitLock.lock();
		try
		{
			while (reinitInProgress)
			{
				canBorrow.await();
			}

			ITesseract instance = pool.take();
			borrowedCount++;
			return instance;
		}
		finally
		{
			reinitLock.unlock();
		}
	}

	private static void release(ITesseract tesseract) throws InterruptedException
	{
		reinitLock.lock();
		try
		{
			pool.put(tesseract);
			borrowedCount--;

			if (reinitInProgress && borrowedCount == 0)
			{
				canReinit.signal();
			}
		}
		finally
		{
			reinitLock.unlock();
		}
	}

	public static String ocr(BufferedImage image)
	{
		if (image == null)
		{
			return "";
		}

		ITesseract tesseract = null;
		try
		{
			tesseract = borrow();
			return tesseract.doOCR(image).trim();
		}
		catch (Exception e)
		{
			System.err.println("Could not do OCR: " + e.getMessage());
			return "";
		}
		finally
		{
			if (tesseract != null)
			{
				try
				{
					release(tesseract);
				}
				catch (InterruptedException ie)
				{
					System.err.println("Failed to release Tesseract instance");
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public static void changeTimePrefixAsync(String text)
	{
		executor.submit(() ->
		{
			if (text == null || text.isEmpty())
			{
				return;
			}

			StringBuilder newChars = getCharactersNotInWhitelist(text);

			if (newChars.isEmpty())
			{
				return;
			}

			reinitLock.lock();
			try
			{
				String extended = DEFAULT_WHITELIST + newChars;

				if (extended.equals(whitelist))
				{
					return;
				}

				reinitInProgress = true;

				while (borrowedCount > 0)
				{
					canReinit.await();
				}

				whitelist = extended;
				initializeManager(poolSize, dataPath, language, whitelist);

				reinitInProgress = false;
				canBorrow.signalAll();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			finally
			{
				reinitLock.unlock();
			}
		});
	}

	private static StringBuilder getCharactersNotInWhitelist(String text)
	{
		StringBuilder newChars = new StringBuilder();
		for (char c : text.toCharArray())
		{
			if (DEFAULT_WHITELIST.indexOf(c) < 0)
			{
				if (newChars.indexOf(String.valueOf(c)) < 0)
				{
					newChars.append(c);
				}
			}
		}
		return newChars;
	}
}
