/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.MapContextActionCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.GoogleMap.OnMapLongClickListener;
import com.androidmapsextensions.GoogleMap.OnMarkerDragListener;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.SupportMapFragment;

/**
 * API level 11 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(11)
public class Api11Adapter extends Api8Adapter {
	@Override
	public void configureMapViewContextualMenu(final SupportMapFragment fragment, final MapContextActionCallback callback) {
	
		final GoogleMap googleMap = fragment.getExtendedMap();
	
		googleMap.setOnMarkerDragListener(new OnMarkerDragListener() {
			private Marker currentMarker;
			private ActionMode actionMode;
			
			@Override
			public void onMarkerDragStart(Marker marker) {
				if(!marker.isCluster()) {
					if(null == currentMarker || !marker.equals(currentMarker)) {
						if(null != actionMode) {
							actionMode.finish();
						}
						
						if(null != currentMarker) {
							callback.onResetMarker(currentMarker);
						}
						
						currentMarker = marker;
					}
				}
			}
			
			@Override
			public void onMarkerDragEnd(final Marker marker) {
				if(marker.isCluster()) {
					return;
				}
				
				if (actionMode != null) {
					if(null == currentMarker || !marker.equals(currentMarker)) {
						actionMode.finish();
					} else {
						((Vibrator)fragment.getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
						return;
					}
				}
				
				actionMode = ((ActionBarActivity)fragment.getActivity()).getSupportActionBar().startActionMode(new ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						callback.onCreate(menu);
						return true;
					}
					
					@Override
					public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
						callback.onPrepare(menu, marker);
						// Return true to indicate change
						return true;
					}
					
					LatLng newPosition;
					
					@Override
					public void onDestroyActionMode(ActionMode mode) {
						actionMode = null;
						currentMarker = null;
						newPosition = marker.getPosition();
						callback.onResetMarker(marker);
					}
					
					@Override
					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
						mode.finish();
						
						if(null != newPosition) {
							marker.setPosition(newPosition);
							newPosition = null;
						}
						
						return callback.onClick(item, marker);
					}
				});
			}
			
			@Override
			public void onMarkerDrag(Marker paramMarker) { }
		});
	
		googleMap.setOnMapLongClickListener(new OnMapLongClickListener() {
			private ActionMode actionMode;
			private Marker marker;
			
			@Override
			public void onMapLongClick(final LatLng point) {
				if (actionMode != null) {
					actionMode.finish();
				}
				
				marker = fragment.getExtendedMap().addMarker(new MarkerOptions()
					.position(point)
					.title("New location")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
				
				actionMode = ((ActionBarActivity)fragment.getActivity()).getSupportActionBar().startActionMode(new ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						callback.onCreate(menu);
						return true;
					}
					
					@Override
					public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
						callback.onPrepare(menu, marker);
						// Return true to indicate change
						return true;
					}
					
					@Override
					public void onDestroyActionMode(ActionMode mode) {
						actionMode = null;
						
						if(null != marker) {
							marker.remove();
						}
					}
					
					@Override
					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
						boolean result = callback.onClick(item, marker);
						mode.finish();
						return result;
					}
				});
			}
		});
	};
}