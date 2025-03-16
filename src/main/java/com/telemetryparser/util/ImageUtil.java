package com.telemetryparser.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.imageio.ImageIO;

public class ImageUtil
{
	public static BufferedImage scaleUpImage(BufferedImage source, double scale)
	{
		int newWidth = (int) (source.getWidth() * scale);
		int newHeight = (int) (source.getHeight() * scale);

		BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = scaled.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(source, 0, 0, newWidth, newHeight, null);
		g2d.dispose();

		return scaled;
	}

	public static BufferedImage unsharpMask(BufferedImage source, double radius, double amount, int threshold)
	{
		BufferedImage blurred = gaussianBlur(source, radius);
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());

		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int srcRGB = source.getRGB(x, y) & 0xFF;
				int blurRGB = blurred.getRGB(x, y) & 0xFF;

				int diff = srcRGB - blurRGB;
				if (Math.abs(diff) < threshold)
				{
					diff = 0;
				}

				int newVal = (int) Math.round(srcRGB + (amount * diff));
				newVal = Math.min(255, Math.max(0, newVal));
				int grayRGB = (newVal << 16) | (newVal << 8) | newVal;
				result.setRGB(x, y, grayRGB);
			}
		}

		return result;
	}

	public static float[] createGaussianKernel(int size, double radius)
	{
		float[] data = new float[size * size];
		if (radius <= 0)
		{
			data[size * size / 2] = 1f;
			return data;
		}

		double sigma = radius / 3.0;
		double mean = size / 2.0;
		double sum = 0.0;

		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				double dx = x - mean;
				double dy = y - mean;
				double value = Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
				data[y * size + x] = (float) value;
				sum += value;
			}
		}

		for (int i = 0; i < data.length; i++)
		{
			data[i] /= (float) sum;
		}

		return data;
	}

	public static BufferedImage gaussianBlur(BufferedImage source, double radius)
	{
		int kernelSize = (int) Math.ceil(radius * 3);
		if (kernelSize < 1)
		{
			kernelSize = 1;
		}
		if (kernelSize % 2 == 0)
		{
			kernelSize++;
		}

		float[] kernelData = createGaussianKernel(kernelSize, radius);
		Kernel kernel = new Kernel(kernelSize, kernelSize, kernelData);

		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		BufferedImage blurred = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		op.filter(source, blurred);

		return blurred;
	}

	public static BufferedImage thresholdBlackWhite(BufferedImage source, int threshold)
	{
		BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int rgb = source.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = (rgb) & 0xFF;
				if (r > threshold && g > threshold && b > threshold)
				{
					output.setRGB(x, y, 0xFFFFFF);
				}
				else
				{
					output.setRGB(x, y, 0x000000);
				}
			}
		}
		return output;
	}

	public static BufferedImage extractROI(BufferedImage frame, ROIRatios roiRatios)
	{
		if (roiRatios == null)
		{
			return null;
		}

		int x = (int) (roiRatios.xRatio * frame.getWidth());
		int y = (int) (roiRatios.yRatio * frame.getHeight());
		int width = (int) (roiRatios.widthRatio * frame.getWidth());
		int height = (int) (roiRatios.heightRatio * frame.getHeight());

		x = Math.max(0, Math.min(x, frame.getWidth() - 1));
		y = Math.max(0, Math.min(y, frame.getHeight() - 1));
		width = Math.min(width, frame.getWidth() - x);
		height = Math.min(height, frame.getHeight() - y);

		if (width <= 0 || height <= 0)
		{
			return null;
		}

		return frame.getSubimage(x, y, width, height);
	}

	private static int count = 0;
	private static void saveImage(String name, BufferedImage image)
	{
		try
		{
			File outputFile = new File(name + count + ".png");
			count++;
			ImageIO.write(image, "png", outputFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static EngineData computeEngineBounds(BufferedImage engineRegionImage, int width, int height, long initialX, long initialY)
	{
		BufferedImage thresholdedImage = ImageUtil.thresholdBlackWhite(engineRegionImage, 100);

		Rectangle bounds = computeNonBlackRegion(thresholdedImage);
		ROIRatios ratios = new ROIRatios((double) (bounds.x + initialX) / width, (double) (bounds.y + initialY) / height, (double) (bounds.width) / width, (double) (bounds.height) / height);
		BufferedImage croppedImage = thresholdedImage.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
		List<EngineLocation> engineLocations = findEngineLocations(croppedImage);
		Map<Integer, EngineLocation> engineMap = findEngineRingsAndOrder(engineLocations);

		return new EngineData(ratios, engineMap);
	}

	public static Map<Integer, EngineLocation> findEngineRingsAndOrder(List<EngineLocation> engines)
	{
		Map<Integer, EngineLocation> resultMap = new LinkedHashMap<>();
		if (engines == null || engines.isEmpty())
		{
			return resultMap;
		}

		double sumX = 0;
		double sumY = 0;
		for (EngineLocation e : engines)
		{
			sumX += e.middle().getX();
			sumY += e.middle().getY();
		}
		double centerX = sumX / engines.size();
		double centerY = sumY / engines.size();

		List<DistEngine> distList = new ArrayList<>(engines.size());
		for (EngineLocation e : engines)
		{
			double dx = e.middle().getX() - centerX;
			double dy = e.middle().getY() - centerY;
			double dist = Math.sqrt(dx * dx + dy * dy);
			distList.add(new DistEngine(e, dist));
		}

		distList.sort(Comparator.comparingDouble(d -> d.distance));

		List<Integer> boundaries = getBoundaries(distList);

		List<List<DistEngine>> rings = new ArrayList<>();
		int startIdx = 0;
		for (int boundary : boundaries)
		{
			rings.add(distList.subList(startIdx, boundary + 1));
			startIdx = boundary + 1;
		}
		rings.add(distList.subList(startIdx, distList.size()));

		int globalIndex = 1;
		int ringNumber = 1;
		for (List<DistEngine> ring : rings)
		{
			if (ring.isEmpty())
			{
				continue;
			}

			double ringMinY = ring.stream().mapToDouble(de -> de.location.middle().getY()).min().orElse(Double.POSITIVE_INFINITY);

			double yThreshold = ringMinY + 3.0;
			List<DistEngine> topCandidates = ring.stream().filter(de -> de.location.middle().getY() <= yThreshold).toList();

			DistEngine ringStart = topCandidates.stream().max(Comparator.comparingDouble(de -> de.location.middle().getX())).orElse(ring.getFirst());

			for (DistEngine de : ring)
			{
				double x = de.location.middle().getX();
				double y = de.location.middle().getY();
				de.angle = computeClockwiseAngleFromTop(centerX, centerY, x, y);
			}

			double startAngle = ringStart.angle;
			for (DistEngine de : ring)
			{
				de.angle = (de.angle - startAngle + 2 * Math.PI) % (2 * Math.PI);
			}
			ring.sort(Comparator.comparingDouble(de -> de.angle));

			for (DistEngine de : ring)
			{
				resultMap.put(globalIndex++, new EngineLocation(de.location.middle(), de.location.radius(), ringNumber));
			}
			ringNumber++;
		}
		return resultMap;
	}

	private static List<Integer> getBoundaries(List<DistEngine> distList)
	{
		double minDist = distList.getFirst().distance;
		double maxDist = distList.getLast().distance;
		double range = maxDist - minDist;

		double gapThreshold = 0.1 * range;

		List<Integer> boundaries = new ArrayList<>();
		for (int i = 0; i < distList.size() - 1; i++)
		{
			double gap = distList.get(i + 1).distance - distList.get(i).distance;
			if (gap > gapThreshold)
			{
				boundaries.add(i);
			}
		}
		return boundaries;
	}

	private static double computeClockwiseAngleFromTop(double centerX, double centerY, double x, double y)
	{
		double dx = x - centerX;
		double dyFlipped = -(y - centerY);
		double standardAngle = Math.atan2(dyFlipped, dx);
		double angleFromTop = (Math.PI / 2) - standardAngle;
		return (angleFromTop + 2 * Math.PI) % (2 * Math.PI);
	}

	record RadiusPoint(int x, int y, int radius) {}

	public static List<EngineLocation> findEngineLocations(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		boolean[][] visited = new boolean[width][height];

		List<EngineLocation> locations = new ArrayList<>();

		boolean shouldUseFloodFillMethod = !containsLargeBlob(image);
		
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (isWhite(image.getRGB(x, y)) && !visited[x][y])
				{
					if(shouldUseFloodFillMethod)
					{
						List<Point> blobPixels = bfsCollectBlob(image, x, y, visited, Color.WHITE, true);
						List<EngineLocation> engines = splitCollectedBlobs(blobPixels, width, height);
						locations.addAll(engines);
					}
					else
					{
						List<Point> blobPixels = dynamicCollectBlob(image, x, y, visited, Color.WHITE);
						RadiusPoint radiusPoint = computeCentroidAndRadius(blobPixels);
						locations.add(new EngineLocation(new Point(radiusPoint.x, radiusPoint.y), radiusPoint.radius, null));
					}
				}
			}
		}
		return locations;
	}

	private static RadiusPoint computeCentroidAndRadius(List<Point> blobPixels)
	{
		double maxDist = 0;
		double sumX = 0;
		double sumY = 0;
		for (Point p : blobPixels)
		{
			sumX += p.x;
			sumY += p.y;
		}
		double centerX = sumX / blobPixels.size();
		double centerY = sumY / blobPixels.size();

		for (Point p : blobPixels)
		{
			double dx = p.x - centerX;
			double dy = p.y - centerY;
			double dist = Math.sqrt(dx * dx + dy * dy);
			if (dist > maxDist)
			{
				maxDist = dist;
			}
		}
		return new RadiusPoint((int) Math.round(centerX), (int) Math.round(centerY), (int) Math.round(maxDist));
	}

	public static boolean containsLargeBlob(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		for (int x = 0; x <= width - 5; x++)
		{
			for (int y = 0; y <= height - 5; y++)
			{
				if (is5x5White(image, x, y))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean is5x5White(BufferedImage image, int startX, int startY)
	{
		for (int dx = 0; dx < 5; dx++)
		{
			for (int dy = 0; dy < 5; dy++)
			{
				int rgb = image.getRGB(startX + dx, startY + dy);
				if (!isWhite(rgb))
				{
					return false;
				}
			}
		}
		return true;
	}

	private static List<EngineLocation> splitCollectedBlobs(List<Point> blobPixels, int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, width, height);
		g2.dispose();

		for (Point p : blobPixels)
		{
			image.setRGB(p.x, p.y, Color.WHITE.getRGB());
		}

		boolean[][] visited = new boolean[width][height];

		floodFillBlackToWhite(image, 0, 0, visited);
		floodFillBlackToWhite(image, 0, height-1, visited);
		floodFillBlackToWhite(image, width-1, 0, visited);
		floodFillBlackToWhite(image, width-1, height-1, visited);

		visited = new boolean[width][height];
		List<EngineLocation> engineLocations = new ArrayList<>();

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (!visited[x][y] && isBlack(image.getRGB(x, y)))
				{
					List<Point> blackBlob = collectBlackBlob(image, x, y, visited);

					RadiusPoint radiusPoint = computeCentroidAndRadius(blackBlob);


					EngineLocation location = new EngineLocation(new Point(radiusPoint.x, radiusPoint.y), radiusPoint.radius, null);

					if(radiusPoint.radius > 7)
					{
						engineLocations.add(location);
					}
				}
			}
		}

		return engineLocations;
	}

	private static void floodFillBlackToWhite(BufferedImage image, int startX, int startY, boolean[][] visited)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		Queue<Point> queue = new LinkedList<>();
		queue.add(new Point(startX, startY));
		visited[startX][startY] = true;

		while (!queue.isEmpty())
		{
			Point current = queue.poll();
			image.setRGB(current.x, current.y, Color.WHITE.getRGB());
			visitRegion(image, visited, width, height, queue, current);
		}
	}

	private static void visitRegion(BufferedImage image, boolean[][] visited, int width, int height, Queue<Point> queue, Point current)
	{
		int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };
		for (int[] d : dirs)
		{
			int nx = current.x + d[0];
			int ny = current.y + d[1];
			if (nx >= 0 && nx < width && ny >= 0 && ny < height)
			{
				if (!visited[nx][ny] && isBlack(image.getRGB(nx, ny)))
				{
					visited[nx][ny] = true;
					queue.add(new Point(nx, ny));
				}
			}
		}
	}

	private static List<Point> collectBlackBlob(BufferedImage image, int startX, int startY, boolean[][] visited)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		List<Point> blob = new ArrayList<>();
		Queue<Point> queue = new LinkedList<>();

		queue.add(new Point(startX, startY));
		visited[startX][startY] = true;

		while (!queue.isEmpty())
		{
			Point current = queue.poll();
			blob.add(current);

			visitRegion(image, visited, width, height, queue, current);
		}
		return blob;
	}

	private static boolean isBlack(int rgb)
	{
		return (rgb == Color.BLACK.getRGB());
	}

	private static List<Point> dynamicCollectBlob(BufferedImage image, int startX, int startY, boolean[][] visited, Color target)
	{
		boolean[][] mask = new boolean[visited.length][visited[0].length];
		List<Point> blobPixels = bfsCollectBlob(image, startX, startY, mask, target, true);

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		for (Point p : blobPixels)
		{
			minX = Math.min(minX, p.x);
			maxX = Math.max(maxX, p.x);
		}
		int diameter = maxX - minX;

		if (blobPixels.size() > diameter * Math.PI * 1.5)
		{
			visited[startX][startY] = true;
			blobPixels = bfsCollectBlob(image, startX, startY, visited, target, false);

		}
		else
		{
			for(int y = 0; y < mask[0].length; y++)
			{
				for(int x = 0; x < mask.length; x++)
				{
					if(mask[x][y])
					{
						visited[x][y] = true;
					}
				}
			}
		}

		return blobPixels;
	}

	private static List<Point> bfsCollectBlob(BufferedImage image, int startX, int startY, boolean[][] visited, Color targetColor, boolean includeDiagonals)
	{
		int[][] directions4 = {{0,1}, {0,-1}, {1,0}, {-1,0}};
		int[][] directions8 = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
		int[][] directions = includeDiagonals ? directions8 : directions4;

		List<Point> blobPixels = new ArrayList<>();
		Queue<Point> queue = new LinkedList<>();

		queue.add(new Point(startX, startY));
		visited[startX][startY] = true;

		while (!queue.isEmpty())
		{
			Point current = queue.poll();
			blobPixels.add(current);

			for (int[] dir : directions)
			{
				int nx = current.x + dir[0];
				int ny = current.y + dir[1];

				if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight())
				{
					if (!visited[nx][ny] && matchesColor(image.getRGB(nx, ny), targetColor))
					{
						visited[nx][ny] = true;
						queue.add(new Point(nx, ny));
					}
				}
			}
		}

		return blobPixels;
	}

	private static boolean matchesColor(int rgb, Color targetColor)
	{
		return (rgb == targetColor.getRGB());
	}

	private static boolean isWhite(int rgb)
	{
		Color c = new Color(rgb);
		return c.getRed() == 255 && c.getGreen() == 255 && c.getBlue() == 255;
	}

	public static ROIRatios computeFuelRegion(BufferedImage fuelRegionImage, int width, int height, long initialX, long initialY)
	{
		Rectangle bounds = computeNonBlackRegion(fuelRegionImage);
		return new ROIRatios((double) (bounds.x + initialX) / width, (double) (bounds.y + initialY) / height, (double) (bounds.width) / width, (double) (bounds.height) / height);
	}

	public static Rectangle computeNonBlackRegion(BufferedImage fuelRegionImage)
	{
		int firstX = fuelRegionImage.getWidth();
		int firstY = fuelRegionImage.getHeight();
		int lastX = 0;
		int lastY = 0;

		int baseColor = fuelRegionImage.getRGB(0, 0);
		int baseR = (baseColor >> 16) & 0xFF;
		int baseG = (baseColor >> 8) & 0xFF;
		int baseB = baseColor & 0xFF;

		for (int y = 0; y < fuelRegionImage.getHeight(); y++)
		{
			for (int x = 0; x < fuelRegionImage.getWidth(); x++)
			{
				int rgb = fuelRegionImage.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;

				boolean isNearBase = (Math.abs(r - baseR) < 10 && Math.abs(g - baseG) < 10 && Math.abs(b - baseB) < 10);

				if (!isNearBase)
				{
					firstX = Math.min(firstX, x);
					firstY = Math.min(firstY, y);
					lastX = Math.max(lastX, x);
					lastY = Math.max(lastY, y);
				}
			}
		}

		lastX++;
		lastY++;

		if (firstX > lastX || firstY > lastY)
		{
			return new Rectangle(0, 0, 1, 1);
		}
		return new Rectangle(firstX, firstY, lastX-firstX, lastY-firstY);
	}

	public static boolean checkForSuspiciousTextImage(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		boolean whiteOnEdge = false;

		for (int x = 0; x < width; x++)
		{
			if (isWhite(image.getRGB(x, 0)) || isWhite(image.getRGB(x, height - 1)))
			{
				whiteOnEdge = true;
				break;
			}
		}
		if (!whiteOnEdge)
		{
			for (int y = 0; y < height; y++)
			{
				if (isWhite(image.getRGB(0, y)) || isWhite(image.getRGB(width - 1, y)))
				{
					whiteOnEdge = true;
					break;
				}
			}
		}

		int topMostWhite = -1;
		int bottomMostWhite = -1;

		outerTop:
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (isWhite(image.getRGB(x, y)))
				{
					topMostWhite = y;
					break outerTop;
				}
			}
		}

		outerBottom:
		for (int y = height - 1; y >= 0; y--)
		{
			for (int x = 0; x < width; x++)
			{
				if (isWhite(image.getRGB(x, y)))
				{
					bottomMostWhite = y;
					break outerBottom;
				}
			}
		}

		boolean topBottomCheck = false;
		if (topMostWhite != -1 && bottomMostWhite != -1)
		{
			boolean topOutside = topMostWhite > 0.3 * height;
			boolean bottomOutside = bottomMostWhite < 0.7 * height;
			if (topOutside || bottomOutside)
			{
				topBottomCheck = true;
			}
		}

		return whiteOnEdge || topBottomCheck;
	}


	private static class DistEngine
	{
		EngineLocation location;
		double distance;
		double angle;

		DistEngine(EngineLocation loc, double dist)
		{
			this.location = loc;
			this.distance = dist;
		}
	}
}
