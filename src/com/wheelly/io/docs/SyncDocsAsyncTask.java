package com.wheelly.io.docs;

import java.io.IOException;

import android.accounts.Account;
import android.database.Cursor;
import android.database.MatrixCursor;
import api.wireless.gdata.spreadsheets.data.ListEntry;

import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendActivity;
import com.wheelly.content.WheellyProviderUtils;

/**
 * @author tstrypko
 */
public class SyncDocsAsyncTask extends SendDocsAsyncTask {
	
	public SyncDocsAsyncTask(AbstractSendActivity activity, long trackId, Account account) {
		super(activity, trackId, account);
	}
	
	@Override
	protected Cursor resolveEntity(long trackId) {
		
		if(trackId < 0) {
			// Cannot determine records to send as spreadsheet/worksheet has not yet been determined,
			// so returning fake entity to continue.
			return new MatrixCursor(new String[0]);
		}
		
		return super.resolveEntity(trackId);
	}
	
	@Override
	protected boolean addTrackInfo(Cursor track) {
		try {
			return super.addTrackInfo(MatrixCursor.class == track.getClass() ? resolveRealEntity() : track);
		} catch (IOException e) {
			return false;
		}
	}
	
	private Cursor resolveRealEntity() throws IOException {
		final String worksheetUri = String.format(SendDocsUtils.GET_WORKSHEET_URI, spreadsheetId, worksheetId);
		ListEntry entry = new SpreadsheetPoster(context, worksheetUri, spreadsheetsAuthToken).getLatestRow();
		
		WheellyProviderUtils provider = new WheellyProviderUtils(this.context); 
		Cursor cursor = null != entry
			? provider.getLatestRecords(Long.parseLong(entry.getValue("odometer")))
			: provider.getSyncCursor(-1);
//		int cnt = cursor.getCount();
		return cursor;
	}
}