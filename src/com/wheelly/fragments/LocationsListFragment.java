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

import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.LocationBroker;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.util.LocationUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "NewApi", "InlinedApi" })
public class LocationsListFragment extends ListFragment {
	
	private static final int MENU_ADD = Menu.FIRST+1;
	private static final int MENU_EDIT = Menu.FIRST+2;
	private static final int MENU_DELETE = Menu.FIRST+3;
	private static final int MENU_RESOLVE = Menu.FIRST + 5;
	
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	boolean inSelectMode;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Intent intent = getActivity().getIntent();
		inSelectMode = intent.hasExtra(LocationActivity.LOCATION_ID_EXTRA);
		getListView().setChoiceMode(inSelectMode ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		setListAdapter(inSelectMode ? buildSelectableAdapter() : buildAdapter());
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		BusProvider.getInstance().register(this);
	}
	
	private ListAdapter buildSelectableAdapter() {
		return
			new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_single_choice, null,
				new String[] { "name" },
				new int[] { android.R.id.text1 }, 0
			);
	}
	
	private ListAdapter buildAdapter() {
		final Location location = LocationUtils.getLastKnownLocation(getActivity());
		final Location dest = new Location(location);
		
		return
			new SimpleCursorAdapter(getActivity(), R.layout.location_item, null,
					new String[] { "name", "resolved_address", "name" },
					new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 }, 0
				) {{
					setViewBinder(new ViewBinder() {
						@Override
						public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
							switch(view.getId()) {
							case android.R.id.text2:
								((TextView)view).setText(LocationUtils.locationToText(cursor));
								return true;
							case R.id.text3:
								if(null != location) {
									TextView tv = (TextView)view.findViewById(R.id.text3);
									dest.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
									dest.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
									tv.setText(LocationUtils.formatDistance(location.distanceTo(dest)));
								}
								
								return true;
							}
							return false;
						}
					});
				}};
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		BusProvider.getInstance().post(new LocationSelectedEvent(((AdapterContextMenuInfo)menuInfo).id, LocationsListFragment.this));
		menu.setHeaderTitle(R.string.locations);
		menu.add(0, MENU_RESOLVE, 0, R.string.resolve_address).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(1, MENU_EDIT, 1, R.string.edit).setIcon(android.R.drawable.ic_menu_edit);
		menu.add(1, MENU_DELETE, 2, R.string.delete).setIcon(android.R.drawable.ic_menu_delete);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item)) {
			return true;
		}
		
		final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
		
		switch(item.getItemId()) {
		case MENU_RESOLVE:
			startGeocode(new LocationBroker(getActivity()).loadOrCreate(mi.id));
			return true;
		case MENU_EDIT:
			onListItemClick(getListView(), null, mi.position, mi.id);
			return true;
		case MENU_DELETE:
			getActivity().getContentResolver().delete(
				Locations.CONTENT_URI,
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(mi.id) }
			);
			return true;
		};
		return false;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(0, MENU_ADD, 0, R.string.item_add)
			.setIcon(android.R.drawable.ic_menu_add)
			.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(MENU_ADD == item.getItemId()) {
			startActivityForResult(
				new Intent(getActivity(), LocationActivity.class),
				NEW_LOCATION_REQUEST
			);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, final long id) {
		BusProvider.getInstance().post(new LocationSelectedEvent(id, LocationsListFragment.this));
		
		if(inSelectMode) {
			l.setItemChecked(position, true);
			final Intent intent = new Intent();
			intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
			getActivity().setResult(Activity.RESULT_OK, intent);
			getActivity().finish();
		} else {
			startActivityForResult(
				new Intent(getActivity(), LocationActivity.class) {{
					putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
				}},
				EDIT_LOCATION_REQUEST
			);
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
//				getLoaderManager().restartLoader(0, null, LocationsListFragment.this);
				//locationText.setText(found.name);
			} else if (geocoder.lastException != null) {
				Toast.makeText(getActivity(), R.string.service_is_not_available, Toast.LENGTH_LONG).show();
			}
			//setActionEnabled(true);
			Log.d("Geocoder", "About to exit from onPostExecute");
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BusProvider.getInstance().unregister(this);
	}
	
	@Subscribe public void onLoadFinished(LocationsLoadedEvent event) {
		final CursorAdapter a = (CursorAdapter) getListAdapter();
		a.swapCursor(event.cursor);
	}
	
	private int getItemPositionByAdapterId(final long id)
	{
		final ListAdapter adapter = getListAdapter();
		int i = 0;
		while(i < adapter.getCount() && id != adapter.getItemId(i++));
		
		return i == adapter.getCount() && id != adapter.getItemId(i - 1) ? -1 : --i;
	}
	
	@Subscribe
	public void onSelectionChanged(final LocationSelectedEvent event) {
		if(this != event.sender) {
			int position = getItemPositionByAdapterId(event.id);
			getListView().setItemChecked(position, true);
			getListView().setSelection(position);
		}
	}
}