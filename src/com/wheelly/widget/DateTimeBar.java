package com.wheelly.widget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.wheelly.R;
import com.wheelly.db.DatabaseSchema;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public final class DateTimeBar extends LinearLayout {
	
	//controls
	private DateButton dateButton;
	private TimeButton timeButton;
	
	public DateTimeBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	public DateTimeBar(Context context) {
		super(context);
		initialize(context);
	}

	private void initialize(Context context) {
		LayoutInflater.from(context).inflate(R.layout.datetimebar, this, true);
		
		dateButton = (DateButton)findViewById(R.id.date);
		timeButton = (TimeButton)findViewById(R.id.time);
	}
	
	public String getDateTime() {
		final long date = dateButton.getDate();
		final long time = timeButton.getTime();
		
		return
			new SimpleDateFormat(DatabaseSchema.DateFormat).format(new Date(date))
			+ " "
			+ new SimpleDateFormat(DatabaseSchema.TimeFormat).format(new Date(time));
	}
	
	public void setDateTime(String dateTime) {
		try {
			long millis = dateTime != null
				? new SimpleDateFormat(DatabaseSchema.DateTimeFormat).parse(dateTime).getTime()
				: new Date().getTime();
			dateButton.setDate(millis);
			timeButton.setTime(millis);
		} catch (ParseException e) {}
	}
}