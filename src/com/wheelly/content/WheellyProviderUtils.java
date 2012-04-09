package com.wheelly.content;

import com.wheelly.db.DatabaseSchema.Timeline;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

public class WheellyProviderUtils {
	protected Context context;
	
	public WheellyProviderUtils(Context context) {
		this.context = context;
	}
	
	public Cursor getSyncCursor(long heartbeatId) {
		final ContentResolver cr = context.getContentResolver();
		// Get the track from the provider:
		return
			heartbeatId > 0
				? cr.query(Timeline.CONTENT_URI, Timeline.ListProjection, "h._id = ?",
						new String[] { Long.toString(heartbeatId) }, null)
				: cr.query(Timeline.CONTENT_URI, Timeline.ListProjection, null, null,
						"odometer ASC");
	}
}
