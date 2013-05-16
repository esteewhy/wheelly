package com.wheelly.io.docs;

import java.io.IOException;
import java.net.URL;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import android.util.Pair;

import com.google.android.apps.mytracks.util.StringUtils;
import com.google.gdata.client.spreadsheet.ListQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.ResourceNotFoundException;
import com.google.gdata.util.ServiceException;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.db.HeartbeatBroker;

/**
 * Reusable worker to send and persist single row at a time.
 */
public class SpreadsheetPoster {
	private final URL worksheetUrl;
	private final SpreadsheetService spreadsheetService;
	private final Context context;
	
	public SpreadsheetPoster(Context context, URL worksheetUrl, SpreadsheetService spreadsheetService) {
		this.context = context;
		this.worksheetUrl = worksheetUrl;
		this.spreadsheetService = spreadsheetService;
	}
	
	public void addTrackInfo(Cursor track)
			throws IOException, ServiceException {
		syncRow(track, getIdAndVersion(track));
	}

	private void syncRow(Cursor track, Pair<String, String> idAndEtag) throws IOException, ServiceException {
		final long syncState = track.getLong(track.getColumnIndexOrThrow("sync_state"));
		
		EntryPostResult result = null;
		
		if(Timeline.SYNC_STATE_READY == syncState && null != idAndEtag.first) {
			result = postRow(track, idAndEtag);
			idAndEtag = getIdAndVersion(result.Entry);
		}
		
		if(canAcceptResult(track, result)) {
			new HeartbeatBroker(context).updateOrInsert(
				prepareLocalValues(track, result)
			);
		} else if(idAndEtag.first != null || idAndEtag.second != null) {
			// id+etag matched some row but that appears to be a wrong one,
			// so re-iterating..
			syncRow(track, new Pair<String, String>(null, null));
		} else {
			Log.e("GDocs", "Newly created record doesn't match original");
			//throw new IOException("Something bad..");
		}
	}
	
	private static boolean canAcceptResult(Cursor local, EntryPostResult result) {
		if(result.Status == EntryPostStatus.READ) {
			final Pair<String, String> type = new Pair<String, String>(
				DocsHelper.iconFlagsToTypeString(local.getInt(local.getColumnIndexOrThrow("icons"))),
				result.Entry.getCustomElements().getValue("type")
			);
		
			if(type.first.equals(type.second)) {
				return true;
			}
		}
		
		if(null == result.Entry) {
			return false;
		}
		
		final Pair<Long, Long> odo = new Pair<Long, Long>(
			local.getLong(local.getColumnIndex("odometer")),
			Long.parseLong(result.Entry.getCustomElements().getValue("odometer"))
		);
		
		final Pair<String, String> type = new Pair<String, String>(
			DocsHelper.iconFlagsToTypeString(local.getInt(local.getColumnIndex("icons"))),
			result.Entry.getCustomElements().getValue("type")
		);
		
		return odo.first.equals(odo.second)
				&& null != type.second && type.second.length() > 0
				&& type.second.equals(type.first);
	}
	
	private static ContentValues prepareLocalValues(Cursor local, EntryPostResult result) {
		final Pair<String, String> idAndVersion = getIdAndVersion(result.Entry);
		final long id = local.getLong(local.getColumnIndex(BaseColumns._ID));
		final ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, id);
		
		final String localSyncId = local.getString(local.getColumnIndex("sync_id")); 
		if(!idAndVersion.first.equals(localSyncId)) {
			values.put("sync_id", idAndVersion.first);
		}
		
		final String localEtag = local.getString(local.getColumnIndex("sync_etag")); 
		if(!idAndVersion.second.equals(localEtag)) {
			values.put("sync_etag", idAndVersion.second);
		}
		
		values.put("sync_date", result.Entry.getUpdated().toString());
		
		if(result.Status != EntryPostStatus.CONFLICT) {
			
			values.put("odometer", Long.parseLong(result.Entry.getCustomElements().getValue("odometer")));
			values.put("fuel", Integer.parseInt(result.Entry.getCustomElements().getValue("fuel")));
			values.put("sync_state", Timeline.SYNC_STATE_READY);
		} else {
			values.put("sync_state", Timeline.SYNC_STATE_CONFLICT);
		}
		
		return values;
	}
	
	private static Pair<String, String> getIdAndVersion(BaseEntry entry) {
		return new Pair<String, String>(entry.getId(), entry.getVersionId());
	}
	
	private static Pair<String, String> getIdAndVersion(Cursor track) {
		return
			new Pair<String, String>(
				track.getString(track.getColumnIndex("sync_id")),
				track.getString(track.getColumnIndex("sync_etag"))
			);
	}
	
	private EntryPostResult postRow(Cursor track, Pair<String, String> idAndVersion)
			throws IOException, ServiceException {
		ListEntry remote = null;
		try {
			remote = null == idAndVersion.first
				? resolveRow(track)
				: null == idAndVersion.second
					? load(idAndVersion.first)
					: load(idAndVersion.first, idAndVersion.second);
		} catch(ResourceNotFoundException ex) {
		}
		
		ListEntry local = new ListEntry();
		DocsHelper.assignEntityFromCursor(track, local);
		
		if(null != remote) {
			remote.getCustomElements().replaceWithLocal(local.getCustomElements());
			return new EntryPostResult(remote.update(), EntryPostStatus.UPDATE);
		} else {
			return new EntryPostResult(spreadsheetService.insert(worksheetUrl, local), EntryPostStatus.ADD);
		}
	}
	
	private static enum EntryPostStatus {
		READ, ADD, UPDATE, CONFLICT
	}
	
	private static class EntryPostResult {
		public final ListEntry Entry;
		public final EntryPostStatus Status;
		
		public EntryPostResult(ListEntry entry, EntryPostStatus status) {
			Entry = entry;
			Status = status;
		}
	}
	
	public ListEntry getLatestRow() throws IOException, ServiceException {
		ListQuery query = new ListQuery(worksheetUrl);
		query.setReverse(true);
		query.setMaxResults(1);
		ListFeed feed = spreadsheetService.query(query, ListFeed.class);
		return feed.getEntries().get(0);
	}
	
	/**
	 * Attempts to locate remote record by non-key values.
	 */
	private ListEntry resolveRow(Cursor local) throws IOException, ServiceException {
		String sq = "odometer="
			+ Long.toString(local.getLong(local.getColumnIndex("odometer")))
			+ " and "
			+ "fuel="
			+ Integer.toString(local.getInt(local.getColumnIndex("fuel")))
			+ " and "
			+ "type="
			+ DocsHelper.iconFlagsToTypeString(local.getInt(local.getColumnIndex("icons")));
		
		final String location = local.getString(local.getColumnIndex("place"));
		if(null != location) {
			sq += " and location=\"" + location + "\"";
		}
		
		ListQuery query = new ListQuery(worksheetUrl);
		query.setMaxResults(1);
		query.setSpreadsheetQuery(sq);
		ListFeed feed = spreadsheetService.query(query, ListFeed.class);
		return feed.getEntries().get(0);
	}
	
	private ListEntry load(String id) throws IOException, ServiceException {
		return spreadsheetService.getEntry(
			new URL(worksheetUrl.toString() + "/" + id),
			ListEntry.class
		);
	}
	
	private ListEntry load(String id, String version) throws IOException, ServiceException {
		return spreadsheetService.getEntry(
			new URL(worksheetUrl.toString() + "/" + id + "/" + version),
			ListEntry.class
		);
	}
}