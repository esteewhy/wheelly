package com.wheelly.db;

import java.util.Date;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.util.DateUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

/**
 * Higher level abstraction over heartbeat persistence that accommodates
 * database connection management.
 */
public class HeartbeatBroker {
	
	private final Context context;
	
	public HeartbeatBroker(Context context)
	{
		this.context = context;
	}
	
	public ContentValues loadOrCreate(long id) {
		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor =
			id > 0
				? cr.query(
					ContentUris.withAppendedId(Heartbeats.CONTENT_URI, id),
					null, null, null, null)
				: cr.query(Timeline.CONTENT_URI,
					null, null, null,
					"_created DESC LIMIT 1");
		
		ContentValues values;
		try {
			if(cursor.moveToFirst()) {
				values = deserialize(cursor);
			} else {
				values = new ContentValues();
				values.put("odometer", 0);
				values.put("fuel", 0);
				values.put("place_id", -1);
			}
		} finally {
			cursor.close();
		}
		
		if(id <= 0) {
			values.put(BaseColumns._ID, -1);
			values.put("_created", DateUtils.dbFormat.format(new Date()));
			//enforce location auto-selection in editor
			///@todo move auto-selection logic down to BL
			///to provide fall-back to history value
			values.put("place_id", 0);
		}
		
		return values;
	}
	
	public long updateOrInsert(ContentValues values){
		long id;
		ContentResolver cr = context.getContentResolver();
		
		values.put("modified", System.currentTimeMillis() / 1000L);
		
		if(values.containsKey(BaseColumns._ID)
				&& (id = values.getAsLong(BaseColumns._ID)) > 0) {
			cr.update(
				ContentUris.withAppendedId(Heartbeats.CONTENT_URI, id),
				values, null, null);
			return id;
		} else {
			values.remove(BaseColumns._ID);
			return ContentUris.parseId(cr.insert(Heartbeats.CONTENT_URI, values));
		}
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("_created", cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("odometer", cursor.getLong(cursor.getColumnIndexOrThrow("odometer")));
		values.put("fuel", cursor.getLong(cursor.getColumnIndexOrThrow("fuel")));
		values.put("place_id", cursor.getLong(cursor.getColumnIndexOrThrow("place_id")));
		values.put("type", cursor.getLong(cursor.getColumnIndexOrThrow("type")));
		//stop hearbeat
		//values.put("distance", cursor.getFloat(cursor.getColumnIndexOrThrow("distance")));
		//values.put("track_id", cursor.getLong(cursor.getColumnIndexOrThrow("track_id")));
		//refuel
		//values.put("amount", cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		//values.put("unit_price", cursor.getFloat(cursor.getColumnIndexOrThrow("unit_price")));
		//values.put("cost", cursor.getFloat(cursor.getColumnIndexOrThrow("cost")));
		//values.put("transaction_id", cursor.getLong(cursor.getColumnIndexOrThrow("transaction_id")));
		
		return values;
	}
}