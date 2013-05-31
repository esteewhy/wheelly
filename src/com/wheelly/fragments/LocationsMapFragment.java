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

import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.MapContextActionCallback;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.LocationBroker;
import com.wheelly.util.LocationUtils;
import com.wheelly.util.LocationUtils.AddressResolveCallback;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LocationsMapFragment extends SupportMapFragment {
	static final int NEW_LOCATION_REQUEST = 1;
	static final int EDIT_LOCATION_REQUEST = 2;
	
	private GoogleMap googleMap;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View layout = super.onCreateView(inflater, container, savedInstanceState);

		/*
		 * At this point, after super.onCreateView, getMap will not return null
		 * and we can initialize googleMap. However, onCreateView can be called
		 * multiple times, e.g., when the user switches tabs. With
		 * GoogleMapOptions.useViewLifecycleInFragment == false, googleMap
		 * lifecycle is tied to the fragment lifecycle and the same googleMap
		 * object is returned in getMap. Thus we only need to initialize
		 * googleMap once, when it is null.
		 */
		if (googleMap == null) {
			googleMap = getMap();
			googleMap.setMyLocationEnabled(true);

			googleMap.getUiSettings().setMyLocationButtonEnabled(true);
			googleMap.getUiSettings().setAllGesturesEnabled(true);
			googleMap.setIndoorEnabled(true);
			
			googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {
				@Override
				public boolean onMarkerClick(Marker marker) {
					final long id = Long.parseLong(marker.getSnippet());
					BusProvider.getInstance().post(new LocationSelectedEvent(id, LocationsMapFragment.this));
					return false;
				}
			});
			
			googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				
				@Override
				public void onInfoWindowClick(Marker marker) {
					final long id = Long.parseLong(marker.getSnippet());
					if (isResumed()) {
						startActivityForResult(
								new Intent(getActivity(), LocationActivity.class) {{
									putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
								}},
								EDIT_LOCATION_REQUEST
							);
					}
				}
			});
		}

		return layout;
	}
	
	private boolean inSelectMode;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuCompat.setShowAsAction(
			menu.add(Menu.NONE, 1, Menu.NONE, R.string.item_add)
				.setIcon(android.R.drawable.ic_menu_add),
			MenuItem.SHOW_AS_ACTION_IF_ROOM
		);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		BusProvider.getInstance().register(this);
		
		inSelectMode = getActivity().getIntent().hasExtra(LocationActivity.LOCATION_ID_EXTRA);
		
        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
 
        // Showing status
        if(status != ConnectionResult.SUCCESS){ // Google Play Services are not available
 
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, getActivity(), requestCode);
            dialog.show();
 
        } else { // Google Play Services are available
 
            // Getting GoogleMap object from the fragment
            googleMap = getMap();
        }
        
        ApiAdapterFactory.getApiAdapter().configureMapViewContextualMenu(this, new MapContextActionCallback() {
			private Marker marker;
			private EditText editText; 
			@Override
			public void onPrepare(Menu arg0, int arg1, long arg2) {
				// TODO Auto-generated method stub
			}
			
			@SuppressLint("NewApi")
			@Override
			public void onCreate(Menu menu) {
				editText = (EditText)
				menu.add(0, 0, 0, "name")
					.setIcon(android.R.drawable.ic_menu_edit)
					.setActionView(R.layout.location_edit)
					.getActionView()
					.findViewById(R.id.location_name);
        	configureTitleWidget();
				menu.add(0, 1, 0, R.string.item_add)
					.setIcon(android.R.drawable.ic_menu_save);
			}
			
			private void configureTitleWidget() {
				if(null != marker) {
					LocationUtils.resolveAddress(getActivity(), marker.getPosition(), "", new AddressResolveCallback() {
						@Override
						public void onResolved(Address address) {
							if(getResources().getString(R.string.new_location).equals(editText.getText().toString()) && null != address) {
								editText.setText(LocationUtils.formatAddress(address));
							}
						}
					});
				}
			}
			
			@Override
			public CharSequence getCaption(View arg0) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean onClick(int arg0, int arg1, long arg2) {
				if(null != marker) {
					final ContentValues myLocation = new ContentValues();
					if(null != editText) {
						myLocation.put("name", editText.getText().toString());
					}
					myLocation.put("provider", "fusion");
					myLocation.put("latitude", marker.getPosition().latitude);
					myLocation.put("longitude", marker.getPosition().longitude);
					myLocation.put("datetime", System.currentTimeMillis());
					
					LocationUtils.resolveAddress(getActivity(), marker.getPosition(), myLocation.getAsString("name"),
						new AddressResolveCallback() {
							@Override
							public void onResolved(Address address) {
								if(null != address) {
									myLocation.put("resolved_address", LocationUtils.formatAddress(address));
								}
								
								new LocationBroker(getActivity()).updateOrInsert(myLocation);
								marker.remove();
							}
						});
					
					return true;
				}
				return false;
			}
			
			@Override
			public boolean onMapLongClick(LatLng point) {
				marker = googleMap.addMarker(new MarkerOptions()
					.position(point)
					.title("New location")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
				);
				return true;
			}

			@Override
			public void onCancel() {
				if(null != marker) {
					marker.remove();
				}
			}
		});
	}
	
	private interface Aggregate {
		void seed(LatLng l, long id);
		CameraUpdate result();
	}
	
	private Aggregate buildCameraUpdateDelegate() {
		return
			!inSelectMode
				? new Aggregate() {
					private final LatLngBounds.Builder b = LatLngBounds.builder();
					@Override
					public void seed(LatLng l, long id) {
						b.include(l);
					}
					
					@Override
					public CameraUpdate result() {
						return CameraUpdateFactory.newLatLngBounds(b.build(), 15);
					}
				}
				: new Aggregate() {
					private final long selectedId = getActivity().getIntent().getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					private LatLng selected = null;
					@Override
					public void seed(LatLng l, long id) {
						if(id == selectedId) {
							selected = l;
						}
					}
					
					@Override
					public CameraUpdate result() {
						return CameraUpdateFactory.newLatLngZoom(selected, 17);
					}
				};
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BusProvider.getInstance().unregister(this);
	}
	
	@Subscribe
	public void onLoadFinished(LocationsLoadedEvent event) {
		googleMap.clear();
		
		if(null != event.cursor && event.cursor.moveToFirst()) {
			final int latIdx = event.cursor.getColumnIndex("latitude");
			final int lonIdx = event.cursor.getColumnIndex("longitude");
			final int nameIdx = event.cursor.getColumnIndex("name");
			final int idIdx = event.cursor.getColumnIndex(BaseColumns._ID);
			
			final Aggregate delegate = buildCameraUpdateDelegate();
				
			do {
				final LatLng l = new LatLng(event.cursor.getDouble(latIdx), event.cursor.getDouble(lonIdx));
				final long id = event.cursor.getLong(idIdx);
				final MarkerOptions markerOptions = new MarkerOptions()
		        	.position(l)
		        	.title(event.cursor.getString(nameIdx))
		        	.snippet(Long.toString(id))
		        	.draggable(true);
		        
		        // Adding marker on the Google Map
		        googleMap.addMarker(markerOptions);
		        delegate.seed(l, id);
			} while(event.cursor.moveToNext());
			
			//googleMap.animateCamera(delegate.result());
			googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition paramCameraPosition) {
					googleMap.animateCamera(delegate.result());
					googleMap.setOnCameraChangeListener(null);
				}
			});
		}
	}
	
	@Subscribe
	public void onSelectionChanged(final LocationSelectedEvent event) {
		if(this != event.sender) {
			final ContentValues c = new LocationBroker(getActivity()).loadOrCreate(event.id);
			final LatLng selected = new LatLng(c.getAsDouble("latitude"), c.getAsDouble("longitude"));
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selected, 17));
		}
	}
}