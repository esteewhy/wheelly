package com.wheelly.io.docs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.format.Time;
import android.util.Pair;
import api.wireless.gdata.spreadsheets.data.ListEntry;
import api.wireless.gdata.spreadsheets.parser.xml.XmlSpreadsheetsGDataParserFactory;

import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.QueryParamsImpl;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.client.QueryParams;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.data.StringUtils;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.util.DateUtils;

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
		syncRow(track, getIdAndVersion(track));
	}

	private void syncRow(Cursor track, Pair<String, String> idAndEtag) throws HttpException, IOException {
		final EntryPostResult result = postRow(track, idAndEtag);
		
		if(isEntryValid(track, result.Entry)) {
			new HeartbeatBroker(context).updateOrInsert(
				prepareLocalValues(track, result)
			);
		} else {
			// id+etag matched some row but that appears to be a wrong one,
			// so re-iterating..
			syncRow(track, new Pair<String, String>(null, null));
		}
	}
	
	private static boolean isEntryValid(Cursor local, ListEntry remote) {
		if(null == remote) {
			return false;
		}
		
		final Pair<Long, Long> odo = new Pair<Long, Long>(
				local.getLong(local.getColumnIndex("odometer")),
				Long.parseLong(remote.getValue("odometer", "-1"))
		);
		
		final Pair<String, String> type = new Pair<String, String>(
			DocsHelper.iconFlagsToTypeString(local.getInt(local.getColumnIndex("icons"))),
			remote.getValue("type")
		);
		
		return odo.first.equals(odo.second)
				&& !StringUtils.isEmptyOrWhitespace(type.second)
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
			values.put("sync_etag", result.Status == EntryPostStatus.CONFLICT ? null : idAndVersion.second);
		}
		
		if(result.Status != EntryPostStatus.CONFLICT) {
			final Time t = new Time();
			
			if(t.parse3339(result.Entry.getUpdateDate())) {
				values.put("sync_date", DateUtils.dbFormat.format(new Date(t.toMillis(false))));
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
	
	private EntryPostResult postRow(Cursor track, Pair<String, String> idAndVersion) throws HttpException, IOException {
		String entityUri = null;
		String editUri = worksheetUri;
		
		if(null == idAndVersion.first) {
			ListEntry existing = resolveRow(track);
			
			if(null != existing) {
				idAndVersion = getIdAndVersion(existing);
			}
		} else if(null == idAndVersion.second) {
			ListEntry existing = load(idAndVersion.first);
			
			if(null != existing) {
				idAndVersion = getIdAndVersion(existing);
			}
		}
	
		if(null != idAndVersion.first) {
			entityUri = worksheetUri + "/" + idAndVersion.first;
			
			if(null != idAndVersion.second) {
				editUri += "/" + idAndVersion.first + "/" + idAndVersion.second;
			}
		}
		
		try {
			return addOrUpdateRow(editUri, DocsHelper.getRowContent(track, entityUri));
		} catch (FileNotFoundException e) {
			return forceAddRow(track);
		} catch (ParseException ex1) {
			ex1.printStackTrace();
			return forceAddRow(track);
		}
	}
	
	private EntryPostResult forceAddRow(Cursor track) throws IOException {
		// Probably, our stored id+etag are totally incorrect (i.e.: Spreadsheet was re-created),
		// so try from the scratch..
		try {
			return addOrUpdateRow(worksheetUri, DocsHelper.getRowContent(track, null));
		} catch (ParseException e1) {
			return new EntryPostResult(null, EntryPostStatus.CONFLICT);
		}
	}
	
	private final EntryPostResult addOrUpdateRow(String editUri, String rowContent)
			throws IOException, ParseException {
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
	
	private static ListEntry parseEntity(InputStream is) throws IOException, ParseException {
		if(null != is) {
			GDataParser parser = new XmlSpreadsheetsGDataParserFactory(new AndroidXmlParserFactory())
				.createParser(ListEntry.class, is);
			
			try {
				while(parser.hasMoreData()) {
					return (ListEntry) parser.parseStandaloneEntry();
				}
			} finally {
				parser.close();
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
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Attempts to locate remote record by non-key values.
	 * @throws ParseException 
	 */
	private ListEntry resolveRow(Cursor local) throws IOException, HttpException {
		QueryParams query = new QueryParamsImpl();
		query.setMaxResults("1");
		
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
		query.setParamValue("sq", sq); 
		final String feedUri = query.generateQueryUrl(worksheetUri);
		
		try {
			return parseEntity(GDataClientFactory.getGDataClient(context).getFeedAsStream(feedUri, authToken));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private ListEntry load(String id) throws IOException, HttpException {
		try {
			return parseEntity(GDataClientFactory.getGDataClient(context).getFeedAsStream(worksheetUri + "/" + id, authToken));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}