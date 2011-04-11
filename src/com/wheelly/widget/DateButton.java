package com.wheelly.widget;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;

public final class DateButton extends Button implements OnClickListener {

	Calendar dateTime = Calendar.getInstance();
	DateFormat df;
	
	public DateButton(Context context) {
		super(context);
		initialize();
	}

	public DateButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public DateButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	void initialize() {
		setOnClickListener(this);
		this.df = android.text.format.DateFormat.getLongDateFormat(this.getContext());
		setDate(dateTime.getTimeInMillis());
	}
	
	@Override
	public void onClick(View v) {
		new DatePickerDialog(
				this.getContext(),
				new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker arg0, int y, int m, int d) {
						dateTime.set(y, m, d);
						DateButton.this.setText(df.format(dateTime.getTime()));
					}					
				},
				dateTime.get(Calendar.YEAR),
				dateTime.get(Calendar.MONTH),
				dateTime.get(Calendar.DAY_OF_MONTH)
		).show();
	}
	
	public void setDate(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		this.setText(df.format(d));
	}
	
	public long getDate() {
		return dateTime.getTimeInMillis();
	}
}
