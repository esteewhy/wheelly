package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Higher level abstraction over refuel persistence to accommodate
 * database connection management.
 */
public class RefuelController {
	
	final Context context;
	
	public RefuelController(Context context)
	{
		this.context = context;
	}
	
	public long update(ContentValues refuel, ContentValues heartbeat) {
		SQLiteDatabase db = null;
		long heartbeatId = heartbeat.getAsLong(BaseColumns._ID);
		long id = refuel.getAsLong(BaseColumns._ID);
		
		try {
			
			// Save heartbeat first to obtain ID.
			IRepository heartbeatRepository = new HeartbeatRepository(
				db = new DatabaseHelper(context).getWritableDatabase()
			);
			
			if(heartbeatId > 0) {
				heartbeatRepository.update(heartbeat);
			} else {
				heartbeatId = heartbeatRepository.insert(heartbeat);
			}
			
			refuel.put("heartbeat_id", heartbeatId);
			
			// Now save refuel itself.
			IRepository repository = new MileageRepository(db);
		
			if(id > 0) {
				repository.update(refuel);
				return id;
			} else {
				return repository.insert(refuel);
			}
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
}