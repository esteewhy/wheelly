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
		super(context, "wheelly.db", null, 12);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Mileages.Create);
		db.execSQL(Refuels.Create);
		db.execSQL(Heartbeats.Create);
		db.execSQL(Locations.Create);
		
		db.execSQL(Mileages.NextMileageView);
		db.execSQL(Mileages.PrevMileageView);
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
			break;
		case 4:
			db.execSQL("ALTER TABLE heartbeats ADD sync_etag TEXT;");
			break;
		case 5:
			db.execSQL("ALTER TABLE heartbeats ADD sync_state LONG NOT NULL DEFAULT 0;");
			db.execSQL("update heartbeats SET sync_state=(SELECT CASE"
+" WHEN DATETIME(COALESCE(m1._modified, m2._modified, r._modified)) >"
+" DATETIME(sync_date) AND sync_id IS NOT NULL AND sync_etag IS NOT NULL THEN 2"
+" WHEN sync_etag IS NOT NULL THEN 1"
+" WHEN sync_id IS NOT NULL THEN 3"
+" ELSE 0"
+" END FROM heartbeats h"
+" LEFT JOIN mileages m1 ON m1.start_heartbeat_id = h._id"
+" LEFT JOIN mileages m2 ON m2.stop_heartbeat_id = h._id"
+" LEFT JOIN refuels r ON r.heartbeat_id = h._id"
+" WHERE heartbeats._id == h._id)");
			break;
		case 6:
			db.execSQL(Mileages.NextMileageView);
			break;
		case 7:
			db.execSQL("DROP VIEW next_mileages;");
			db.execSQL(Mileages.NextMileageView);
		case 8:
			db.execSQL("ALTER TABLE locations ADD COLUMN color TEXT;");
			break;
		case 9:
			db.execSQL(Mileages.PrevMileageView);
			break;
		case 10:
			db.execSQL("ALTER TABLE heartbeats ADD COLUMN modified TIMESTAMP;");
			db.execSQL("UPDATE heartbeats SET modified = strftime('%s', 'now');");
		case 11:
			db.execSQL("ALTER TABLE locations ADD column modified TIMESTAMP;");
			db.execSQL("UPDATE locations SET modified = strftime('%s', 'now');");
			db.execSQL("ALTER TABLE locations ADD column sync_etag TEXT;");
		}
	}
}