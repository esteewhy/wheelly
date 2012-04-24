package com.wheelly.content;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Timeline;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class WheellyProviderUtils {
	protected final Context context;
	protected final ContentResolver cr;
	
	public WheellyProviderUtils(Context context) {
		this.context = context;
		this.cr = context.getContentResolver();
	}
	
	public Cursor getSyncCursor(long heartbeatId) {
		// Get the track from the provider:
		return
			heartbeatId > 0
				? cr.query(Timeline.CONTENT_URI, Timeline.ListProjection,
						"h._id = ?",
						new String[] { Long.toString(heartbeatId) }, null)
				: cr.query(Timeline.CONTENT_URI, Timeline.ListProjection,
						"icons > 0",
						null,
						"odometer ASC");
	}
	
	public Cursor getLatestRecords(long lastOdometer) {
		return cr.query(Timeline.CONTENT_URI, Timeline.ListProjection,
				"h.odometer > ? AND icons > 0",
				new String[] { Long.toString(lastOdometer) },
				"odometer ASC");
	}
	
	public void resetSync(long id) {
		final ContentValues values = new ContentValues();
		values.put("sync_id", (String)null);
		values.put("sync_etag", (String)null);
		cr.update(
				id > 0
					? ContentUris.withAppendedId(Heartbeats.CONTENT_URI, id)
					: Heartbeats.CONTENT_URI,
				values, null, null);
	}
}
