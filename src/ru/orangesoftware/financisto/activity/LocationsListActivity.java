/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.utils.AddressGeocoder;

import com.wheelly.R;
import com.wheelly.app.LocationUtils;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationBroker;
import com.wheelly.db.LocationRepository;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocationsListActivity extends ListActivity {
	
	private static final int MENU_EDIT = Menu.FIRST+2;
	private static final int MENU_DELETE = Menu.FIRST+3;
	private static final int MENU_RESOLVE = Menu.FIRST + 5;
	
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	private Cursor cursor;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.location_list);
		
		this.cursor = new LocationRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		startManagingCursor(cursor);
		
		setListAdapter(
			new SimpleCursorAdapter(this, R.layout.location_item, cursor,
				new String[] { "name", "resolved_address" },
				new int[] { R.id.line1, R.id.label }
			) {{
				setViewBinder(new ViewBinder() {
					@Override
					public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
						switch(view.getId()) {
						case R.id.label:
							((TextView)view).setText(LocationUtils.locationToText(cursor));
							return true;
						}
						return false;
					}
				});
			}}
		);
		
		findViewById(R.id.bAdd).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivityForResult(
					new Intent(LocationsListActivity.this, LocationActivity.class),
					NEW_LOCATION_REQUEST
				);
			}
		});
		
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.locations);
		menu.add(0, MENU_RESOLVE, 0, R.string.resolve_address);
		menu.add(1, MENU_EDIT, 1, R.string.edit);
		menu.add(1, MENU_DELETE, 2, R.string.delete);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item)) {
			return true;
		}
		
		final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
		
		switch(item.getItemId()) {
		case MENU_RESOLVE:
			cursor.moveToPosition(mi.position);
			startGeocode(LocationRepository.deserialize(cursor));
			return true;
		case MENU_EDIT:
			onListItemClick(getListView(), null, mi.position, mi.id);
			return true;
		case MENU_DELETE:
			new LocationRepository(new DatabaseHelper(this).getWritableDatabase()).delete(mi.id);
			cursor.requery();
			return true;
		};
		return false;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, final long id) {
		startActivityForResult(
			new Intent(this, LocationActivity.class) {{
				putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
			}},
			EDIT_LOCATION_REQUEST
		);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			cursor.requery();
		}
	}
	
	private void startGeocode(ContentValues location) {
		new GeocoderTask(location)
			.execute(
				location.getAsDouble("latitude"),
				location.getAsDouble("longitude")
			);
	}
	
	private class GeocoderTask extends AsyncTask<Double, Void, String> {
		private final AddressGeocoder geocoder;
		private final ContentValues location;
		
		private GeocoderTask(ContentValues location) {
			this.geocoder = new AddressGeocoder(LocationsListActivity.this);
			this.location = location;
		}
		
		@Override
		protected void onPreExecute() {
			Log.d("Geocoder", "About to enter from onPreExecute");
			// Show progress spinner and disable buttons
			setProgressBarIndeterminateVisibility(true);
			//setActionEnabled(false);
			Log.d("Geocoder", "About to exit from onPreExecute");
		}
		
		@Override
		protected String doInBackground(Double... args) {
			Log.d("Geocoder", "About to enter from doInBackground");
			// Reverse geocode using location
			return geocoder.resolveAddressFromLocation(args[0], args[1]);
		}
		
		@Override
		protected void onPostExecute(String found) {
			Log.d("Geocoder", "About to enter from onPostExecute");
			setProgressBarIndeterminateVisibility(false);
			// Update GUI with resolved string
			if (found != null) {
				Toast.makeText(LocationsListActivity.this, found, Toast.LENGTH_LONG).show();
				location.put("resolved_address", found);
				new LocationBroker(LocationsListActivity.this).updateOrInsert(location);
				cursor.requery();
				//locationText.setText(found.name);
			} else if (geocoder.lastException != null) {
				Toast.makeText(LocationsListActivity.this, R.string.service_is_not_available, Toast.LENGTH_LONG).show();
			}
			//setActionEnabled(true);
			Log.d("Geocoder", "About to exit from onPostExecute");
		}
	}
}