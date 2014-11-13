package com.wheelly.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.db.DatabaseSchema.Timeline;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private final AssetManager assetManager;
	
	public DatabaseHelper(Context context) {
		super(context, "wheelly.db", null, 19);
		this.assetManager = context.getAssets();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Mileages.Create);
		db.execSQL(Refuels.Create);
		try {
			runScript(db, readFile("db/create/heartbeats.sql"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.execSQL(Locations.Create);
		
		db.execSQL(Mileages.NextMileageView);
		db.execSQL(Mileages.PrevMileageView);
		
		db.execSQL(Timeline.PrevEventView);
		db.execSQL(Timeline.NextEventView);
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
			break;
		case 12:
			db.execSQL(Timeline.PrevEventView);
			db.execSQL(Timeline.NextEventView);
			break;
		case 13:
		case 14:
		case 15:
		case 16:
		case 17:
			db.execSQL("DROP VIEW prev_event;");
			db.execSQL("DROP VIEW next_event;");
			db.execSQL(Timeline.PrevEventView);
			db.execSQL(Timeline.NextEventView);
			break;
		case 18:
			try {
				runScript(db, readFile("db/create/heartbeats.sql").replace("heartbeats", "heartbeats_t"));
				runScript(db, readFile("db/alter/20141112-upgrade_heartbeats.sql"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String readFile(String scriptFile) throws IOException {
		StringBuilder sb = new StringBuilder();
		InputStream is = assetManager.open(scriptFile);
		Scanner scanner = new Scanner(is);
		try {
			while (scanner.hasNextLine()) {
				sb.append(scanner.nextLine().trim()).append(" ");
			}
		} finally {
			scanner.close();
			is.close();
		}
		return sb.toString().trim();
	}
	
	private static void runScript(SQLiteDatabase db, String script) throws IOException {
		String[] content = script.split(";");
		for (String s : content) {
			String sql = s.trim();
			if (sql.length() > 1) {
				try {
					db.execSQL(sql);
				} catch (SQLiteException ex) {
					Log.e("DatabaseSchema", "Unable to run sql: "+sql, ex);
					throw ex;
				}
			}
		}
	}
}