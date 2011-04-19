package com.wheelly.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.RefuelBroker;
import com.wheelly.db.RefuelRepository;

public class RefuelList extends ListActivity {

	private static final int NEW_REQUEST = 1;
	protected static final int EDIT_REQUEST = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.refuel_list);
		
		final Cursor cursor = new RefuelRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		super.startManagingCursor(cursor);
		
		super.setListAdapter(
			new SimpleCursorAdapter(this, R.layout.refuel_list_item, cursor,
				new String[] {
					"name", "calc_mileage", "cost", "_created", "amount"
				},
				new int[] {
					R.id.name, R.id.mileage, R.id.cost, R.id.date, R.id.consumption
				}
			)
		);
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(RefuelList.this, Refuel.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.refuels_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.opt_menu_add:
				Intent intent = new Intent(this, Refuel.class);
				startActivityForResult(intent, NEW_REQUEST);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		getMenuInflater().inflate(R.menu.context_menu, menu);
		menu.setHeaderTitle("Mileages");
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
					.setMessage(R.string.delete_mileage_confirm)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							new RefuelBroker(RefuelList.this).delete(mi.id);
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