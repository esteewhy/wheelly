package com.wheelly.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class BackupHelper {
	final Context context;
	
	public BackupHelper(Context context) {
		this.context = context;
	}
	
	public boolean backup() {
		final String fileName = "wheelly-" + DateUtils.dbFormat.format(new Date()).replace(":", "-") + ".db";
		SQLiteDatabase db = null;
		try {
			db = new DatabaseHelper(context).getReadableDatabase(); 
			return BackupUtils.copyDatabase(db.getPath(),
				Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName
			);
		} finally {
			if(null != db) {
				db.close();
			}
			Toast.makeText(context, "Backup file created: " + fileName, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void restore() {
		final File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		final List<String> fileList = new ArrayList<String>();
		fileList.clear();
		for (File file : root.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".db") && filename.startsWith("wheelly");
			}
			
		})) {
			fileList.add(file.getName());  
		}
		
		new AlertDialog.Builder(context)
			.setSingleChoiceItems(
				new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, fileList),
				-1,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if(which >= 0) {
							BackupUtils.copyDatabase(
								root.getAbsolutePath() + "/" + fileList.get(which),
								new DatabaseHelper(context)
									.getReadableDatabase()
									.getPath()
							);
						}
					}
				}
			)
			.setTitle(R.string.tracks)
			.show();
	}
}