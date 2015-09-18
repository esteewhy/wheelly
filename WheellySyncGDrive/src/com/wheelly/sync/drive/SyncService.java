package com.wheelly.sync.drive;

import com.wheelly.io.docs.SendSpreadsheetsAsyncTask;

import android.app.IntentService;
import android.content.Intent;

public class SyncService extends IntentService {

	public SyncService(String name) {
		super(name);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		if(intent.hasExtra("accountName")) {
			
			new SendSpreadsheetsAsyncTask(this,
				intent.getLongExtra("id", -1),
				intent.getStringExtra("accountName")
			).execute();
		}
	}
}