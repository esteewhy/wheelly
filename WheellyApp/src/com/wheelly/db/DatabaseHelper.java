package com.wheelly.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private final AssetManager assetManager;

	public DatabaseHelper(Context context) {
		super(context, "wheelly.db", null, 21);
		this.assetManager = context.getAssets();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			runScript(db, readFile("db/create/heartbeats.sql"));
			runScript(db, readFile("db/create/locations.sql"));
			runScript(db, readFile("db/views/next_event.sql"));
			runScript(db, readFile("db/views/prev_event.sql"));
			runScript(db, readFile("db/views/start_events.sql"));
			runScript(db, readFile("db/views/stop_events.sql"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private static void runScript(SQLiteDatabase db, String script)
			throws IOException {
		String[] content = script.split(";");
		for (String s : content) {
			String sql = s.trim();
			if (sql.length() > 1) {
				try {
					db.execSQL(sql);
				} catch (SQLiteException ex) {
					Log.e("DatabaseSchema", "Unable to run sql: " + sql, ex);
					throw ex;
				}
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
}