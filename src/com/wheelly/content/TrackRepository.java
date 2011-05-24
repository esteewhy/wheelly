package com.wheelly.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.wheelly.db.LocationBroker;

public class TrackRepository {
	
	private final Context context;
	
	public TrackRepository(Context context) {
		this.context = context;
	}
	
	/**
	 * Checks if My Tracks content provider is available.
	 */
	public boolean checkAvailability() {
		return null != context.getContentResolver()
			.getType(TracksColumns.CONTENT_URI);
	}
	
	public Cursor list() {
		return context.getContentResolver().query(
			TracksColumns.CONTENT_URI, null, null, null, "_id DESC");
	}
	
	/*
	 * Retrieves track distance in kilometers.
	 */
	public long getDistance(long trackId) {
		final Cursor cursor = context.getContentResolver()
			.query(TracksColumns.CONTENT_URI,
				new String[] { TracksColumns.TOTALDISTANCE },
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(trackId) },
				null);
		try {
			cursor.moveToFirst();
			return cursor.getLong(cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE)) / 1000;
		} finally {
			cursor.close();
		}
	}
	
	/*
	 * Renames newly recorded track using start and stop location names.
	 */
	public void renameTrack(long trackId, long start_place_id, long stop_place_id) {
		final LocationBroker broker = new LocationBroker(context);
		final String name =
			broker.loadOrCreate(start_place_id).getAsString("name")
			+ " - "
			+ broker.loadOrCreate(stop_place_id).getAsString("name");
		ContentValues values = new ContentValues();
		values.put("name", name);
		context.getContentResolver().update(
			TracksColumns.CONTENT_URI,
			values,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(trackId)}
		);
	}
}