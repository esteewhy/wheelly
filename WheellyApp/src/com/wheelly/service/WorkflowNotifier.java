package com.wheelly.service;

import com.wheelly.R;
import com.wheelly.activity.Stop;
import com.wheelly.db.MileageBroker;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;

public class WorkflowNotifier extends Notifier {
	public WorkflowNotifier(Context context) {
		super(context);
	}

	private static final String NOTIFICATION_TAG = "track";
	
	public void notifyAboutPendingMileages() {
		final long id = new MileageBroker(context).getLastPendingId();
		if(id > 0) {
			notifyAboutPendingMileage(id);
		}
	}
	
	public void notifyAboutPendingMileage(long id) {
		Intent intent = new Intent(context, Stop.class);
		intent.putExtra(BaseColumns._ID, id);
		//intent.putExtra("ui_command", TripControlBar.UI_STOP);
		
		final Notification n =
			new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon_small)
				.setContentTitle("Mileage in progress")
				.setContentText("Select to edit.")
				.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
				.build();
		nm.notify(NOTIFICATION_TAG, (int)id, n);
	}
	
	public void canceNotificationForMileage(long id) {
		nm.cancel(NOTIFICATION_TAG, (int)id);
	}
}