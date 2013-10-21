package com.wheelly.widget;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;

public final class TimeButton extends Button implements OnClickListener {

	private Calendar dateTime = Calendar.getInstance();
	private DateFormat tf;
	
	public TimeButton(Context context) {
		super(context);
		initialize();
	}

	public TimeButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public TimeButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	private void initialize() {
		setOnClickListener(this);
		this.tf = android.text.format.DateFormat.getTimeFormat(this.getContext());
		setTime(dateTime.getTimeInMillis());
	}
	
	@Override
	public void onClick(View v) {
		boolean is24Format = "24".equals(
				Settings.System.getString(
						this.getContext().getContentResolver(),
						Settings.System.TIME_12_24));
		
		new TimePickerDialog(
				this.getContext(),
				new OnTimeSetListener(){
					@Override
					public void onTimeSet(TimePicker picker, int h, int m) {
						dateTime.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
						dateTime.set(Calendar.MINUTE, picker.getCurrentMinute());
						TimeButton.this.setText(tf.format(dateTime.getTime()));
					}
				},
				dateTime.get(Calendar.HOUR_OF_DAY),
				dateTime.get(Calendar.MINUTE),
				is24Format
		).show();
	}
	
	public void setTime(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		this.setText(tf.format(d));
	}
	
	public long getTime() {
		return dateTime.getTimeInMillis();
	}
}