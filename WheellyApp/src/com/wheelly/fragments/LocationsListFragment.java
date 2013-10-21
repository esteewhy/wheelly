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
import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.gms.location.LocationListener;
import com.google.common.base.Strings;
import com.wheelly.R;
import com.wheelly.app.ColorInput;
import com.wheelly.app.ColorInput.OnSelectColorListener;
import com.wheelly.app.LocationViewBinder;
import com.wheelly.bus.LocationChoosenEvent;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.LocationBroker;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.util.LocationUtils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuCompat;
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
	
	private static final int MENU_ADD = Menu.FIRST + 1;
	private static final int MENU_EDIT = Menu.FIRST + 2;
	private static final int MENU_DELETE = Menu.FIRST + 3;
	private static final int MENU_RESOLVE = Menu.FIRST + 5;
	private static final int MENU_COLOR = Menu.FIRST + 6;
	
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	private ContextualActionModeCallback contextualActionModeCallback = new ContextualActionModeCallback() {
		@Override
		public boolean onClick(int itemId, int position, long id) {
			return handleContextItem(itemId, id);
		}

		@Override
		public void onPrepare(Menu menu, int position, long id) {
			BusProvider.getInstance().post(new LocationSelectedEvent(id, LocationsListFragment.this));
		}

		@Override
		public void onCreate(Menu menu) {
			menu.add(0, MENU_RESOLVE, 0, R.string.resolve_address).setIcon(android.R.drawable.ic_menu_mylocation);
			menu.add(1, MENU_EDIT, 1, R.string.edit).setIcon(android.R.drawable.ic_menu_edit);
			menu.add(1, MENU_DELETE, 2, R.string.delete).setIcon(android.R.drawable.ic_menu_delete).setVisible(!inSelectMode);
			menu.add(1, MENU_COLOR, 6, R.string.color).setIcon(android.R.drawable.ic_menu_slideshow);
		}

		@Override
		public CharSequence getCaption(View view) {
			TextView textView = (TextView) view.findViewById(android.R.id.text1);
			return textView != null ? textView.getText() : null;
		}
	};
	
	boolean inSelectMode;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Intent intent = getActivity().getIntent();
		inSelectMode = intent.hasExtra(LocationActivity.LOCATION_ID_EXTRA);
		getListView().setChoiceMode(inSelectMode ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		setListAdapter(buildAdapter());
		ApiAdapterFactory.getApiAdapter().configureListViewContextualMenu(this, contextualActionModeCallback);
		setHasOptionsMenu(true);
		BusProvider.getInstance().register(this);
	}
	
	private ListAdapter buildAdapter() {
		if(inSelectMode) {
			return
				new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_single_choice, null,
					new String[] { "name" },
					new int[] { android.R.id.text1 }, 0
				) {{
					setViewBinder(new ViewBinder() {
						@Override
						public boolean setViewValue(View view, Cursor cursor, int paramInt) {
							if(android.R.id.text1 == view.getId()) {
								final String argb = cursor.getString(cursor.getColumnIndex("color"));
								if(!Strings.isNullOrEmpty(argb)) {
									view.setBackgroundColor(Color.parseColor(argb));
								}
							}
							return false;
						}
					});
				}};
		}
		
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.location_item, null,
			new String[] { "name", "resolved_address", "name" },
			new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 }, 0
		);
		
		adapter.setViewBinder(new LocationViewBinder(null));
		
		LocationUtils.obtainLocation(getActivity(), new LocationListener() {
			@Override
			public void onLocationChanged(Location paramLocation) {
				((LocationViewBinder)adapter.getViewBinder()).location = paramLocation;
				adapter.notifyDataSetChanged();
			}
		});
		
		return adapter;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.locations);
		contextualActionModeCallback.onCreate(menu);
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
		contextualActionModeCallback.onPrepare(menu, mi.position, mi.id);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return
			(handleContextItem(item.getItemId(), ((AdapterContextMenuInfo) item.getMenuInfo()).id))
				? true
				: super.onContextItemSelected(item);

	}
	
	private boolean handleContextItem(int itemId, final long id) {
		switch(itemId) {
		case MENU_RESOLVE:
			startGeocode(new LocationBroker(getActivity()).loadOrCreate(id));
			return true;
		case MENU_EDIT:
			onListItemClick(getListView(), null, 0, id);
			return true;
		case MENU_DELETE:
			getActivity().getContentResolver().delete(
				Locations.CONTENT_URI,
				BaseColumns._ID + " = ?",
				new String[] { Long.toString(id) }
			);
			return true;
		case MENU_COLOR:
			new ColorInput(new OnSelectColorListener() {
				@Override
				public void onSelect(DialogInterface dialog, int which, int color,
						String argb) {
					final LocationBroker broker = new LocationBroker(getActivity());
					final ContentValues location = broker.loadOrCreate(id);
					
					if(location.size() > 0) {
						location.put("color", argb);
						broker.updateOrInsert(location);
					}
				}
			}).show(getFragmentManager(), null);
			return true;
		};
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuCompat.setShowAsAction(menu.add(0, MENU_ADD, 0, R.string.item_add)
			.setIcon(android.R.drawable.ic_menu_add), MenuItem.SHOW_AS_ACTION_ALWAYS);
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
			BusProvider.getInstance().post(new LocationChoosenEvent(id));
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