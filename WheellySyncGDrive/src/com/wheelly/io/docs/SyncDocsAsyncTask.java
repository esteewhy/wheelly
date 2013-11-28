package com.wheelly.io.docs;

import java.io.IOException;
import java.net.URL;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.util.ServiceException;
import com.wheelly.content.WheellyProviderUtils;

/**
 * @author tstrypko
 */
public class SyncDocsAsyncTask extends SendSpreadsheetsAsyncTask {
	
	public SyncDocsAsyncTask(Context context, long trackId, String accountName) {
		super(context, trackId, accountName);
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
	protected boolean addTrackInfo(
			SpreadsheetService spreadsheetService,
			URL worksheetUrl,
			Cursor track) {
		try {
			return super.addTrackInfo(
				spreadsheetService,
				worksheetUrl,
				MatrixCursor.class == track.getClass()
					? resolveRealEntity(worksheetUrl, spreadsheetService)
					: track
			);
		} catch (IOException e) {
			return false;
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	private Cursor resolveRealEntity(URL worksheetUrl, SpreadsheetService spreadsheetService)
			throws IOException, ServiceException {
		ListEntry entry = new SpreadsheetPoster(context, worksheetUrl, spreadsheetService).getLatestRow();
		
		WheellyProviderUtils provider = new WheellyProviderUtils(this.context); 
		return null != entry
			? provider.getLatestRecords(entry.getCustomElements().getValue("date"))
			: provider.getSyncCursor(-1);
	}
}