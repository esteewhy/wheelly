package com.wheelly.io.docs;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import com.google.gdata.client.spreadsheet.ListQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.InvalidEntryException;
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
	
	private final List<ListEntry> cache = new ArrayList<ListEntry>(20);
	
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
		//final long syncState = track.getLong(track.getColumnIndexOrThrow("sync_state"));
		
		EntryPostResult result = postRow(track, idAndEtag);
		idAndEtag = getIdAndVersion(result.Entry);
		
		if(canAcceptResult(track, result)) {
			new HeartbeatBroker(context).updateOrInsert(
				prepareLocalValues(track, result)
			);
		} else if(idAndEtag.first != null || idAndEtag.second != null) {
			// id+etag matched some row but that appears to be a wrong one,
			// so re-iterating..
			syncRow(track, new Pair<String, String>(null, null));
		} else {
			Log.e("WheellySync", "Newly created record doesn't match original");
			//throw new IOException("Something bad..");
		}
	}
	
	private static boolean canAcceptResult(Cursor local, EntryPostResult result) {
		
		Assert.assertNotNull(result);
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
		Pair<Long, Long> odo = null;
		try {
		odo = new Pair<Long, Long>(
			local.getLong(local.getColumnIndex("odometer")),
			Long.parseLong(result.Entry.getCustomElements().getValue("odometer"))
		);
		} catch(NumberFormatException e) {
			Log.w("WheellySync", e);
		}
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
		
		if(null != idAndVersion.second && !idAndVersion.second.equals(localEtag)) {
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
	
	private static Pair<String, String> getIdAndVersion(ListEntry entry) {
		String id = entry.getId();
		
		if(id.contains("/")) {
			id = id.substring(id.lastIndexOf("/") + 1);
		}
		
		return new Pair<String, String>(id, entry.getVersionId());
	}
	
	private static Pair<String, String> getIdAndVersion(Cursor track) {
		String id = track.getString(track.getColumnIndex("sync_id"));
		
		if(null != id && id.contains("/")) {
			id = id.substring(id.lastIndexOf("/") + 1);
		}
		
		return
			new Pair<String, String>(
				id,
				track.getString(track.getColumnIndex("sync_etag"))
			);
	}
	
	private EntryPostResult postRow(Cursor track, Pair<String, String> idAndVersion)
			throws IOException, ServiceException {
		ListEntry remote = obtainRemoteEntry(idAndVersion, track);
		ListEntry local = new ListEntry();
		DocsHelper.assignEntityFromCursor(track, local);
		
		if(null != remote) {
			remote.getCustomElements().replaceWithLocal(local.getCustomElements());
			return new EntryPostResult(remote.update(), EntryPostStatus.UPDATE);
		} else {
			return new EntryPostResult(spreadsheetService.insert(worksheetUrl, local), EntryPostStatus.ADD);
		}
	}
	
	private ListEntry obtainRemoteEntry(Pair<String, String> idAndVersion, Cursor track) throws IOException, ServiceException {
		ListEntry result = null;
		boolean cacheUpdated = false;
		
		if(null != idAndVersion.first) {
			result = loadFromCache(idAndVersion.first);
			if(null != result) {
				return result;
			}
			updateCache(makeKeyFromCursor(track));
			cacheUpdated = true;
			result = loadFromCache(idAndVersion.first);
			if(null != result) {
				return result;
			}
		}
		
		result = resolveFromCache(makeKeyFromCursor(track));
		if(null != result) {
			return result;
		}
		
		if(!cacheUpdated) {
			updateCache(makeKeyFromCursor(track));
			
			result = resolveFromCache(makeKeyFromCursor(track));
			if(null != result) {
				return result;
			}
		}
		
		Log.w("WheellySync", "Missed cache for: " + idAndVersion.first);
		
		return
			null == idAndVersion.first
				? resolveRow(makeKeyFromCursor(track))
				: null == idAndVersion.second
					? load(idAndVersion.first)
					: load(idAndVersion.first, idAndVersion.second);
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
		final ListQuery query = new ListQuery(worksheetUrl) {{
			setReverse(true);
			setMaxResults(1);
		}};
		
		ListFeed feed = spreadsheetService.query(query, ListFeed.class);
		final List<ListEntry> result = feed.getEntries();
		return result.isEmpty() ? null : result.get(0);
	}
	
	private static final DateFormat FORMAT_TIMESTAMP_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Date parseTimeStamp(String s) {
		try {
			return FORMAT_TIMESTAMP_ISO_8601.parse(s);
		} catch (ParseException e) {
			return null;
		}
	}
	
	private static Map<String, String> makeKeyFromCursor(final Cursor cursor) {
		return new HashMap<String, String>() {{
			put("odometer", Long.toString(cursor.getLong(cursor.getColumnIndex("odometer"))));
			put("fuel", Integer.toString(cursor.getInt(cursor.getColumnIndex("fuel"))));
			put("type", DocsHelper.iconFlagsToTypeString(cursor.getInt(cursor.getColumnIndex("icons"))));
			String d = cursor.getString(cursor.getColumnIndexOrThrow("_created"));
			put("date", d);
			put("location", cursor.getString(cursor.getColumnIndex("place")));
		}};
	}
	
	private void updateCache(Map<String, String> values) throws IOException, ServiceException {
	//	String d = FORMAT_TIMESTAMP_ISO_8601.format(new Date(values.get("date")));
		String d = values.get("date");
		String sq = "odometer >=" + values.get("odometer")
				+ " and date >=" + d;
			
		ListQuery query = new ListQuery(worksheetUrl);
		query.setMaxResults(20);
		query.setSpreadsheetQuery(sq);
		Log.d("WheellySync", "Query: " + sq);
		ListFeed feed = spreadsheetService.query(query, ListFeed.class);
		final List<ListEntry> result = feed.getEntries();
		
		Log.i("WheellySync", "Updating cache with: " + result.size() + " entries, it still contains: " + cache.size());
		
		cache.addAll(result);
	}
	
	private ListEntry resolveFromCache(Map<String, String> values) {
		for(ListEntry le : cache) {
			final CustomElementCollection cec = le.getCustomElements();
			
			boolean candidate = true;
			for(String key : values.keySet()) {
				if(null != values.get(key)
						&& !values.get(key).equals(
							"date".equals(key)
								? FORMAT_TIMESTAMP_ISO_8601.format(new Date(cec.getValue(key)))
								: cec.getValue(key)
						)) {
					candidate = false;
					break;
				}
			}
			
			if(candidate) {
				Log.i("WheellySync", "Cache hit by compound key: " + values.toString());
				cache.remove(le);
				return le;
			}
		}
		
		return null;
	}
	
	/**
	 * Attempts to locate remote record by non-key values.
	 * @throws ServiceException 
	 */
	private ListEntry resolveRow(Map<String, String> values) throws IOException, ServiceException {
		String sq = "odometer=" + values.get("odometer")
			+ " and fuel=" + values.get("fuel")
			+ " and type=" + values.get("type")
			+ (null != values.get("location") ? " and location=\"" + values.get("location") + "\"" : "");
		
		ListQuery query = new ListQuery(worksheetUrl);
		query.setMaxResults(1);
		query.setSpreadsheetQuery(sq);
		Log.d("WheellySync", "Query: " + sq);
		ListFeed feed = spreadsheetService.query(query, ListFeed.class);
		final List<ListEntry> result = feed.getEntries(); 
		return result.isEmpty() ? null : result.get(0);
	}
	
	private ListEntry loadFromCache(String id) {
		for(ListEntry le : cache) {
			String remoteId = getIdAndVersion(le).first;
			if(remoteId.equals(id)) {
				Log.i("WheellySync", "Cache hit by ID: " + id);
				cache.remove(le);
				return le;
			}
		}
		
		return null;
	}
	
	private ListEntry load(String id) throws IOException, ServiceException {
		return spreadsheetService.getEntry(
			new URL(worksheetUrl.toString() + "/" + id),
			ListEntry.class
		);
	}
	
	private ListEntry load(String id, String version) throws IOException, ServiceException {
		try {
			try {
				return spreadsheetService.getEntry(
					new URL(worksheetUrl.toString() + "/" + id),
					ListEntry.class,
					version
				);
			} catch(InvalidEntryException e) {
				Log.e("WheellySync", "Invalid version: " + id + "/" + version);
				Log.w("WheellySync", e);
				return load(id);
			}
		} catch(ResourceNotFoundException e) {
			Log.e("WheellySync", "Nonexisting entry: " + id);
			Log.w("WheellySync", e);
			return null;
		}
	}
}