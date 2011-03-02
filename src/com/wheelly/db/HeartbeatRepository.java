package com.wheelly.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public final class HeartbeatRepository {
	final SQLiteDatabase database;
	
	public HeartbeatRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public long insert(ContentValues values) {
		return this.database.insert("heartbeats", null, values);
	}
}
