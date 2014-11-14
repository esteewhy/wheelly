package com.wheelly.content;

import java.util.List;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.DatabaseSchema;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Locations;
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

public class LocationProvider extends ContentProvider {
	
	private static final int LOCATIONS = 400;
	private static final int LOCATIONS_ID = 401;
	private static final SparseArray<String> DataSchemaLookup = new SparseArray<String>();
	private static final SparseArray<Uri> UriMap = new SparseArray<Uri>();
	private static final UriMatcher uriMatcher;
	
	static {
		final String a = DatabaseSchema.CONTENT_AUTHORITY;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH) {{
			addURI(a, "locations", LOCATIONS);
			addURI(a, "locations/#", LOCATIONS_ID);
		}};
		
		DataSchemaLookup.put(LOCATIONS, Locations.CONTENT_TYPE);
		DataSchemaLookup.put(LOCATIONS_ID, Locations.CONTENT_ITEM_TYPE);
		
		UriMap.put(LOCATIONS, Locations.CONTENT_URI);
	}
	
	private SQLiteOpenHelper dbHelper;
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriCode = uriMatcher.match(uri);
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			final int count = db.delete("locations", selection, selectionArgs);
			if(count > 0) {
				notify(uri, uriCode);
			}
			
			return count;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public String getType(Uri uri) {
		final int uriCode = uriMatcher.match(uri);
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			return DataSchemaLookup.get(uriCode);
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int uriCode = uriMatcher.match(uri);
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0 && UriMap.indexOfKey(uriCode) >= 0) {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			
			final long id = db.insert("locations", BaseColumns._ID, values);
			
			Uri result = ContentUris.appendId(
				UriMap.get(uriCode).buildUpon(), id).build();
			
			final ContentResolver cr = getContext().getContentResolver();
			cr.notifyChange(uri, null);
			
			switch(uriCode) {
			case LOCATIONS:
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
		
		switch(uriCode) {
		case LOCATIONS_ID:
			return dbHelper.getReadableDatabase().query(
				"locations",
				projection,
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(ContentUris.parseId(uri)) },
				null, null,
				sortOrder,
				"1");
		}
		
		if(DataSchemaLookup.indexOfKey(uriCode) >= 0) {
			final Cursor cursor = dbHelper.getReadableDatabase().query(
				"locations",
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
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			final int count = LOCATIONS_ID == uriCode
					? db.update("locations",
							values,
							BaseColumns._ID + " = ?",
							new String[] { Long.toString(getIdFromUriOrValues(uri, values)) })
					: db.update("locations",
							values,
							selection,
							selectionArgs);
			
			if(count > 0) {
				notify(uri, uriCode);
			}
			
			return count;
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
	
	private void notify(Uri uri, int uriCode) {
		final ContentResolver cr = getContext().getContentResolver();
		cr.notifyChange(uri, null);
	}
}