package com.wheelly.io.docs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import api.wireless.gdata.spreadsheets.data.ListEntry;
import api.wireless.gdata.spreadsheets.parser.xml.XmlSpreadsheetsGDataParserFactory;

import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.wheelly.db.HeartbeatBroker;

/**
 * Reusable worker to send and persist single row at a time.
 */
public class SpreadsheetPoster {
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ATOM_FEED_MIME_TYPE = "application/atom+xml";
	private static final String AUTHORIZATION = "Authorization";
	private static final String AUTHORIZATION_PREFIX = "GoogleLogin auth=";
	
	private final String worksheetUri;
	private final String authToken;
	private final Context context;
	
	public SpreadsheetPoster(Context context, String worksheetUri, String authToken) {
		this.context = context;
		this.worksheetUri = worksheetUri;
		this.authToken = authToken;
	}
	
	public void addTrackInfo(Cursor track)
			throws IOException {
		final EntryPostResult result = postRow(track, authToken);
		
		if(isEntryValid(track, result.Entry)) {
			new HeartbeatBroker(context).updateOrInsert(
				prepareLocalValues(track, result)
			);
		}
	}
	
	private static boolean isEntryValid(Cursor record, ListEntry entry) {
		return Integer.parseInt(entry.getValue("odometer", "-1")) == record.getLong(record.getColumnIndex("odometer"));
	}
	
	private static ContentValues prepareLocalValues(Cursor local, EntryPostResult result) {
		String[] parts = result.Entry.getEditUri().split("/");
		String syncId = parts[parts.length - 2];
		String newEtag = parts[parts.length - 1];
		
		final long id = local.getLong(local.getColumnIndex(BaseColumns._ID));
		final ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, id);
		
		final String localSyncId = local.getString(local.getColumnIndex("sync_id")); 
		if(!localSyncId.equals(syncId)) {
			values.put("sync_id", syncId);
		}
		
		final String localEtag = local.getString(local.getColumnIndex("sync_etag")); 
		if(!localEtag.equals(newEtag)) {
			values.put("sync_etag", result.Status == EntryPostStatus.CONFLICT ? null : newEtag);
		}
		
		if(result.Status != EntryPostStatus.CONFLICT) {
			final String localSyncDate = local.getString(local.getColumnIndex("sync_date"));
			if(!localSyncDate.equals(result.Entry.getUpdateDate())) {
				values.put("sync_date", result.Entry.getUpdateDate());
			}
			
			values.put("odometer", Long.parseLong(result.Entry.getValue("odometer")));
			values.put("fuel", Integer.parseInt(result.Entry.getValue("fuel")));
		}
		
		return values;
	}
	
	private EntryPostResult postRow(Cursor track, String authToken) throws IOException {
		String rowId = track.getString(track.getColumnIndex("sync_id"));
		String etag = track.getString(track.getColumnIndex("sync_etag"));
		String entityUri = null;
		String editUri = worksheetUri;
		if(null != rowId && null != etag) {
			entityUri = worksheetUri + "/" + rowId;
			editUri += "/" + rowId + "/" + etag;
		}
		
		return addOrUpdateRow(editUri, DocsHelper.getRowContent(track, entityUri));
	}
	
	private final EntryPostResult addOrUpdateRow(String editUri, String rowContent)
			throws IOException {
		URL url = new URL(editUri);
		URLConnection conn = url.openConnection();
		
		EntryPostStatus status = EntryPostStatus.ADD;
		
		if(rowContent.contains("<id>")) {
			((HttpURLConnection)conn).setRequestMethod("PUT");
			conn.setDoInput(true);
			status = EntryPostStatus.UPDATE;
		}
		
		conn.addRequestProperty(CONTENT_TYPE, ATOM_FEED_MIME_TYPE);
		conn.addRequestProperty(AUTHORIZATION, AUTHORIZATION_PREFIX + authToken);
		conn.setDoOutput(true);
		
		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
		writer.write(rowContent);
		writer.flush();
		writer.close();
		
		final boolean success = !isConflict(conn);
		
		return new EntryPostResult(
			parseEntity(success
				? conn.getInputStream()
				: ((HttpURLConnection)conn).getErrorStream()
			),
			success ? status : EntryPostStatus.CONFLICT
		);
	}
	
	private static boolean isConflict(URLConnection conn) throws IOException {
		return conn instanceof HttpURLConnection
				&& 409 == ((HttpURLConnection)conn).getResponseCode();
	}
	
	private static ListEntry parseEntity(InputStream is) throws IOException {
		if(null != is) {
		    try {
				GDataParser parser = new XmlSpreadsheetsGDataParserFactory(new AndroidXmlParserFactory())
					.createParser(ListEntry.class, is);
			
				while(parser.hasMoreData()) {
					return (ListEntry) parser.parseStandaloneEntry();
				}
				parser.close();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private static enum EntryPostStatus {
		ADD, UPDATE, CONFLICT
	}
	
	private static class EntryPostResult {
		public final ListEntry Entry;
		public final EntryPostStatus Status;
		
		public EntryPostResult(ListEntry entry, EntryPostStatus status) {
			Entry = entry;
			Status = status;
		}
	}
}