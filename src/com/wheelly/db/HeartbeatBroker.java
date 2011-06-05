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
			final IRepository repository = new HeartbeatRepository(
				db = new DatabaseHelper(this.context).getReadableDatabase()
			);
			return id > 0 ? repository.load(id) : repository.getDefaults();
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
	
	public long updateOrInsert(ContentValues values){
		SQLiteDatabase db = null;
		final long id = values.containsKey(BaseColumns._ID) ? values.getAsLong(BaseColumns._ID) : 0;
		
		try {
			final IRepository repository = new HeartbeatRepository(
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