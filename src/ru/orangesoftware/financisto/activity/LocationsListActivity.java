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

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationRepository;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class LocationsListActivity extends ListActivity {
	
	static final int MENU_RESOLVE = Menu.FIRST + 5;
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.location_list);
		
		final Cursor cursor = new LocationRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		startManagingCursor(cursor);
		
		final SimpleCursorAdapter adapter =
			new SimpleCursorAdapter(this, R.layout.location_item, cursor,
				new String[] { "name", "resolvedAddress" },
				new int[] { R.id.line1, R.id.label }
			);
		
		final String[] columnNames = cursor.getColumnNames();
		
		adapter.setViewBinder(new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if("resolvedAddress".equals(columnNames[columnIndex])) {
					((TextView)view.findViewById(R.id.label)).setText(
						LocationRepository.locationToText(cursor)
					);
					return true;
				}
				return false;
			}
		});
		
		setListAdapter(adapter);
		//registerForContextMenu(getListView());
	}
	/*
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
		String headerTitle = getContextMenuHeaderTitle(mi.position);
		if (headerTitle != null) {
			menu.setHeaderTitle(headerTitle);
		}
		List<MenuItemInfo> menus = createContextMenus(mi.id);
		int i = 0;
		for (MenuItemInfo m : menus) {
			if (m.enabled) {
				menu.add(0, m.menuId, i++, m.titleId);				
			}
		}
	}
	
	//@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		menus.add(0, new MenuItemInfo(MENU_RESOLVE, R.string.resolve_address));
		return menus;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item)) {
			return true;
		}
		if (item.getItemId() == MENU_RESOLVE) {
			AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			resolveAddress(mi.position, mi.id);
		}
		return false;
	}

	private void resolveAddress(int position, long id) {
		cursor.moveToPosition(position);
		MyLocation location = EntityManager.loadFromCursor(cursor, MyLocation.class);
		startGeocode(location);
	}*/
/*
	@Override
	protected void addItem() {
		Intent intent = new Intent(this, LocationActivity.class);
		startActivityForResult(intent, NEW_LOCATION_REQUEST);
	}

	@Override
	protected void deleteItem(int position, long id) {
		em.deleteLocation(id);
		cursor.requery();
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(this, LocationActivity.class);
		intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_LOCATION_REQUEST);
	}

	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.location);
	}

	@Override
	protected void viewItem(int position, long id) {
		editItem(position, id);
//		try { 
//			cursor.moveToPosition(position);
//			MyLocation location = EntityManager.loadFromCursor(cursor, MyLocation.class);
//			Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:"
//					+Location.convert(location.latitude, Location.FORMAT_DEGREES)+","
//					+Location.convert(location.longitude, Location.FORMAT_DEGREES))); 
//		    startActivity(myIntent); 
//		} catch (Exception e) { }
	}	

    private void startGeocode(MyLocation location) {
        new GeocoderTask(location).execute(location.latitude, location.longitude);
    }

    private class GeocoderTask extends AsyncTask<Double, Void, String> {
        
    	private final AddressGeocoder geocoder;
        private final MyLocation location;

        private GeocoderTask(MyLocation location) {
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
				Toast t = Toast.makeText(LocationsListActivity.this, found, Toast.LENGTH_LONG);
				t.show();
				location.resolvedAddress = found;
				em.saveLocation(location);
				requeryCursor();
        		//locationText.setText(found.name);            		
            } else if (geocoder.lastException != null) {
				Toast t = Toast.makeText(LocationsListActivity.this, R.string.service_is_not_available, Toast.LENGTH_LONG);
				t.show();            	
            }
            //setActionEnabled(true);
            Log.d("Geocoder", "About to exit from onPostExecute");
        }
    }
*/
}
