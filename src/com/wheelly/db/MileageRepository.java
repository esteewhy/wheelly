package com.wheelly.db;

import java.util.Date;

import com.wheelly.db.DatabaseSchema.Mileages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class MileageRepository {
	final SQLiteDatabase database;
	
	public MileageRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public Cursor list() {
		return this.database.rawQuery(Mileages.Select, null);
	}
	
	public ContentValues load(long id) {
		Cursor cursor = this.database.rawQuery(
			Mileages.Single, new String[] { ((Object)id).toString()});
		ContentValues values = new ContentValues();
		values.put("name",			cursor.getString(cursor.getColumnIndex("name")));
		values.put("track_id",		cursor.getInt(cursor.getColumnIndex("track_id")));
		values.put("start_time",	Date.parse(cursor.getString(cursor.getColumnIndex("start_time"))));
		values.put("start_place_id", cursor.getLong(cursor.getColumnIndex("start_place_id")));
		values.put("stop_time",		Date.parse(cursor.getString(cursor.getColumnIndex("stop_time"))));
		values.put("stop_place_id",	cursor.getLong(cursor.getColumnIndex("stop_place_id")));
		values.put("mileage",		cursor.getFloat(cursor.getColumnIndex("mileage")));
		values.put("amount",		cursor.getFloat(cursor.getColumnIndex("amount")));
		values.put("calc_cost",		cursor.getFloat(cursor.getColumnIndex("calc_cost")));
		values.put("calc_amount",	cursor.getFloat(cursor.getColumnIndex("calc_amount")));
		return values;
	}
}