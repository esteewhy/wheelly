package com.wheelly.service;

import com.wheelly.R;
import com.wheelly.activity.Main;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

public class ProgressNotifier extends Notifier {
	private static final int NOTIFICATION_ID = 1;
	private static final String NOTIFICATION_TAG = "sync";
	private Notification notification;
	
	private final static boolean VISUAL = false;
	
	public ProgressNotifier(Context context) {
		super(context);
	}
	
	public void startProgress() {
		notification =
			VISUAL	
			? new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon_small)
				//.setContentTitle("Synchronizing")
				.setContent(new RemoteViews(context.getApplicationContext().getPackageName(), R.layout.notification_progress))
				.getNotification()
			: new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon_small)
				.getNotification();
		
		notification.contentIntent = PendingIntent.getService(context, 0, new Intent(context, Main.class), 0); 
		nm.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
	}
	
	public void notifyProgress(int progress) {
		if (notification != null) {
			if(VISUAL) {
				notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);
				notification.contentView.setTextViewText(R.id.status_text, "Synchrozing " + String.valueOf(progress));
			} else {
				notification.setLatestEventInfo(context,
					"Synchronizing",
					"Progress: "
						+ String.valueOf(progress)
						+ "%",
					notification.contentIntent);
			}
			nm.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);
		}
	}
	
	public void reportResult(boolean success) {
		if (notification != null) {
			if(success) {
				nm.cancel(NOTIFICATION_ID);
			} else {
				notification.contentView.setViewVisibility(R.id.status_progress, View.INVISIBLE);
				notification.contentView.setTextViewText(R.id.status_text, "Synchronization failed");
			}
		}
	}
	
	public void canceNotificationForMileage(long id) {
		nm.cancel(NOTIFICATION_TAG, (int)id);
	}
}