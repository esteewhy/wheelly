package com.wheelly.app;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Refuels;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.widget.Toast;

public final class AndiCarImporter {
	private final Activity context;
	
	public AndiCarImporter(Activity context) {
		this.context = context;
	}
	
	public List<String> listFiles() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + "/andicar/backups");
			if(fileDir.exists() && fileDir.isDirectory()) {
				String[] files = fileDir.list();
				if(files.length > 0) {
					List<String> list = Arrays.asList(files);
					Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
					Collections.reverse(list);
					return list;
				}  else {
					Toast.makeText(context, R.string.andicar_no_backups, Toast.LENGTH_LONG).show();
				}
			}
		}
		return Arrays.asList(new String[0]);
	}
	
	public void attemptImporting() {
		List<String> list = listFiles();
		if(list.size() > 0) {
			SharedPreferences prefs = context.getPreferences(Activity.MODE_PRIVATE);
			
			if(!prefs.contains("andicar_last_sync_file")
					|| 0 < Collections.binarySearch(list, prefs.getString("andicar_last_sync_file", null))) {
				importFromFile(list.get(0));
			}
		}
	}
	
	final static short Theirs = 0;
	final static short Mine = 1;
	final static short WE_GO = 1;
	final static short THEY_GO = 2;
	
	public boolean importFromFile(String file) {
		final SQLiteDatabase[] db = { null, null };
		
		try {
			db[Theirs] = SQLiteDatabase.openDatabase(new File(Environment.getExternalStorageDirectory().getPath() + "/andicar/backups/" + file).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
			db[Mine] = new DatabaseHelper(context).getWritableDatabase();
			
			importRefuels(db);
			
		} finally {
			for(SQLiteDatabase i : db) if(i != null) i.close();
		}
		
		return false;
	}
	
	private void importRefuels(SQLiteDatabase[] db) {
		final Cursor[] cursor = {
			db[Theirs].query("CAR_REFUEL",
				"CarIndex,Quantity,Price,DATETIME(Date, 'unixepoch') AS Date,Amount".split(","),
				null, null, null, null, "CarIndex DESC"),
			db[Mine].rawQuery(
				"SELECT h.odometer FROM heartbeats h"
				+ " INNER JOIN refuels r ON r.heartbeat_id = h." + BaseColumns._ID
				+ " ORDER BY h.odometer DESC", null)
		};
		
		long[] odo = { 0, 0 };
		short move = WE_GO | THEY_GO;
		boolean commit = false;
		
		while(true) {
			if(commit) {
				commit = false;
				importRefuel(cursor[Theirs]);
			}
			
			if((move & THEY_GO) != 0) {
				if(cursor[Theirs].moveToNext()) {
					odo[Theirs] = cursor[Theirs].getLong(cursor[Theirs].getColumnIndexOrThrow("CarIndex"));
				} else break;
			}
			
			if((move & WE_GO) != 0) {
				if(cursor[Mine].moveToNext()) {
					odo[Mine] = cursor[Mine].getLong(cursor[Mine].getColumnIndexOrThrow("odometer"));
				} else {
					commit = true;
					move = WE_GO | THEY_GO;
					continue;
				}
			}
			
			if(odo[Theirs] > odo[Mine]) {
				move = THEY_GO;
			} else if(odo[Theirs] == odo[Mine]) {
				move = WE_GO | THEY_GO;
			} else {
				commit = move == THEY_GO;
				move = WE_GO;
			}
		}
		
		for(Cursor i : cursor) i.close();
	}
	
	private static ContentValues deserializeTheirRefuel(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put("CarIndex",	cursor.getLong(cursor.getColumnIndexOrThrow("CarIndex")));
		values.put("Quantity",	cursor.getFloat(cursor.getColumnIndexOrThrow("Quantity")));
		values.put("Amount",	cursor.getFloat(cursor.getColumnIndexOrThrow("Amount")));
		values.put("Date",		cursor.getString(cursor.getColumnIndexOrThrow("Date")));
		values.put("Price",		cursor.getFloat(cursor.getColumnIndexOrThrow("Price")));
		return values;
	}
	
	private void importRefuel(Cursor cursor) {
		final long full_tank = PreferenceManager.getDefaultSharedPreferences(context).getInt("fuel_capacity", 60);
		
		final ContentValues values = deserializeTheirRefuel(cursor);
		
		final ContentValues heartbeat = new ContentValues();
		heartbeat.put("_created",	values.getAsString("Date"));
		heartbeat.put("odometer",	values.getAsLong("CarIndex"));
		heartbeat.put("fuel",		full_tank);
		
		final ContentResolver cr = context.getContentResolver();
		
		final ContentValues refuel = new ContentValues();
		refuel.put("heartbeat_id",	ContentUris.parseId(cr.insert(Heartbeats.CONTENT_URI, heartbeat)));
		refuel.put("amount",		values.getAsFloat("Quantity"));
		refuel.put("unit_price",	values.getAsFloat("Price"));
		refuel.put("cost",			values.getAsFloat("Amount"));
		refuel.put("name",			"imported from AndiCar");
		
		cr.insert(Refuels.CONTENT_URI, refuel);
	}
}