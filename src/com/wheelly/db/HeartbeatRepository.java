package com.wheelly.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public final class HeartbeatRepository {
	final SQLiteDatabase database;
	
	public HeartbeatRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public Cursor list() {
		return this.database.rawQuery(DatabaseSchema.Heartbeats.Select, null);
	}
	
	public long insert(ContentValues values) {
		values.remove(BaseColumns._ID);
		return this.database.insert("heartbeats", null, values);
	}
	
	public void update(ContentValues values) {
		long id = values.getAsLong(BaseColumns._ID);
		values.remove(BaseColumns._ID);
		this.database.update(
			"heartbeats",
			values,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) });
	}
	
	public ContentValues load(long id) {
		Cursor cursor = this.database.rawQuery(DatabaseSchema.Heartbeats.Single, new String[] {
			Long.toString(id)
		});
		
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}
	
	public ContentValues getDefaults() {
		Cursor cursor = this.database.rawQuery(DatabaseSchema.Heartbeats.Defaults, null);
		try {
			ContentValues values;
			if(cursor.moveToFirst()) {
				values = deserialize(cursor);
			} else {
				values = new ContentValues();
				values.put("odometer", 0);
				values.put("fuel", 0);
				
			}
			values.put(BaseColumns._ID, -1);
			values.put("_created", new SimpleDateFormat(DatabaseSchema.DateTimeFormat).format(new Date()));
			return values;
		} finally {
			cursor.close();
		}
	}
	
	static ContentValues deserialize(Cursor cursor) {
		
		ContentValues values = new ContentValues();
		
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("_created", cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("odometer", cursor.getLong(cursor.getColumnIndexOrThrow("odometer")));
		values.put("fuel", cursor.getLong(cursor.getColumnIndexOrThrow("fuel")));
		
		return values;
	}
}
