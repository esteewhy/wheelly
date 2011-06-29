package com.wheelly.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Mileages;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
				: cr.query(Heartbeats.CONTENT_URI,
					null, null, null,
					"_created DESC LIMIT 1");
		
		ContentValues values;
		if(cursor.moveToFirst()) {
			values = deserialize(cursor);
		} else {
			values = new ContentValues();
			values.put("odometer", 0);
			values.put("fuel", 0);
			values.put("place_id", -1);
			
		}
		values.put(BaseColumns._ID, -1);
		values.put("_created", new SimpleDateFormat(DatabaseSchema.DateTimeFormat).format(new Date()));
		return values;
	}
	
	public long updateOrInsert(ContentValues values){
		long id;
		ContentResolver cr = context.getContentResolver();
		
		if(values.containsKey(BaseColumns._ID)
				&& (id = values.getAsLong(BaseColumns._ID)) > 0) {
			cr.update(
				ContentUris.withAppendedId(Mileages.CONTENT_URI, id),
				values, null, null);
			return id;
		} else {
			values.remove(BaseColumns._ID);
			return ContentUris.parseId(cr.insert(Mileages.CONTENT_URI, values));
		}
	}
	
	public int referenceCount(final long id) {
		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor = cr.query(
			ContentUris.withAppendedId(Uri.withAppendedPath(Heartbeats.CONTENT_URI, "references"), id),
				null, null, null, null);
		return cursor.moveToFirst() ? cursor.getInt(0) : 0;
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("_created", cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("odometer", cursor.getLong(cursor.getColumnIndexOrThrow("odometer")));
		values.put("fuel", cursor.getLong(cursor.getColumnIndexOrThrow("fuel")));
		values.put("place_id", cursor.getLong(cursor.getColumnIndexOrThrow("place_id")));
		
		return values;
	}
}