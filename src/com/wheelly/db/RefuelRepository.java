package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Refuels;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Basic persistence operations over fuel refill entity.
 */
public final class RefuelRepository implements IRepository {
	final SQLiteDatabase database;
	
	public RefuelRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public Cursor list() {
		return this.database.rawQuery(Refuels.Select, null);
	}
	
	public ContentValues load(long id) {
		Cursor cursor = this.database.rawQuery(Refuels.Single, new String[] {
			((Object)id).toString()
		});
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}
	
	public void delete(long id) {
		this.database.delete("refuels",
			BaseColumns._ID,
			new String[] { Long.toString(id) }
		);
	}
	
	public ContentValues getDefaults() {
		Cursor cursor = this.database.rawQuery(DatabaseSchema.Refuels.Defaults, null);
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}
	
	public long insert(ContentValues values) {
		values.remove(BaseColumns._ID);
		return this.database.insert("refuels", null, values);
	}
	
	public void update(ContentValues values) {
		long id = values.getAsLong(BaseColumns._ID);
		
		for(String column : new String[] {BaseColumns._ID}) {
			values.remove(column);
		}
		
		this.database.update(
			"refuels",
			values,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) });
	}
	
	public long exists(ContentValues values) {
		return 0;
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("name",			cursor.getString(cursor.getColumnIndexOrThrow("name")));
		values.put("_created",		cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("transaction_id",cursor.getLong(cursor.getColumnIndexOrThrow("transaction_id")));
		values.put("heartbeat_id",	cursor.getLong(cursor.getColumnIndexOrThrow("heartbeat_id")));
		values.put("calc_mileage",	cursor.getFloat(cursor.getColumnIndexOrThrow("calc_mileage")));
		values.put("amount",		cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.put("cost",			cursor.getFloat(cursor.getColumnIndexOrThrow("cost")));
		values.put("unit_price",	cursor.getFloat(cursor.getColumnIndexOrThrow("unit_price")));
		return values;
	}
}