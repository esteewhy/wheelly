package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.db.DatabaseSchema.Refuels;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, "wheelly.db", null, 1);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Mileages.Create);
		db.execSQL(Refuels.Create);
		db.execSQL(Heartbeats.Create);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
}
