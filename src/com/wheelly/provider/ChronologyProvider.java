package com.wheelly.provider;

import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;
import com.wheelly.db.MileageRepository;
import com.wheelly.db.RefuelRepository;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class ChronologyProvider extends ContentProvider {
	
	private static final int MILEAGES = 100;
	private static final int REFUELS = 200;
	private static final int HEARTBEATS = 300;
	private static final int LOCATIONS = 400;
	
	private static final UriMatcher uriMatcher;
	
	static {
		final String a = ChronologyContract.CONTENT_AUTORITY;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(a, "mileages", MILEAGES);
		uriMatcher.addURI(a, "refuels", REFUELS);
		uriMatcher.addURI(a, "heartbeats", HEARTBEATS);
		uriMatcher.addURI(a, "locations", LOCATIONS);
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
			return "vnd.android.cursor.dir/vnd.wheelly.mileage";
		case REFUELS:
			return "vnd.android.cursor.dir/vnd.wheelly.refuel";
		case HEARTBEATS:
			return "vnd.android.cursor.dir/vnd.wheelly.heartbeat";
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
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		switch(uriMatcher.match(uri)) {
		case MILEAGES:
			return new MileageRepository(db).list();
		case REFUELS:
			return new RefuelRepository(db, getContext()).list();
		case HEARTBEATS:
			return new HeartbeatRepository(db).list();
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