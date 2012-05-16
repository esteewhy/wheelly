package com.wheelly.service;

import com.wheelly.R;
import com.wheelly.activity.Mileage;
import com.wheelly.db.MileageBroker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;

public class Notifier {
	private final Context context;
	
	public Notifier(Context context) {
		this.context = context;
	}
	
	public void notifyAboutPendingMileages() {
		final long id = new MileageBroker(context).getLastPendingId();
		if(id > 0) {
			notifyAboutPendingMileage(id);
		}
	}
	
	public void notifyAboutPendingMileage(long id) {
		final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent intent = new Intent(context, Mileage.class);
		intent.putExtra(BaseColumns._ID, id);
		//intent.putExtra("ui_command", TripControlBar.UI_STOP);
		
		final Notification n =
			new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon_small)
				.setContentTitle("Mileage in progress")
				.setContentText("Select to edit.")
				.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
				.getNotification();
		nm.notify((int)id, n);
	}
	
	public void canceNotificationForMileage(long id) {
		final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel((int)id);
	}
}