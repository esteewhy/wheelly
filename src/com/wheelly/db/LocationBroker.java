package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Higher level abstraction over location persistence that accommodates
 * database connection management.
 */
public class LocationBroker {
	
	private final Context context;
	
	public LocationBroker(Context context)
	{
		this.context = context;
	}
	
	public ContentValues loadOrCreate(long id) {
		SQLiteDatabase db = null;
		try {
			db = new DatabaseHelper(this.context).getReadableDatabase();
			final IRepository repository = new LocationRepository(db);
			return id > 0 ? repository.load(id) : repository.getDefaults();
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
	
	public long updateOrInsert(ContentValues values){
		SQLiteDatabase db = null;
		final long id = values.containsKey(BaseColumns._ID)
			? values.getAsLong(BaseColumns._ID) : -1;
		
		try {
			final IRepository repository = new LocationRepository(
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