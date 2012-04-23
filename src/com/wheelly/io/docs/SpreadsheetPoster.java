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
import android.util.Pair;
import api.wireless.gdata.spreadsheets.data.ListEntry;
import api.wireless.gdata.spreadsheets.parser.xml.XmlSpreadsheetsGDataParserFactory;

import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.QueryParamsImpl;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.client.QueryParams;
import com.google.wireless.gdata.data.Entry;
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
			throws IOException, HttpException {
		final EntryPostResult result = postRow(track, authToken);
		
		if(isEntryValid(track, result.Entry)) {
			new HeartbeatBroker(context).updateOrInsert(
				prepareLocalValues(track, result)
			);
		}
	}
	
	private static boolean isEntryValid(Cursor local, ListEntry remote) {
		final long localOdo = Long.parseLong(remote.getValue("odometer", "-1"));
		final long remoteOdo = local.getLong(local.getColumnIndex("odometer")); 
		return localOdo == remoteOdo;
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
			values.put("sync_etag", result.Status == EntryPostStatus.CONFLICT ? null : idAndVersion.second);
		}
		
		if(result.Status != EntryPostStatus.CONFLICT) {
			final String syncDate = result.Entry.getUpdateDate();
			final String localSyncDate = local.getString(local.getColumnIndex("sync_date"));
			if(!syncDate.equals(localSyncDate)) {
				values.put("sync_date", syncDate);
			}
			
			values.put("odometer", Long.parseLong(result.Entry.getValue("odometer")));
			values.put("fuel", Integer.parseInt(result.Entry.getValue("fuel")));
		}
		
		return values;
	}
	
	private static Pair<String, String> getIdAndVersion(Entry entry) {
		String[] parts = entry.getEditUri().split("/");
		return new Pair<String, String>(parts[parts.length - 2], parts[parts.length - 1]);
	}
	
	private static Pair<String, String> getIdAndVersion(Cursor track) {
		return
			new Pair<String, String>(
				track.getString(track.getColumnIndex("sync_id")),
				track.getString(track.getColumnIndex("sync_etag"))
			);
	}
	
	private EntryPostResult postRow(Cursor track, String authToken) throws IOException, HttpException {
		Pair<String, String> idAndVersion = getIdAndVersion(track);
		String entityUri = null;
		String editUri = worksheetUri;
		
		if(null == idAndVersion.first) {
			ListEntry existing = resolveRow(track);
			
			if(null != existing) {
				idAndVersion = getIdAndVersion(existing);
			}
		}
		
		if(null != idAndVersion.first && null != idAndVersion.second) {
			entityUri = worksheetUri + "/" + idAndVersion.first;
			editUri += "/" + idAndVersion.first + "/" + idAndVersion.second;
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
				&& HttpException.SC_CONFLICT == ((HttpURLConnection)conn).getResponseCode();
	}
	
	private static ListEntry parseEntity(InputStream is) throws IOException {
		if(null != is) {
		    try {
				GDataParser parser = new XmlSpreadsheetsGDataParserFactory(new AndroidXmlParserFactory())
					.createParser(ListEntry.class, is);
			
				try {
					while(parser.hasMoreData()) {
						return (ListEntry) parser.parseStandaloneEntry();
					}
				} finally {
					parser.close();
				}
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
	
	public ListEntry getLatestRow() throws IOException {
		QueryParams query = new QueryParamsImpl();
		query.setMaxResults("1");
		query.setParamValue("reverse", "true");
		final String feedUri = query.generateQueryUrl(worksheetUri);
		
		try {
			return parseEntity(GDataClientFactory.getGDataClient(context).getFeedAsStream(feedUri, authToken));
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Attempts to locate remote record by non-key values.
	 * @throws IOException 
	 * @throws HttpException 
	 */
	private ListEntry resolveRow(Cursor local) throws IOException, HttpException {
		QueryParams query = new QueryParamsImpl();
		query.setMaxResults("1");
		query.setParamValue("odometer", Long.toString(local.getLong(local.getColumnIndex("odometer"))));
		query.setParamValue("fuel", Integer.toString(local.getInt(local.getColumnIndex("fuel"))));
		query.setParamValue("location", local.getString(local.getColumnIndex("place")));
		final String feedUri = query.generateQueryUrl(worksheetUri);
		
		return parseEntity(GDataClientFactory.getGDataClient(context).getFeedAsStream(feedUri, authToken));
	}
}