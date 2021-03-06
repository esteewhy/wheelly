package com.wheelly.content;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.DatabaseSchema;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.util.DateUtils;

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
import android.util.SparseArray;

/**
 * Meant to be a single entry point to Wheelly Data API.
 */
public class ChronologyProvider extends ContentProvider {
	
	private static final int MILEAGES_ID = 101;
	private static final int MILEAGES_DEFAULTS = 102;
	private static final int REFUELS_ID = 201;
	private static final int REFUELS_DEFAULTS = 202;
	private static final int HEARTBEATS = 300;
	private static final int HEARTBEATS_ID = 301;
	private static final int HEARTBEATS_DEFAULTS = 302;
	private static final int TIMELINE = 305;
	private static final int TIMELINE_ID = 307;
	private static final SparseArray<String[]> DataSchemaLookup = new SparseArray<String[]>();
	private static final SparseArray<Uri> UriMap = new SparseArray<Uri>();
	private static final UriMatcher uriMatcher;
	
	private static final int LOOKUP_CONTENT_TYPE = 0;
	private static final int LOOKUP_TABLE_LIST = 1;
	private static final int LOOKUP_TABLE = 2;
	
	static {
		final String a = DatabaseSchema.CONTENT_AUTHORITY;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH) {{
			addURI(a, "timeline", TIMELINE);
			addURI(a, "heartbeats", HEARTBEATS);
			
			addURI(a, "mileages/#", MILEAGES_ID);
			addURI(a, "refuels/#", REFUELS_ID);
			addURI(a, "heartbeats/#", HEARTBEATS_ID);
			addURI(a, "timeline/#", TIMELINE_ID);
			
			addURI(a, "mileages/defaults", MILEAGES_DEFAULTS);
			addURI(a, "refuels/defaults", REFUELS_DEFAULTS);
			addURI(a, "heartbeats/defaults", HEARTBEATS_DEFAULTS);
		}};
		
		DataSchemaLookup.put(MILEAGES_ID, new String[] { Mileages.CONTENT_ITEM_TYPE, "heartbeats", "heartbeats" });
		DataSchemaLookup.put(REFUELS_ID, new String[] { Refuels.CONTENT_ITEM_TYPE, "heartbeats", "heartbeats" });
		DataSchemaLookup.put(HEARTBEATS_ID, new String[] { Heartbeats.CONTENT_ITEM_TYPE, "heartbeats", "heartbeats" });
		DataSchemaLookup.put(HEARTBEATS, new String[] { Heartbeats.CONTENT_ITEM_TYPE, "heartbeats", "heartbeats" });
		DataSchemaLookup.put(TIMELINE, new String[] { Timeline.CONTENT_TYPE, Timeline.Tables, Timeline.Tables });
		DataSchemaLookup.put(TIMELINE_ID, new String[] { Timeline.CONTENT_ITEM_TYPE, Timeline.Tables, null });
		
		UriMap.put(HEARTBEATS, Heartbeats.CONTENT_URI);
		UriMap.put(TIMELINE, Timeline.CONTENT_URI);
	}
	
	private SQLiteOpenHelper dbHelper;
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriCode = uriMatcher.match(uri);
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			final String tableName = DataSchemaLookup.get(uriCode)[LOOKUP_TABLE];
			
			if(null != tableName) {
				final SQLiteDatabase db = dbHelper.getWritableDatabase();
	
				final int count = db.delete(tableName, selection, selectionArgs);
				
				if(count > 0) {
					notify(uri, uriCode);
				}
				
				return count;
			}
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public String getType(Uri uri) {
		final int uriCode = uriMatcher.match(uri);
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			return DataSchemaLookup.get(uriCode)[LOOKUP_CONTENT_TYPE];
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int uriCode = uriMatcher.match(uri);
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0 && UriMap.indexOfKey(uriCode) >= 0) {
			final String tableName = DataSchemaLookup.get(uriCode)[LOOKUP_TABLE];
			
			if(null != tableName) {
				final SQLiteDatabase db = dbHelper.getWritableDatabase();
				
				final long id = db.insert(tableName, BaseColumns._ID, values);
				
				Uri result = ContentUris.appendId(
					UriMap.get(uriCode).buildUpon(), id).build();
				
				final ContentResolver cr = getContext().getContentResolver();
				cr.notifyChange(uri, null);
				
				return result;
			}
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
		
		switch(uriCode) {
		case MILEAGES_ID:
		case REFUELS_ID:
		case HEARTBEATS_ID:
		case TIMELINE_ID:
			return dbHelper.getReadableDatabase().query(
				DataSchemaLookup.get(uriCode)[LOOKUP_TABLE],
				projection,
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(ContentUris.parseId(uri)) },
				null, null,
				sortOrder,
				"1");
		}
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			final Cursor cursor = dbHelper.getReadableDatabase().query(
				DataSchemaLookup.get(uriCode)[LOOKUP_TABLE_LIST],
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
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			final String tableName = DataSchemaLookup.get(uriCode)[LOOKUP_TABLE];
			
			if(null != tableName) {
				if((uriCode == MILEAGES_ID || uriCode == REFUELS_ID) && !values.containsKey("_modified")) {
					values.put("_modified", DateUtils.dbFormat.format(new Date()));
				}
				
				if(uriCode == HEARTBEATS_ID) {
					final boolean isUpdatedFromSync = values.containsKey("sync_state");
					
					if(!isUpdatedFromSync) {
						values.put("sync_state", 2);
					}
				}
				
				final SQLiteDatabase db = dbHelper.getWritableDatabase();
				final boolean isSingleRecord = Arrays.asList(MILEAGES_ID, REFUELS_ID, HEARTBEATS_ID)
						.contains(uriCode); 
				final int count = isSingleRecord
						? updateSingleRecord(getIdFromUriOrValues(uri, values), values, uriCode, db)
						: db.update(tableName,
								values,
								selection,
								selectionArgs);
				
				if(count > 0) {
					notify(uri, uriCode);
				}
				
				return count;
			}
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	private static int updateSingleRecord(long id, ContentValues values, int uriCode, SQLiteDatabase db) {
		try {
			db.beginTransaction();
			final int count = db.update(DataSchemaLookup.get(uriCode)[LOOKUP_TABLE],
				values,
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(id) });
			
			// Update timestamps of associated records..
			if(count == 1) {
				switch(uriCode) {
				case HEARTBEATS_ID:
					removeSyncInfoDuplicates(id, db);
					break;
				case REFUELS_ID:
					updateRelatedRecordsOfRefuel(id, db);
					break;
				case MILEAGES_ID:
					updateRelatedRecordsOfMileage(id, db);
					break;
				}
			}
			
			db.setTransactionSuccessful();
			return count;
		} finally {
			db.endTransaction();
		}
	}
	
	private static void removeSyncInfoDuplicates(long id, SQLiteDatabase db) {
		db.execSQL("UPDATE heartbeats SET"
			+" sync_id=NULL, sync_state=0, sync_etag=NULL, sync_date=NULL"
		+" WHERE _id IN ("
		+"	SELECT hdup._id FROM heartbeats href"
		+"	INNER JOIN heartbeats hdup"
		+"		ON hdup.sync_id = href.sync_id AND hdup._id != href._id"
		+"	WHERE href._id = ?)", new Object[] { id });
	}
	
	private static void updateRelatedRecordsOfMileage(long id, SQLiteDatabase db) {
		db.execSQL("UPDATE heartbeats SET sync_state = 2"
				+" WHERE sync_state == 1 AND _id = ?",
			new Object[] { id });
	}
	
	private static void updateRelatedRecordsOfRefuel(long id, SQLiteDatabase db) {
		db.execSQL("UPDATE heartbeats SET sync_state = 2"
				+" WHERE sync_state == 1 AND _id = ?",
			new Object[] { id });
	}
	
	private void notify(Uri uri, int uriCode) {
		final ContentResolver cr = getContext().getContentResolver();
		cr.notifyChange(uri, null);
		
		switch(uriCode) {
		case MILEAGES_ID:
		case REFUELS_ID:
		case HEARTBEATS_ID:
		case HEARTBEATS:
			cr.notifyChange(Heartbeats.CONTENT_URI, null);
			cr.notifyChange(Timeline.CONTENT_URI, null);
			cr.notifyChange(Mileages.CONTENT_URI, null);
			cr.notifyChange(Refuels.CONTENT_URI, null);
			break;
		}
	}
}