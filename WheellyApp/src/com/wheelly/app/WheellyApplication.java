package com.wheelly.app;

import org.acra.*;
import org.acra.annotation.ReportsCrashes;

import com.wheelly.IFilterHolder;

import android.app.Application;
import android.content.ContentValues;

@ReportsCrashes(formKey = "", // will not be used
	mailTo = "esteewhy+wheelly@gmail.com")
public class WheellyApplication extends Application implements IFilterHolder {
	private ContentValues filter;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.filter = new ContentValues();
		ACRA.init(this);
	}
	
	public ContentValues getFilter() {
		return filter;
	}
}