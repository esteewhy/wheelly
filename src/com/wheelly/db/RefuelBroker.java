package com.wheelly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.BaseColumns;
import android.widget.Toast;

/**
 * Higher level abstraction over refuel persistence to accommodate
 * database connection management.
 */
public class RefuelBroker {
	
	final Context context;
	
	public RefuelBroker(Context context)
	{
		this.context = context;
	}
	
	public long updateOrInsert(ContentValues refuel, ContentValues heartbeat) {
		SQLiteDatabase db = null;
		long id = refuel.getAsLong(BaseColumns._ID);
		
		try {
			db = new DatabaseHelper(context).getWritableDatabase();
			
			refuel.put("heartbeat_id", resolveHeartbeatId(heartbeat, db));
			
			// Now save refuel itself.
			IRepository repository = new RefuelRepository(db);
		
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
	
	public void delete(long id) {
		SQLiteDatabase db = null;
		
		try {
			new RefuelRepository(
				db = new DatabaseHelper(this.context).getWritableDatabase()
			).delete(id);
		} finally {
			if(null != db) {
				db.close();
			}
		}
	}
	
	/**
	 * Save heartbeat first to obtain ID.
	 * 
	 * If this entity already exists, then update it.
	 * If the entity doesn't yet exist but the one with the same ODO/fuel's present,
	 *  then reuse it.
	 * Otherwise, insert new record and obtain it's ID.
	 */
	long resolveHeartbeatId(ContentValues heartbeat, SQLiteDatabase db) throws SQLiteException {
		long heartbeatId = heartbeat.getAsLong(BaseColumns._ID);
		
		final IRepository heartbeatRepository = new HeartbeatRepository(db);
		
		if(heartbeatId > 0) {
			heartbeatRepository.update(heartbeat);
		} else {
			heartbeatId = heartbeatRepository.exists(heartbeat);
			
			if(heartbeatId > 0) {
				final ContentValues existingHeartbeat = heartbeatRepository.load(heartbeatId);
				existingHeartbeat.put("_created", heartbeat.getAsString("_created"));
				heartbeatRepository.update(existingHeartbeat);
				
				Toast.makeText(context,
					"Reused existing heartbeat at "
					+ existingHeartbeat.getAsLong("odometer").toString()
					+ " km / "
					+ existingHeartbeat.getAsLong("fuel").toString()
					+ " l", 500)
					.show();
			} else {
				heartbeatId = heartbeatRepository.insert(heartbeat);
			}
		}
		
		return heartbeatId;
	}
}