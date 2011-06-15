package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Mileages;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Basic persistance operations over trip entity.
 */
public final class MileageRepository implements IRepository {
	private final SQLiteDatabase database;
	
	/**
	 * @database Initialised instance of SQLite database for which the caller is
	 *           responsible for managing life-cycle.
	 */
	public MileageRepository(SQLiteDatabase database) {
		this.database = database;
	}

	@Deprecated
	public Cursor list() {
		return null;
	}

	public ContentValues load(long id) {
		Cursor cursor = this.database.rawQuery(Mileages.Single,
				new String[] { ((Object) id).toString() });
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}
	
	@Deprecated
	public void delete(long id) {
	}

	public ContentValues getDefaults() {
		Cursor cursor = this.database.rawQuery(
				DatabaseSchema.Mileages.Defaults, null);
		try {
			cursor.moveToFirst();
			return deserialize(cursor);
		} finally {
			cursor.close();
		}
	}

	public long insert(ContentValues values) {
		values.remove(BaseColumns._ID);
		return this.database.insert("mileages", null, values);
	}

	public void update(ContentValues values) {
		long id = values.getAsLong(BaseColumns._ID);

		for (String column : new String[] { "stop_place_id", "start_place_id" }) {
			values.remove(column);
		}

		this.database.update("mileages", values, BaseColumns._ID + " = ?",
				new String[] { Long.toString(id) });
	}

	public long exists(ContentValues values) {
		return 0;
	}

	private static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID,
				cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
		values.put("name",
				cursor.getString(cursor.getColumnIndexOrThrow("name")));
		values.put("_created",
				cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("track_id", cursor.getInt(cursor.getColumnIndex("track_id")));
		values.put("start_heartbeat_id", cursor.getLong(cursor
				.getColumnIndexOrThrow("start_heartbeat_id")));
		values.put("stop_heartbeat_id", cursor.getLong(cursor
				.getColumnIndexOrThrow("stop_heartbeat_id")));
		values.put("mileage",
				cursor.getFloat(cursor.getColumnIndexOrThrow("mileage")));
		values.put("amount",
				cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.put("calc_cost",
				cursor.getFloat(cursor.getColumnIndexOrThrow("calc_cost")));
		values.put("calc_amount",
				cursor.getFloat(cursor.getColumnIndexOrThrow("calc_amount")));
		values.put("location_id",
				cursor.getLong(cursor.getColumnIndexOrThrow("location_id")));
		return values;
	}
}