package com.wheelly.fragments;

import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.otto.Subscribe;
import com.wheelly.bus.BusProvider;
import com.wheelly.bus.TrackChangedEvent;

public class TrackMapFragment extends SupportMapFragment {
	private GoogleMap googleMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BusProvider.getInstance().register(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BusProvider.getInstance().unregister(this);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		googleMap = getMap();
	}
	
	@Subscribe public void onTrackChanged(TrackChangedEvent event) {
		if(event.id > -1 && null != googleMap) {
			drawMap(event);
		}
	}
	
	private static LatLng POINT(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
	
	private static PolylineOptions PATH1(MyTracksProviderUtils utils, long id) {
		LocationIterator locations = utils.getTrackPointLocationIterator(id, -1, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
		Track t = new Track();
		
		while(locations.hasNext()) {
			t.addLocation(locations.next());
		}
		
		LocationUtils.decimate(t, 500);
		final PolylineOptions path = new PolylineOptions();
		
		for(Location l : t.getLocations()) {
			path.add(POINT(l));
		}
		
		return path;
	}
	
	private static PolylineOptions PATH2(MyTracksProviderUtils utils, long id) {
		final Cursor locations = utils.getTrackPointCursor(id, -1, -1, false);
		final PolylineOptions path = new PolylineOptions();
		try {
			if(locations.moveToFirst()) {
				
				
				final int latIdx = locations.getColumnIndex("latitude");
				final int lonIdx = locations.getColumnIndex("longitude");
				
				do {
					LatLng l = new LatLng(locations.getDouble(latIdx) / 1e6, locations.getDouble(lonIdx) / 1e6);
					if(l.latitude != 0 || l.longitude != 0) {
						path.add(l);
					}
				} while(locations.moveToNext());
			}
		} finally {
			locations.close();
		}
		
		return path;
	}
	
	private class PathTask extends AsyncTask<Void, Void, PolylineOptions> {
		private final MyTracksProviderUtils utils;
		private final long id;
		
		public PathTask(MyTracksProviderUtils utils, long id) {
			this.utils = utils;
			this.id = id;
			googleMap.clear();
			
			final Location first = utils.getFirstValidTrackPoint(id);
			final Location last = utils.getLastValidTrackPoint(id);
			
			if(first != null && last != null) {
				final LatLng start = POINT(first);
				final LatLng stop = POINT(last); 
				googleMap.addMarker(new MarkerOptions().position(start).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
				googleMap.addMarker(new MarkerOptions().position(stop).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
				
				final LatLngBounds bounds = new LatLngBounds.Builder()
					.include(start).include(stop).build();
				
				//googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
				googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
					@Override
					public void onCameraChange(CameraPosition arg0) {
						googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
						googleMap.setOnCameraChangeListener(null);
					}
				});
			}
		}
		
		@Override
		protected PolylineOptions doInBackground(Void... paramArrayOfParams) {
			return PATH1(utils, id);
		}
		
		@Override
		protected void onPostExecute(PolylineOptions result) {
			super.onPostExecute(result);
			
			googleMap.addPolyline(result);
			
			
		}
	}
	
	private PathTask pt;
	
	private void drawMap(TrackChangedEvent event) {
		final MyTracksProviderUtils utils = MyTracksProviderUtils.Factory.get(getActivity()); 
		Track track = utils.getTrack(event.id);
		
		if(null != track) {
			getView().setVisibility(View.VISIBLE);
			if(pt != null && !pt.isCancelled()) {
				pt.cancel(true);
			}
			
			pt = new PathTask(utils, event.id);
			pt.execute();
		} else {
			getView().setVisibility(View.GONE);
		}
	}
}