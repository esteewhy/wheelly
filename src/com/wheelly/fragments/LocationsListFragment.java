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
package com.wheelly.fragments;

import ru.orangesoftware.financisto.activity.LocationActivity;
import ru.orangesoftware.financisto.utils.AddressGeocoder;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationBroker;
import com.wheelly.db.LocationRepository;
import com.wheelly.util.LocationUtils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocationsListFragment extends ListFragment {
	
	private static final int MENU_EDIT = Menu.FIRST+2;
	private static final int MENU_DELETE = Menu.FIRST+3;
	private static final int MENU_RESOLVE = Menu.FIRST + 5;
	
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	private Cursor cursor;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		final View v = inflater.inflate(R.layout.location_list, null);
		
		this.cursor = new LocationRepository(new DatabaseHelper(getActivity()).getReadableDatabase()).list();
		getActivity().startManagingCursor(cursor);
		
		setListAdapter(
			new SimpleCursorAdapter(getActivity(), R.layout.location_item, cursor,
				new String[] { "name", "resolved_address" },
				new int[] { R.id.line1, R.id.label }, 0
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
		
		v.findViewById(R.id.bAdd).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivityForResult(
					new Intent(getActivity(), LocationActivity.class),
					NEW_LOCATION_REQUEST
				);
			}
		});
		
		registerForContextMenu(v);
		
		return v;
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
			new LocationRepository(new DatabaseHelper(getActivity()).getWritableDatabase()).delete(mi.id);
			cursor.requery();
			return true;
		};
		return false;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, final long id) {
		startActivityForResult(
			new Intent(getActivity(), LocationActivity.class) {{
				putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
			}},
			EDIT_LOCATION_REQUEST
		);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
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
			this.geocoder = new AddressGeocoder(getActivity());
			this.location = location;
		}
		
		@Override
		protected void onPreExecute() {
			Log.d("Geocoder", "About to enter from onPreExecute");
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
			// Update GUI with resolved string
			if (found != null) {
				Toast.makeText(getActivity(), found, Toast.LENGTH_LONG).show();
				location.put("resolved_address", found);
				new LocationBroker(getActivity()).updateOrInsert(location);
				cursor.requery();
				//locationText.setText(found.name);
			} else if (geocoder.lastException != null) {
				Toast.makeText(getActivity(), R.string.service_is_not_available, Toast.LENGTH_LONG).show();
			}
			//setActionEnabled(true);
			Log.d("Geocoder", "About to exit from onPostExecute");
		}
	}
}