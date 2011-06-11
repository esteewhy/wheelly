package com.wheelly.content;

import java.util.HashMap;
import java.util.Map;

import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.DatabaseSchema;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.db.DatabaseSchema.Mileages;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/**
 * Meant to be a single entry point to Wheelly Data API.
 */
public class ChronologyProvider extends ContentProvider {
	
	private static final int MILEAGES = 100;
	private static final int REFUELS = 200;
	private static final int HEARTBEATS = 300;
	private static final int LOCATIONS = 400;
	
	private static final Map<Integer, String> SqlTableMap = new HashMap<Integer, String>();
	private static final UriMatcher uriMatcher;
	
	static {
		final String a = DatabaseSchema.CONTENT_AUTHORITY;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(a, "mileages", MILEAGES);
		uriMatcher.addURI(a, "refuels", REFUELS);
		uriMatcher.addURI(a, "heartbeats", HEARTBEATS);
		uriMatcher.addURI(a, "locations", LOCATIONS);
		
		SqlTableMap.put(MILEAGES, Mileages.Tables);
		SqlTableMap.put(REFUELS, Refuels.Tables);
		SqlTableMap.put(HEARTBEATS, Heartbeats.Tables);
	}
	
	private SQLiteOpenHelper dbHelper;
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		final int match = uriMatcher.match(uri);
		switch (match) {
		case MILEAGES:
			return Mileages.CONTENT_TYPE;
		case REFUELS:
			return Refuels.CONTENT_TYPE;
		case HEARTBEATS:
			return Heartbeats.CONTENT_TYPE;
		case LOCATIONS:
			return "vnd.android.cursor.dir/vnd.financisto.location";
		}
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
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
			return dbHelper.getReadableDatabase().query(
				SqlTableMap.get(uriCode),
				projection, selection, selectionArgs,
				null, null,
				sortOrder);
		}
		
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}