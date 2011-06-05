package com.wheelly.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import android.widget.Toast;

import com.wheelly.R;
import com.wheelly.app.StatusBarControls;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.MileageRepository;
import com.wheelly.db.MileageBroker;
import com.wheelly.service.Tracker;

public class MileageList extends ListActivity {

	private static final int NEW_REQUEST = 1;
	private static final int EDIT_REQUEST = 2;
	private static final int FILTER_REQUEST = 6;
	
	final private ContentValues filter = new ContentValues();
	private StatusBarControls c;
	private boolean suggestInstall = false;
	private SQLiteDatabase db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mileage_list);
		
		//Advise user installing MyTracks app.
		suggestInstall =
			null != savedInstanceState && savedInstanceState.containsKey("suggestInstall")
				? savedInstanceState.getBoolean("suggestInstall")
				: !new Tracker(this).checkAvailability();
		
		if(!suggestInstall) {
			Toast.makeText(this, R.string.advertise_mytracks, Toast.LENGTH_LONG).show();
		}
		
		final Cursor cursor = new MileageRepository(db = new DatabaseHelper(this).getReadableDatabase()).list();
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
						v.setText(String.format("%\u002003.2f", Float.parseFloat(text)));
						break;
					default: super.setViewText(v, text);
					}
				}
			}
		);
		registerForContextMenu(getListView());
		
		// Set up status bar (if present).
		c = new StatusBarControls(this);
		c.AddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				c.AddButton.setEnabled(false);
				Intent intent = new Intent(MileageList.this, Mileage.class);
				startActivityForResult(intent, NEW_REQUEST);
			}
		});
		c.TransferButton.setVisibility(View.GONE);
		c.TemplateButton.setVisibility(View.GONE);
		c.FilterButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				c.FilterButton.setEnabled(false);
				Intent intent = new Intent(MileageList.this, Filter.class);
				filter.put(Filter.F.LOCATION_CONSTRAINT, "mileages");
				Filter.filterToIntent(filter, intent);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		c.TotalLayout.setVisibility(View.GONE);
	}
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("suggestInstall", suggestInstall);
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(this, Mileage.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case FILTER_REQUEST:
			if (resultCode == RESULT_FIRST_USER) {
				filter.clear();
			} else if (resultCode == RESULT_OK) {
				Filter.intentToFilter(data, filter);
			}
			filter.remove(Filter.F.LOCATION_CONSTRAINT);
			c.FilterButton.setImageResource(filter.size() == 0 ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
			stopManagingCursor(((SimpleCursorAdapter)this.getListAdapter()).getCursor());
			
			MileageRepository repo = new MileageRepository(new DatabaseHelper(this).getReadableDatabase());
			Cursor cursor = filter.size() == 0 ? repo.list() : repo.list(filter);
			startManagingCursor(cursor);
			((SimpleCursorAdapter)this.getListAdapter()).changeCursor(cursor);
			
			c.FilterButton.setEnabled(true);
			break;
		case NEW_REQUEST:
			c.AddButton.setEnabled(true);
			break;
		}
		((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mileages_menu, menu);
		menu.findItem(R.id.opt_menu_install_mytracks).setVisible(suggestInstall);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.opt_menu_add:
				Intent intent = new Intent(this, Mileage.class);
				startActivityForResult(intent, NEW_REQUEST);
				return true;
			case R.id.opt_menu_install_mytracks:
				Intent marketIntent = new Intent(Intent.ACTION_VIEW)
					.setData(Uri.parse("market://details?id=com.google.android.maps.mytracks"));
				startActivity(marketIntent);
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