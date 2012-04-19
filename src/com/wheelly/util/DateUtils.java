package com.wheelly.util;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class DateUtils {
	public static final String DateFormat = "yyyy-MM-dd";
	public static final String TimeFormat = "HH:mm:ss";
	public static final String DateTimeFormat = DateFormat + " " + TimeFormat;

	public final static SimpleDateFormat dbFormat = new SimpleDateFormat(DateTimeFormat);
	
	final static Format todayFormat = new SimpleDateFormat("HH:mm");
	final static Format yearFormat = new SimpleDateFormat("d MMM");
	final static Format otherFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	final static Calendar today = Calendar.getInstance();
	
	public static String formatVarying(String date) {
		try {
			final Calendar c = Calendar.getInstance();
			c.setTime(dbFormat.parse(date));
			final Format f =
				c.get(Calendar.YEAR) == today.get(Calendar.YEAR)
					? c.get(Calendar.MONTH) == today.get(Calendar.MONTH)
						&& c.get(Calendar.DATE) == today.get(Calendar.DATE)
						? todayFormat
						: yearFormat
					: otherFormat;
			
			return f.format(c.getTime());
		} catch (ParseException e) {
			return date;
		}
	}
}