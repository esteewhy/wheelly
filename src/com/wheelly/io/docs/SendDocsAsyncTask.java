package com.wheelly.io.docs;

import java.io.IOException;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.wireless.gdata.client.HttpException;
import com.wheelly.content.WheellyProviderUtils;

public class SendDocsAsyncTask extends AbstractSendDocsAsyncTask<Cursor> {

	private static final String TAG = SendDocsAsyncTask.class.getSimpleName();
	protected final WheellyProviderUtils myTracksProviderUtils;
	
	public SendDocsAsyncTask(Context context, long trackId, Account account) {
		super(context, trackId, account);
		
		myTracksProviderUtils = new WheellyProviderUtils(context);
	}
	
	@Override
	protected boolean addTrackInfo(Cursor track) {
		if (isCancelled() || !track.moveToFirst()) {
			return false;
		}
		
		final String worksheetUri = String.format(SendDocsUtils.GET_WORKSHEET_URI, spreadsheetId, worksheetId);
		final SpreadsheetPoster poster = new SpreadsheetPoster(context, worksheetUri, spreadsheetsAuthToken);
		final int total = track.getCount();
		try {
			do {
				poster.addTrackInfo(track);
				publishProgress(track.getPosition() * total / (PROGRESS_COMPLETE - PROGRESS_ADD_TRACK_INFO) + PROGRESS_ADD_TRACK_INFO);
			} while(track.moveToNext());
		} catch (IOException e) {
			Log.d(TAG, "Unable to add track info", e);
			return false;
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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