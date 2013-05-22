package com.wheelly.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.PolylineOptionsCreator;
import com.squareup.otto.Subscribe;
import com.wheelly.bus.BusProvider;
import com.wheelly.bus.TrackChangedEvent;

public class TrackMapFragment extends SupportMapFragment {
	private GoogleMap googleMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		BusProvider.getInstance().register(this);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		googleMap = getMap();
	}
	
	@Subscribe public void onTrackChanged(TrackChangedEvent event) {
		if(event.id > -1 && null != googleMap && null != getActivity()) {
			drawMap(event);
		}
	}
	
	private void drawMap(TrackChangedEvent event) {
		final MyTracksProviderUtils utils = MyTracksProviderUtils.Factory.get(getActivity()); 
		final Cursor points = utils.getTrackPointCursor(event.id, -1, -1, false);
		
		try {
			if(points.moveToFirst()) {
				final LatLngBounds.Builder b = LatLngBounds.builder();
				
				final int latIdx = points.getColumnIndex(TrackPointsColumns.LATITUDE);
				final int lonIdx = points.getColumnIndex(TrackPointsColumns.LONGITUDE);
				
				final PolylineOptions path = new PolylineOptions();
				do {
					final LatLng l = new LatLng((double)points.getInt(latIdx) / 1e6, (double)points.getInt(lonIdx) / 1e6);
					//final MarkerOptions markerOptions = new MarkerOptions().position(l);
					//googleMap.addMarker(markerOptions);
					path.add(l);
					b.include(l);
				} while(points.moveToNext());
				
				googleMap.addPolyline(path);
				
				googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
					@Override
					public void onCameraChange(CameraPosition arg0) {
						googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 15));
						googleMap.setOnCameraChangeListener(null);
					}
				});
			}
		} finally {
			points.close();
		}
	}
}