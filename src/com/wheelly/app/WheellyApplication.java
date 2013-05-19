package com.wheelly.app;

import com.wheelly.IFilterHolder;

import android.app.Application;
import android.content.ContentValues;

public class WheellyApplication extends Application implements IFilterHolder {
	private ContentValues filter;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.filter = new ContentValues();
	}
	
	public ContentValues getFilter() {
		return filter;
	}
}