package com.wheelly.util;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtils {
	public static final String DateFormat = "yyyy-MM-dd";
	public static final String TimeFormat = "HH:mm:ss";
	public static final String DateTimeFormat = DateFormat + " " + TimeFormat;

	public final static SimpleDateFormat dbFormat = new SimpleDateFormat(DateTimeFormat);
	
	final static Format todayFormat = new SimpleDateFormat("HH:mm");
	final static Format yearFormat = new SimpleDateFormat("d MMM");
	final static Format otherFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	final static Date today = new Date();
	
	public static String formatVarying(String date) {
		try {
			final Date d = dbFormat.parse(date);
			final Format f =
				d.getYear() == today.getYear()
					? d.getMonth() == today.getMonth()
						&& d.getDate() == today.getDate()
						? todayFormat
						: yearFormat
					: otherFormat;
			
			return f.format(d);
		} catch (ParseException e) {
			return date;
		}
	}
}
