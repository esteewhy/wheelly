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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.wheelly.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class LocationsMapFragment extends SupportMapFragment {
	private GoogleMap googleMap;
	private View mapView;
	
	@Override
	  public View onCreateView(
	      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    mapView = super.onCreateView(inflater, container, savedInstanceState);
	    View layout = inflater.inflate(R.layout.map, container, false);
	    RelativeLayout mapContainer = (RelativeLayout) layout.findViewById(R.id.map_container);
	    mapContainer.addView(mapView, 0);
	    
	    /*
	     * At this point, after super.onCreateView, getMap will not return null and
	     * we can initialize googleMap. However, onCreateView can be called multiple
	     * times, e.g., when the user switches tabs. With
	     * GoogleMapOptions.useViewLifecycleInFragment == false, googleMap lifecycle
	     * is tied to the fragment lifecycle and the same googleMap object is
	     * returned in getMap. Thus we only need to initialize googleMap once, when
	     * it is null.
	     */
	    if (googleMap == null) {
	      googleMap = getMap();
	      googleMap.setMyLocationEnabled(true);

	      /*
	       * My Tracks needs to handle the onClick event when the my location button
	       * is clicked. Currently, the API doesn't allow handling onClick event,
	       * thus hiding the default my location button and providing our own.
	       */
	      googleMap.getUiSettings().setMyLocationButtonEnabled(false);
	      googleMap.setIndoorEnabled(true);
	      googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {

	          @Override
	        public boolean onMarkerClick(Marker marker) {
	          if (isResumed()) {
	          }
	          return true;
	        }
	      });
	    }
	    
	    return layout;
	  }
	  
/*	
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
		
	}
	*/
}