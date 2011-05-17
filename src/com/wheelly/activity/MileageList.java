package com.wheelly.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.wheelly.R;
import com.wheelly.app.StatusBarControls;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.MileageRepository;
import com.wheelly.db.MileageBroker;

public class MileageList extends ListActivity {

	private static final int NEW_REQUEST = 1;
	private static final int EDIT_REQUEST = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mileage_list);
		
		final Cursor cursor = new MileageRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		startManagingCursor(cursor);
		
		setListAdapter(
			new SimpleCursorAdapter(this, R.layout.mileage_list_item, cursor,
				new String[] {
					"start_place", "stop_place", "mileage", "cost", "stop_time", "fuel", "destination"
				},
				new int[] {
					R.id.start_place, R.id.stop_place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.destination
				}
			) {
				@Override
				public void setViewText(TextView v, String text) {
					switch(v.getId()) {
					case R.id.mileage:
						int val = Integer.parseInt(text);
						v.setText(Html.fromHtml(String.format("%c<b>%03d</b>", val >= 0 ? '+' : '-', val)));
						break;
					case R.id.fuel:
						v.setText(String.format("%\u002003d", Integer.parseInt(text)));
						break;
					default: super.setViewText(v, text);
					}
				}
			}
		);
		registerForContextMenu(getListView());
		
		// Set up status bar (if present).
		final StatusBarControls c = new StatusBarControls(this);
		c.AddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MileageList.this, Mileage.class);
				startActivityForResult(intent, NEW_REQUEST);
			}
		});
		c.TransferButton.setVisibility(View.GONE);
		c.TemplateButton.setVisibility(View.GONE);
		c.FilterButton.setVisibility(View.GONE);
		c.TotalLayout.setVisibility(View.GONE);
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(this, Mileage.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mileages_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.opt_menu_add:
				Intent intent = new Intent(this, Mileage.class);
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
							new MileageBroker(MileageList.this).delete(mi.id);
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