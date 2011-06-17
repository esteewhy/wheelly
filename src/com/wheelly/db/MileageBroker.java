package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Mileages;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Higher level abstraction over trip persistence to accommodate
 * database connection management.
 */
public class MileageBroker {
	
	private final Context context;
	
	public MileageBroker(Context context)
	{
		this.context = context;
	}
	
	public ContentValues loadOrCreate(long id) {
		final ContentResolver cr = context.getContentResolver();
		final Uri uri = ContentUris.appendId(Mileages.CONTENT_URI.buildUpon(), id).build();
		final Cursor cursor =
			id > 0
				? cr.query(uri, Mileages.SingleProjection,
					BaseColumns._ID + " = ?",
					new String[] { Long.toString(id) },
					null)
				: cr.query(uri, Mileages.DefaultProjection,
					null, null, "_created DESC LIMIT 1");
		
		try {
			if(cursor.moveToFirst()) {
				return deserialize(cursor);
			} else {
				final ContentValues m = new ContentValues();
				m.put(BaseColumns._ID, 0);
				m.put("name", "First mileage!");
				m.put("track_id", 0);
				m.put("start_heartbeat_id", 0);
				m.put("stop_heartbeat_id", 0);
				m.put("mileage", 0);
				m.put("amount", 0);
				m.put("calc_cost", 0);
				m.put("calc_amount", 0);
				m.put("location_id", -1);
				return m;
			}
		} finally {
			cursor.close();
		}
	}
	
	public long updateOrInsert(ContentValues mileage) {
		long id = mileage.getAsLong(BaseColumns._ID);
		ContentResolver cr = context.getContentResolver();
		
		if(id > 0) {
			cr.update(
				ContentUris.withAppendedId(Mileages.CONTENT_URI, id),
				mileage, null, null);
			return id;
		} else {
			mileage.remove(BaseColumns._ID);
			return ContentUris.parseId(cr.insert(Mileages.CONTENT_URI, mileage));
		}
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID,
				cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
		values.put("name",
				cursor.getString(cursor.getColumnIndexOrThrow("name")));
		values.put("_created",
				cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("track_id", cursor.getInt(cursor.getColumnIndex("track_id")));
		values.put("start_heartbeat_id", cursor.getLong(cursor
				.getColumnIndexOrThrow("start_heartbeat_id")));
		values.put("stop_heartbeat_id", cursor.getLong(cursor
				.getColumnIndexOrThrow("stop_heartbeat_id")));
		values.put("mileage",
				cursor.getFloat(cursor.getColumnIndexOrThrow("mileage")));
		values.put("amount",
				cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.put("calc_cost",
				cursor.getFloat(cursor.getColumnIndexOrThrow("calc_cost")));
		values.put("calc_amount",
				cursor.getFloat(cursor.getColumnIndexOrThrow("calc_amount")));
		values.put("location_id",
				cursor.getLong(cursor.getColumnIndexOrThrow("location_id")));
		return values;
	}
}