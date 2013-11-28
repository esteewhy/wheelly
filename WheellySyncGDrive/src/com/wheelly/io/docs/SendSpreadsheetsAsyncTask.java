package com.wheelly.io.docs;

import java.io.IOException;
import java.net.URL;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.util.ServiceException;
import com.wheelly.content.WheellyProviderUtils;

/**
 * Comparing to MyTracks implementation, most of the low-level functionality
 * of spreadhseet maangement workflow moved to a base class, so that only
 * spreadhseet content presistence logic remains here.
 */
public class SendSpreadsheetsAsyncTask extends AbstractSendAsyncTask<Cursor> {
	private static final String TAG = SendSpreadsheetsAsyncTask.class.getSimpleName();
	
	protected final WheellyProviderUtils myTracksProviderUtils;
	
	public SendSpreadsheetsAsyncTask(Context context, long trackId, String accountName) {
		super(context, trackId, accountName);
		
		myTracksProviderUtils = new WheellyProviderUtils(context);
	}
	
	@Override
	protected boolean addTrackInfo(SpreadsheetService spreadsheetService, URL worksheetUrl, Cursor track) {
		if (isCancelled() || !track.moveToFirst()) {
			return false;
		}
		
		final SpreadsheetPoster poster = new SpreadsheetPoster(context, worksheetUrl, spreadsheetService);
		final int total = track.getCount();
		try {
			do {
				poster.addTrackInfo(track);
				final int progress = track.getPosition() * (PROGRESS_COMPLETE - PROGRESS_ADD_TRACK_INFO) / total + PROGRESS_ADD_TRACK_INFO;
				if(0 == progress % 5) {
					publishProgress(progress);
				}
			} while(track.moveToNext());
		} catch (IOException e) {
			Log.d(TAG, "Unable to add track info", e);
			return false;
		} catch (ServiceException e) {
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