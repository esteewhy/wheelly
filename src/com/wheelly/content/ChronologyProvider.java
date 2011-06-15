package com.wheelly.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.DatabaseSchema;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.db.DatabaseSchema.Mileages;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Meant to be a single entry point to Wheelly Data API.
 */
public class ChronologyProvider extends ContentProvider {
	
	private static final int MILEAGES = 100;
	private static final int MILEAGES_ID = 101;
	private static final int MILEAGES_DEFAULTS = 102;
	private static final int REFUELS = 200;
	private static final int REFUELS_ID = 201;
	private static final int REFUELS_DEFAULTS = 202;
	private static final int HEARTBEATS = 300;
	private static final int HEARTBEATS_ID = 301;
	private static final int HEARTBEATS_DEFAULTS = 302;
	private static final int LOCATIONS = 400;
	private static final int LOCATIONS_ID = 401;
	
	private static final Map<Integer, String[]> SqlTableMap = new HashMap<Integer, String[]>();
	private static final Map<Integer, Uri> UriMap = new HashMap<Integer, Uri>();
	private static final UriMatcher uriMatcher;
	
	private static final int LOOKUP_CONTENT_TYPE = 0;
	private static final int LOOKUP_TABLE_LIST = 1;
	private static final int LOOKUP_TABLE = 2;
	
	static {
		final String a = DatabaseSchema.CONTENT_AUTHORITY;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(a, "mileages", MILEAGES);
		uriMatcher.addURI(a, "refuels", REFUELS);
		uriMatcher.addURI(a, "heartbeats", HEARTBEATS);
		uriMatcher.addURI(a, "locations", LOCATIONS);
		
		uriMatcher.addURI(a, "mileages/#", MILEAGES_ID);
		uriMatcher.addURI(a, "refuels/#", REFUELS_ID);
		uriMatcher.addURI(a, "heartbeats/#", HEARTBEATS_ID);
		uriMatcher.addURI(a, "locations/#", LOCATIONS_ID);
		
		uriMatcher.addURI(a, "mileages/defaults", MILEAGES_DEFAULTS);
		uriMatcher.addURI(a, "refuels/defaults", REFUELS_DEFAULTS);
		uriMatcher.addURI(a, "heartbeats/defaults", HEARTBEATS_DEFAULTS);
		
		SqlTableMap.put(MILEAGES, new String[] { Mileages.CONTENT_TYPE, Mileages.Tables, "mileages" });
		SqlTableMap.put(MILEAGES_ID, new String[] { Mileages.CONTENT_ITEM_TYPE, "mileages" });
		SqlTableMap.put(REFUELS, new String[] { Refuels.CONTENT_TYPE, Refuels.Tables, "refuels" });
		SqlTableMap.put(REFUELS_ID, new String[] { Refuels.CONTENT_ITEM_TYPE, "refuels" });
		SqlTableMap.put(HEARTBEATS, new String[] { Heartbeats.CONTENT_TYPE, Heartbeats.Tables, "heartbeats" });
		SqlTableMap.put(HEARTBEATS_ID, new String[] { Heartbeats.CONTENT_ITEM_TYPE, "heartbeats" });
		
		UriMap.put(MILEAGES_ID, Mileages.CONTENT_URI);
		UriMap.put(REFUELS_ID, Refuels.CONTENT_URI);
		UriMap.put(HEARTBEATS_ID, Heartbeats.CONTENT_URI);
	}
	
	private SQLiteOpenHelper dbHelper;
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		final int uriCode = uriMatcher.match(uri);
		
		if(SqlTableMap.containsKey(uriCode)) {
			
			final int count = db.delete(SqlTableMap.get(uriCode)[LOOKUP_TABLE],
				selection,
				selectionArgs);
			
			if(count > 0) {
				final ContentResolver cr = getContext().getContentResolver();
				cr.notifyChange(uri, null);
				
				switch(uriCode) {
				case MILEAGES:
				case REFUELS:
					cr.notifyChange(Heartbeats.CONTENT_URI, null);
					break;
				}
			}
			
			return count;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public String getType(Uri uri) {
		final int uriCode = uriMatcher.match(uri);
		
		if(SqlTableMap.containsKey(uriCode)) {
			return SqlTableMap.get(uriCode)[LOOKUP_CONTENT_TYPE];
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int uriCode = uriMatcher.match(uri);
		
		if(SqlTableMap.containsKey(uriCode) && UriMap.containsKey(uriCode)) {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			
			final long id = db.insert(SqlTableMap.get(uriCode)[LOOKUP_TABLE],
				BaseColumns._ID, values);
			
			Uri result = ContentUris.appendId(
				UriMap.get(uriCode).buildUpon(), id).build();
			
			final ContentResolver cr = getContext().getContentResolver();
			cr.notifyChange(uri, null);
			
			switch(uriCode) {
			case MILEAGES:
			case REFUELS:
				cr.notifyChange(Heartbeats.CONTENT_URI, null);
				break;
			}
			
			return result;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int uriCode = uriMatcher.match(uri);
		
		if(SqlTableMap.containsKey(uriCode)) {
			final Cursor cursor = dbHelper.getReadableDatabase().query(
				SqlTableMap.get(uriCode)[LOOKUP_TABLE_LIST],
				projection, selection, selectionArgs,
				null, null,
				sortOrder);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			return cursor;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	private static long getIdFromUriOrValues(Uri uri, ContentValues values) {
		List<String> segments = uri.getPathSegments();
		long id = 0;
		if(segments.size() > 1) {
			try {
				id = Long.parseLong(segments.get(1));
			} catch(NumberFormatException ex) {
				id = 0;
			}
		}
		
		if(id <= 0) {
			if(values.containsKey(BaseColumns._ID)) {
				id = values.getAsLong(BaseColumns._ID);
			} else {
				throw new UnsupportedOperationException("Cannot detect record key for uri: " + uri);
			}
		}
		return id;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final int uriCode = uriMatcher.match(uri);
		
		if(SqlTableMap.containsKey(uriCode)) {
			
			final int count = dbHelper.getWritableDatabase()
				.update(SqlTableMap.get(uriCode)[LOOKUP_TABLE],
					values,
					BaseColumns._ID + " = ?",
					new String[] { Long.toString(getIdFromUriOrValues(uri, values)) });
			
			if(count > 0) {
				final ContentResolver cr = getContext().getContentResolver();
				cr.notifyChange(uri, null);
				
				switch(uriCode) {
				case MILEAGES_ID:
				case MILEAGES:
					cr.notifyChange(Mileages.CONTENT_URI, null);
					break;
				case REFUELS_ID:
				case REFUELS:
					cr.notifyChange(Refuels.CONTENT_URI, null);
					cr.notifyChange(Mileages.CONTENT_URI, null);
					break;
				case HEARTBEATS_ID:
				case HEARTBEATS:
					cr.notifyChange(Heartbeats.CONTENT_URI, null);
					cr.notifyChange(Mileages.CONTENT_URI, null);
					cr.notifyChange(Refuels.CONTENT_URI, null);
					break;
				}
			}
			
			return count;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
}