package com.wheelly.app;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.wheelly.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
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
	
	public boolean importFromFile(String file) {
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openDatabase(new File(Environment.getExternalStorageDirectory().getPath() + "/andicar/backups/" + file).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
			
			//db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy)
		} finally {
			if(null != db) {
				db.close();
			}
		}
		
		return false;
	}
}
