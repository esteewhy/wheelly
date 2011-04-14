package com.wheelly.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;

public class HeartbeatList extends ListActivity {

	private static final int NEW_HEARTBEAT_REQUEST = 1;
	protected static final int EDIT_HEARTBEAT_REQUEST = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.heartbeat_list);
		
		final Cursor cursor = new HeartbeatRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		super.startManagingCursor(cursor);
		
		final SimpleCursorAdapter adapter =
			new SimpleCursorAdapter(this, R.layout.heartbeat_list_item, cursor,
				new String[] { "odometer", "_created", "fuel", "fuel" },
				new int[] { R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge }
			); 
		
		// Some customisation is necessary to bind int to ProgressBar...
		final String[] columnNames = cursor.getColumnNames();
		
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if("fuel".equals(columnNames[columnIndex])) {
					ProgressBar pb = ((ProgressBar)view.findViewById(R.id.fuelGauge));
					if(null != pb) {
						pb.setProgress(cursor.getInt(columnIndex));
						return true;
					}
				}
				return false;
			}
		});
		
		super.setListAdapter(adapter);
		
		((ImageButton)findViewById(R.id.bAdd)).setOnClickListener(
			new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(HeartbeatList.this, Heartbeat.class);
					startActivityForResult(intent, NEW_HEARTBEAT_REQUEST);
				}
		});
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(this, Heartbeat.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_HEARTBEAT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
	}
}