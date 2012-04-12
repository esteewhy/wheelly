package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.db.DatabaseSchema.Refuels;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, "wheelly.db", null, 4);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Mileages.Create);
		db.execSQL(Refuels.Create);
		db.execSQL(Heartbeats.Create);
		db.execSQL(Locations.Create);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch(oldVersion) {
		case 1:
			db.execSQL(Locations.Create);
			break;
		case 2:
			db.execSQL("ALTER TABLE mileages ADD location_id LONG;");
			break;
		case 3:
			db.execSQL("ALTER TABLE heartbeats ADD sync_id TEXT;");
			db.execSQL("ALTER TABLE heartbeats ADD sync_date TIMESTAMP;");
		}
	}
}