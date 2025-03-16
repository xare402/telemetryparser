package com.telemetryparser.util;

public class Util
{
	public static String parseTime(String text, String timePrefix)
	{
		if (text == null || text.isEmpty())
		{
			return "unknown";
		}

		text = text.replace('O', '0').replace('I', '1').replace('|', '1');


		String allowed = "\\d+-:\\s";
		if (timePrefix != null && !timePrefix.isEmpty())
		{
			String escapedPrefix = java.util.regex.Pattern.quote(timePrefix);
			allowed += escapedPrefix;
		}
		text = text.replaceAll("[^" + allowed + "]", "");

		String prefixPart = (timePrefix != null && !timePrefix.isEmpty()) ? "(?:" + java.util.regex.Pattern.quote(timePrefix) + ")?" : "";
		String regex = "^\\s*"
			+ prefixPart
			+ "([+\\-])?"
			+ "(\\d{1,2})"
			+ "(?::(\\d{1,2}))?"
			+ "(?::(\\d{1,2}))?"
			+ "\\s*$";

		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
		java.util.regex.Matcher matcher = pattern.matcher(text);

		if (!matcher.matches())
		{
			return "unknown";
		}

		String sign = matcher.group(1);
		String part1 = matcher.group(2);
		String part2 = matcher.group(3);
		String part3 = matcher.group(4);

		if (sign == null)
		{
			sign = "+";
		}

		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		if (part2 == null && part3 == null)
		{
			seconds = Integer.parseInt(part1);
		}
		else if (part3 == null)
		{
			minutes = Integer.parseInt(part1);
			seconds = Integer.parseInt(part2);
		}
		else
		{
			hours = Integer.parseInt(part1);
			minutes = Integer.parseInt(part2);
			seconds = Integer.parseInt(part3);
		}

		return String.format("T%s%02d:%02d:%02d", sign, hours, minutes, seconds);
	}

	public static int determineSeconds(String time)
	{
		if (time == null || time.length() != 10 || !time.startsWith("T"))
		{
			throw new IllegalArgumentException("Invalid time format. Expected format: T±HH:MM:SS");
		}

		char sign = time.charAt(1);
		int hours = Integer.parseInt(time.substring(2, 4));
		int minutes = Integer.parseInt(time.substring(5, 7));
		int seconds = Integer.parseInt(time.substring(8, 10));

		int totalSeconds = hours * 3600 + minutes * 60 + seconds;

		if (sign == '-')
		{
			totalSeconds = -totalSeconds;
		}
		else if (sign != '+')
		{
			throw new IllegalArgumentException("Sign must be '+' or '-'. Found: " + sign);
		}
		return totalSeconds;
	}
}
