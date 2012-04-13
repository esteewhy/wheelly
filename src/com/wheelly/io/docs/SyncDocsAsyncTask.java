package com.wheelly.io.docs;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient.SpreadsheetEntry;
import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendActivity;

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
		
		if(MatrixCursor.class == track.getClass()) {
			SpreadsheetEntry entry = SendDocsUtils.getLatestRow(spreadsheetId, worksheetId, spreadsheetsClient, spreadsheetsAuthToken);
			track.toString();
		}
		
		return super.addTrackInfo(track);
	}
}