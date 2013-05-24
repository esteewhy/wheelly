package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Locations;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
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
		final ContentResolver cr = context.getContentResolver();
		
		final Cursor cursor =
				cr.query(
					ContentUris.withAppendedId(Locations.CONTENT_URI, id),
					null, null, null, null);
		
		try {
			return
				cursor.moveToFirst()
					? deserialize(cursor)
					: new ContentValues();
		} finally {
			cursor.close();
		}

	}
	
	public long getNearest(Location location, float minDistance) {
		final Cursor locationCursor = context.getContentResolver().query(Locations.CONTENT_URI, null, null, null, null);
		long locationId = -1;
		
		try {
			if(locationCursor.moveToFirst()) {
				do {
					final float distance = location.distanceTo(new Location("existing") {{
						setLongitude(locationCursor.getDouble(locationCursor.getColumnIndex("longitude")));
						setLatitude(locationCursor.getDouble(locationCursor.getColumnIndex("latitude")));
					}});
					
					if(minDistance >= distance) {
						minDistance = distance;
						locationId = locationCursor.getLong(locationCursor.getColumnIndex(BaseColumns._ID)); 
					}
				} while(locationCursor.moveToNext());
			}
		} finally {
			locationCursor.close();
		}
		
		return locationId;
	}
	
	public long updateOrInsert(ContentValues values) {
		final long id = values.containsKey(BaseColumns._ID)
			? values.getAsLong(BaseColumns._ID) : -1;
		
		final ContentResolver cr = context.getContentResolver();
		
		if(id > 0) {
			cr.update(ContentUris.withAppendedId(Locations.CONTENT_URI, id), values, null, null);
			return id;
		} else {
			values.remove(BaseColumns._ID);
			return ContentUris.parseId(cr.insert(Locations.CONTENT_URI, values));
		}
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name"))); 	
		values.put("datetime", cursor.getLong(cursor.getColumnIndexOrThrow("datetime")));
		values.put("provider", cursor.getString(cursor.getColumnIndexOrThrow("provider")));
		values.put("accuracy", cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")));
		values.put("latitude", cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
		values.put("longitude", cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
		values.put("is_payee", cursor.getInt(cursor.getColumnIndexOrThrow("is_payee")));
		values.put("resolved_address", cursor.getString(cursor.getColumnIndexOrThrow("resolved_address")));
		
		return values;
	}
}