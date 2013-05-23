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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.LocationBroker;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
		if(null == event.cursor) {
			googleMap.clear();
			return;
		}
		
		if(event.cursor.moveToFirst()) {
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
			
			googleMap.animateCamera(delegate.result());
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