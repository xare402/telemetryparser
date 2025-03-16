package com.telemetryparser.core.fixed;

import com.telemetryparser.core.Parameter;
import com.telemetryparser.core.ParameterType;
import com.telemetryparser.util.ImageUtil;
import com.telemetryparser.util.Util;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FrameProcessor
{
	private final FrameData queuedFrame;
	public FrameProcessor(FrameData frame)
	{
		this.queuedFrame = frame;
	}

	public FrameAnalysisResult compute()
	{
		Map<Parameter, CVResult> parameterMap = new LinkedHashMap<>();
		try
		{
			BufferedImage frame = queuedFrame.frame();
			Map<Parameter, CompletableFuture<CVResult>> futureMap = new LinkedHashMap<>();
			for (Parameter parameter : Parameter.values())
			{
				BufferedImage image = ImageUtil.extractROI(frame, queuedFrame.preset().ratios().get(parameter));

				CompletableFuture<CVResult> future = switch (parameter.type)
				{
					case FUEL -> CVFuelResult.fromImageAsync(image, frame.getWidth());
					case ENGINES -> switch (parameter)
					{
						case STAGE_1_ENGINES -> CVEngineResult.fromImageAsync(image, queuedFrame.preset().stage1EngineMap(), -1);
						case STAGE_2_ENGINES -> CVEngineResult.fromImageAsync(image, queuedFrame.preset().stage2EngineMap(), -1);
						default -> null;
					};
					case PITCH -> CVOrientationResult.fromImageAsync(image, parameter);
					case ENGINES_VARIANT -> null;
					default -> CVTextResult.fromImageAsync(image);
				};

				if (future != null)
				{
					futureMap.put(parameter, future);
				}
			}

			Double seconds = null;
			if(futureMap.get(Parameter.TIME).get() instanceof CVTextResult timeResult)
			{
				String time = Util.parseTime(timeResult.getResult(), queuedFrame.preset().timePrefix());
				try
				{
					seconds = (double) Util.determineSeconds(time);
				}
				catch (Exception ignored)
				{

				}
			}

			if(seconds != null && seconds >= -6)
			{
				for (Parameter parameter : futureMap.keySet())
				{
					CVResult result = futureMap.get(parameter).get();
					if(result instanceof CVTextResult textResult)
					{
						if(parameter.type != ParameterType.TIME)
						{
							if(queuedFrame.preset().usesComma())
							{
								textResult.replaceComma();
							}
						}
						else
						{
							textResult.parseAsTime(queuedFrame.preset().timePrefix());
						}
						if(parameter == Parameter.TIME || !ImageUtil.checkForSuspiciousTextImage(result.getProcessedImage()))
						{
							parameterMap.put(parameter, result);
						}
					}
					else
					{
						parameterMap.put(parameter, result);
					}

				}
			}
		}
		catch (Exception ignored)
		{

		}

		return new FrameAnalysisResult(queuedFrame, parameterMap);
	}
}
