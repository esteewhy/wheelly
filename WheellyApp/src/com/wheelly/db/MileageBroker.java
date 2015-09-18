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
					null, null, null, null)
				: cr.query(Mileages.CONTENT_URI, Mileages.DefaultProjection,
					null, null, "_created DESC LIMIT 1");
		
		try {
			if(cursor.moveToFirst()) {
				return deserialize(cursor);
			} else {
				final ContentValues m = new ContentValues();
				m.put(BaseColumns._ID, 0);
				m.put("type", 1);
				m.put("name", "First mileage!");
				m.put("place_id", -1);
				return m;
			}
		} finally {
			cursor.close();
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
		values.put("distance", cursor.getFloat(cursor.getColumnIndexOrThrow("distance")));
		values.put("track_id", cursor.getLong(cursor.getColumnIndexOrThrow("track_id")));
		values.put("amount", cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.putAll(HeartbeatBroker.deserialize(cursor));
		return values;
	}
}