package com.wheelly.content;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.android.apps.mytracks.content.TracksColumns;

public class TrackRepository {
	
	private final Context context;
	
	public TrackRepository(Context context) {
		this.context = context;
	}
	
	public Cursor list() {
		return context.getContentResolver().query(
			TracksColumns.CONTENT_URI, null, null, null, "_id DESC");
	}
	
	public long getDistance(long trackId) {
		final Cursor cursor = context.getContentResolver()
			.query(TracksColumns.CONTENT_URI,
				new String[] { TracksColumns.TOTALDISTANCE },
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(trackId) },
				null);
		try {
			cursor.moveToFirst();
			return cursor.getLong(cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE));
		} finally {
			cursor.close();
		}
	}
}