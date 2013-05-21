package com.wheelly.service;

import android.app.NotificationManager;
import android.content.Context;

public abstract class Notifier {
	protected final Context context;
	protected final NotificationManager nm;
	
	public Notifier(Context context) {
		this.context = context;
		this.nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}