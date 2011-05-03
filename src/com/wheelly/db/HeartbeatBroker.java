package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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
		SQLiteDatabase db = null;
		try {
			db = new DatabaseHelper(this.context).getReadableDatabase();
			final HeartbeatRepository repository = new HeartbeatRepository(db);
			return id > 0 ? repository.load(id) : repository.getDefaults();
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
	
	public long updateOrInsert(ContentValues values){
		SQLiteDatabase db = null;
		final long id = values.getAsLong(BaseColumns._ID);
		
		try {
			final HeartbeatRepository repository = new HeartbeatRepository(
				db = new DatabaseHelper(this.context).getWritableDatabase()
			);
			
			if(id > 0) {
				repository.update(values);
				return id;
			} else {
				return repository.insert(values);
			}
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
}