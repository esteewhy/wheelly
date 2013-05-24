package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Locations;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class LocationRepository {
	private final SQLiteDatabase database;
	
	public LocationRepository(SQLiteDatabase database) {
		this.database = database;
	}
	
	public Cursor list(String filter) {
		String sql =
			"mileages".equalsIgnoreCase(filter)
				? Locations.SelectByMileages
				: "refuels".equalsIgnoreCase(filter)
					? Locations.SelectByRefuels
					: Locations.Select;
		
		return this.database.rawQuery(sql, null);
	}
}