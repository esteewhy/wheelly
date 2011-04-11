package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class TripController {
	
	final Context context;
	
	public TripController(Context context)
	{
		this.context = context;
	}
	
	public long updateTrip(ContentValues values) {
		SQLiteDatabase db = null;
		long id = values.getAsLong(BaseColumns._ID);
		
		try {
			
			MileageRepository repository = new MileageRepository(
				db = new DatabaseHelper(context).getWritableDatabase()
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
