package com.wheelly.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import com.wheelly.R;
import com.wheelly.app.StatusBarControls;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;

public class HeartbeatList extends ListActivity {
	private static final int NEW_REQUEST = 1;
	private static final int EDIT_REQUEST = 2;
	private SQLiteDatabase db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heartbeat_list);
		
		final Cursor cursor = new HeartbeatRepository(db = new DatabaseHelper(this).getReadableDatabase()).list();
		startManagingCursor(cursor);
		
		final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(this).getInt("fuel_capacity", 60);
		
		setListAdapter(
			new SimpleCursorAdapter(this, R.layout.heartbeat_list_item, cursor,
				new String[] {
					"odometer", "_created", "fuel", "fuel", "place", "icons"
				},
				new int[] {
					R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge, R.id.place, R.id.icons
				}
			) {{
				setViewBinder(new SimpleCursorAdapter.ViewBinder(){
					@Override
					public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
						switch(view.getId()) {
						case R.id.fuelGauge:
							ProgressBar pb = (ProgressBar)view; 
							pb.setProgress(cursor.getInt(columnIndex));
							pb.setMax(fuelCapacity);
							return true;
						case R.id.icons:
							int mask = cursor.getInt(columnIndex);
							view.findViewById(R.id.icon_refuel).setVisibility((mask & 4) > 0 ? View.VISIBLE : View.GONE);
							view.findViewById(R.id.icon_start).setVisibility((mask & 2) > 0 ? View.VISIBLE : View.GONE);
							view.findViewById(R.id.icon_stop).setVisibility((mask & 1) > 0 ? View.VISIBLE : View.GONE);
							return true;
						}
						return false;
					}
				});
			}}
		);
		registerForContextMenu(getListView());
		
		// Set up status bar (if present).
		final StatusBarControls c = new StatusBarControls(this);
		c.AddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HeartbeatList.this, Heartbeat.class);
				startActivityForResult(intent, NEW_REQUEST);
			}
		});
		c.TransferButton.setVisibility(View.GONE);
		c.TemplateButton.setVisibility(View.GONE);
		c.FilterButton.setVisibility(View.GONE);
		c.TotalLayout.setVisibility(View.GONE);
	}
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(this, Heartbeat.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode != RESULT_CANCELED) {
			((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.heartbeats_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
			case R.id.opt_menu_add:
				Intent intent = new Intent(HeartbeatList.this, Heartbeat.class);
				startActivityForResult(intent, NEW_REQUEST);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
		getMenuInflater().inflate(R.menu.context_menu, menu);
		menu.setHeaderTitle("Heartbeat");
		
		if(0 < new HeartbeatRepository(new DatabaseHelper(this).getReadableDatabase())
			.referenceCount(mi.id)) {
			menu.removeItem(R.id.ctx_menu_delete);
		}
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
		
		switch (item.getItemId()) {
			case R.id.ctx_menu_view: {
				//viewItem(mi.position, mi.id);
				return true;
			} 			
			case R.id.ctx_menu_edit:
				onListItemClick(null, null, 0, mi.id);
				return true;
			case R.id.ctx_menu_delete:
				new AlertDialog.Builder(this)
					.setMessage(R.string.delete_heartbeat_confirm)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							new HeartbeatRepository(new DatabaseHelper(HeartbeatList.this).getWritableDatabase())
								.delete(mi.id);
							onActivityResult(0, RESULT_OK, null);
						}
					})
					.setNegativeButton(R.string.no, null)
					.show();
				return true;
		}
		return false;
	}
}