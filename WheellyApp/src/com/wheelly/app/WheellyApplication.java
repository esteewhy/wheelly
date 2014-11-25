package com.wheelly.app;

import org.acra.*;
import org.acra.annotation.ReportsCrashes;
import android.app.Application;

@ReportsCrashes(formKey = "", // will not be used
	mailTo = "esteewhy+wheelly@gmail.com")
public class WheellyApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
	}
}