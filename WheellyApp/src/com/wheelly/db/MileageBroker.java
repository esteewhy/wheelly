package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Mileages;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
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
		
		final Cursor cursor =
			id > 0
				? cr.query(
					ContentUris.withAppendedId(Heartbeats.CONTENT_URI, id),
					Mileages.SingleEditProjection,
					"type = 2", null, null)
				: cr.query(Mileages.CONTENT_URI, Mileages.DefaultProjection,
					"type = 2", null, "_created DESC LIMIT 1");
		
		try {
			if(cursor.moveToFirst()) {
				return deserialize(cursor);
			} else {
				final ContentValues m = new ContentValues();
				m.put(BaseColumns._ID, 0);
				m.put("type", 2);
				m.put("name", "First mileage!");
				m.put("location_id", -1);
				return m;
			}
		} finally {
			cursor.close();
		}
	}
	
	public long updateOrInsert(ContentValues values) {
		long id;
		ContentResolver cr = context.getContentResolver();
		
		final int type = values.getAsInteger("type");
		if(!values.containsKey("type")|| type != 2) {
			values.put("type", 2);
		}
		
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
	
	public long getLastPendingId() {
		SQLiteDatabase db = null;
		try {
			db = new DatabaseHelper(this.context).getReadableDatabase();
			return db.compileStatement(Mileages.LastPendingIdSql).simpleQueryForLong();
		} catch(SQLiteDoneException e) {
			return -1;
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID,
				cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
		values.put("type", cursor.getInt(cursor.getColumnIndexOrThrow("type")));
		values.put("name",
				cursor.getString(cursor.getColumnIndexOrThrow("name")));
		values.put("_created",
				cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("track_id", cursor.getInt(cursor.getColumnIndex("track_id")));
		values.put("distance",
				cursor.getFloat(cursor.getColumnIndexOrThrow("distance")));
		values.put("amount",
				cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.put("location_id",
				cursor.getLong(cursor.getColumnIndexOrThrow("location_id")));
		return values;
	}
}