package com.wheelly.io.docs;

import java.io.IOException;

import android.accounts.Account;
import android.database.Cursor;
import android.util.Log;

import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendActivity;
import com.wheelly.content.WheellyProviderUtils;

public class SyncDocsAsyncTask extends AbstractSendDocsAsyncTask<Cursor> {

	private static final String TAG = SyncDocsAsyncTask.class.getSimpleName();
	private final WheellyProviderUtils myTracksProviderUtils;
	
	public SyncDocsAsyncTask(AbstractSendActivity activity, long trackId, Account account) {
		super(activity, trackId, account);
		
		myTracksProviderUtils = new WheellyProviderUtils(context);
	}
	
	@Override
	protected boolean addTrackInfo(Cursor track) {
	  if (isCancelled() || !track.moveToFirst()) {
	    return false;
	  }
	  try {
	    do {
	    	SendDocsUtils.addTrackInfo(track, spreadsheetId, worksheetId, spreadsheetsAuthToken, context);
	    } while(track.moveToNext());
	  } catch (IOException e) {
	    Log.d(TAG, "Unable to add track info", e);
	    return false;
	  }
	  return true;
	}

	@Override
	protected Cursor resolveEntity(long trackId) {
		return myTracksProviderUtils.getSyncCursor(trackId);
	}

	@Override
	protected String buildWorksheetTitle(Cursor track) {
		return "Wheelly";
	}
}