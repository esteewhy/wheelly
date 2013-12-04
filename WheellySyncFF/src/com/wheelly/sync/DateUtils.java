package com.wheelly.sync;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	
	private static final DateFormat FORMAT_TIMESTAMP_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static long parseDate(String date) {
		try {
			return FORMAT_TIMESTAMP_ISO_8601.parse(date).getTime() / 1000;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			return 0;
		}
	}
	
	public static String toDate(long date) {
		return FORMAT_TIMESTAMP_ISO_8601.format(new Date(date));
	}
}