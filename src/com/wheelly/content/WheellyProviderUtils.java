package com.wheelly.content;

import java.util.Date;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.util.DateUtils;

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
						"h._id = ?",// AND sync_state != 3",
						new String[] { Long.toString(heartbeatId) }, null)
				: cr.query(Timeline.CONTENT_URI, Timeline.ListProjection,
						"icons > 0",// AND sync_state != 3",
						null,
						"odometer ASC, h._created ASC");
	}
	
	public Cursor getLatestRecords(String lastDate) {
		lastDate = DateUtils.dbFormat.format(new Date(lastDate));
		return cr.query(Timeline.CONTENT_URI, Timeline.ListProjection,
				"h._created > DATETIME(?) AND icons > 0",
				new String[] { lastDate },
				"odometer ASC, h._created ASC");
	}
	
	public void resetSync(long id) {
		final ContentValues values = new ContentValues();
		values.put("sync_id", (String)null);
		values.put("sync_etag", (String)null);
		values.put("sync_state", 0);
		cr.update(
				id > 0
					? ContentUris.withAppendedId(Heartbeats.CONTENT_URI, id)
					: Heartbeats.CONTENT_URI,
				values, null, null);
	}
}
