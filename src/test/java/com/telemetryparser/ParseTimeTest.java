package com.telemetryparser;

import static com.telemetryparser.util.Util.parseTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseTimeTest
{

	@Test
	void testParseTime()
	{
		// 3-part (hours:minutes:seconds)
		Assertions.assertEquals("T+12:34:56", parseTime("+12:34:56", "T"));
		Assertions.assertEquals("T+12:34:56", parseTime("12:34:56", "T"));
		Assertions.assertEquals("T+01:34:56", parseTime("1:34:56", "T"));
		Assertions.assertEquals("T-12:34:56", parseTime("-12:34:56", "T"));
		Assertions.assertEquals("T-01:34:56", parseTime("-1:34:56", "T"));

		// 2-part (minutes:seconds)
		Assertions.assertEquals("T+00:34:56", parseTime("34:56", "T"));
		Assertions.assertEquals("T+00:04:56", parseTime("4:56", "T"));
		Assertions.assertEquals("T+00:04:56", parseTime("+4:56", "T"));
		Assertions.assertEquals("T-00:04:56", parseTime("-4:56", "T"));

		// 1-part (seconds only)
		Assertions.assertEquals("T+00:00:08", parseTime("8", "T"));
		Assertions.assertEquals("T+00:00:08", parseTime("+8", "T"));
		Assertions.assertEquals("T-00:00:08", parseTime("-8", "T"));

		// With explicit 'T' prefix in the string
		Assertions.assertEquals("T+01:02:03", parseTime("T+1:2:3", "T"));
		Assertions.assertEquals("T-01:02:03", parseTime("T-1:2:3", "T"));
		Assertions.assertEquals("T+09:10:11", parseTime("T9:10:11", "T"));
		Assertions.assertEquals("T+00:09:10", parseTime("T9:10", "T"));
		Assertions.assertEquals("T+00:00:05", parseTime("T5", "T"));
		// Negative with 'T' prefix
		Assertions.assertEquals("T-12:34:56", parseTime("T-12:34:56", "T"));
		Assertions.assertEquals("T-00:09:05", parseTime("T-9:05", "T"));
		Assertions.assertEquals("T-00:00:03", parseTime("T-3", "T"));

		Assertions.assertEquals("unknown", parseTime("", "T"));
		Assertions.assertEquals("unknown", parseTime(null, "T"));
		Assertions.assertEquals("unknown", parseTime("T+abc", "T"));
		Assertions.assertEquals("unknown", parseTime("abcdef", "T"));

		Assertions.assertEquals("T-12:34:56", parseTime("-12:34:56", "T"));
		// H:MM:SS with -
		Assertions.assertEquals("T-01:34:56", parseTime("-1:34:56", "T"));
		// MM:SS with -
		Assertions.assertEquals("T-00:34:56", parseTime("-34:56", "T"));
		// M:SS with -
		Assertions.assertEquals("T-00:04:56", parseTime("-4:56", "T"));

		// T-H:MM:SS
		Assertions.assertEquals("T-01:34:56", parseTime("T-1:34:56", "T"));
		// T-MM:SS
		Assertions.assertEquals("T-00:34:56", parseTime("T-34:56", "T"));
		// T-M:SS
		Assertions.assertEquals("T-00:04:56", parseTime("T-4:56", "T"));

		Assertions.assertEquals("T+00:00:08", parseTime("+8", "T"));
		Assertions.assertEquals("T-00:00:08", parseTime("-8", "T"));

		// no sign => + by default
		Assertions.assertEquals("T+00:00:05", parseTime("5", "T"));

		// with prefix
		Assertions.assertEquals("T+00:00:05", parseTime("T+5", "T"));
		Assertions.assertEquals("T-00:00:02", parseTime("T-2", "T"));
		Assertions.assertEquals("T+00:00:02", parseTime("T2", "T"));
	}

}
