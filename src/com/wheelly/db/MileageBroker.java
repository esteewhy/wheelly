package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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
	
	public long update(ContentValues mileage) {
		SQLiteDatabase db = null;
		long id = mileage.getAsLong(BaseColumns._ID);
		
		try {
			IRepository repository = new MileageRepository(
				db = new DatabaseHelper(context).getWritableDatabase()
			);
		
			if(id > 0) {
				repository.update(mileage);
				return id;
			} else {
				return repository.insert(mileage);
			}
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}

	public void delete(long id) {
		SQLiteDatabase db = null;
		
		try {
			new MileageRepository(
				db = new DatabaseHelper(context).getWritableDatabase()
			).delete(id);
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
}