package com.wheelly.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public final class LocationRepository implements IRepository {
	final SQLiteDatabase database;
	
	public LocationRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public Cursor list() {
		return this.database.rawQuery(DatabaseSchema.Locations.Select, null);
	}
	
	public long insert(ContentValues values) {
		values.remove(BaseColumns._ID);
		return this.database.insert("locations", null, values);
	}
	
	public void update(ContentValues values) {
		long id = values.getAsLong(BaseColumns._ID);
		values.remove(BaseColumns._ID);
		this.database.update(
			"locations",
			values,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) });
	}
	
	public ContentValues load(long id) {
		Cursor cursor = this.database.rawQuery(DatabaseSchema.Locations.Single, new String[] {
			Long.toString(id)
		});
		
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}
	
	public void delete(long id) {
		this.database.delete("locations",
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) }
		);
	}
	
	public ContentValues getDefaults() {
		return new ContentValues();
	}
	
	static ContentValues deserialize(Cursor cursor) {
		
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
	
	public static String locationToText(String provider, double latitude, double longitude, float accuracy, String resolvedAddress) {
			if (resolvedAddress != null) {
			return resolvedAddress;
		} else {
			return String.format("%1$s, Lat: %2$2f, Lon: %3$2f%4$s",
				provider,
				latitude,
				longitude,
				accuracy > 0 ? String.format(", ± %1$2f m", accuracy) : ""
			);
		}
	}
	
	public static String locationToText(Cursor cursor) {
		return
			locationToText(
				cursor.getString(cursor.getColumnIndexOrThrow("provider")),
				cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
				cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
				cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")),
				cursor.getString(cursor.getColumnIndexOrThrow("resolvedAddress"))
			);
	}

	@Override
	public long exists(ContentValues values) {
		// TODO Auto-generated method stub
		return 0;
	}
}
