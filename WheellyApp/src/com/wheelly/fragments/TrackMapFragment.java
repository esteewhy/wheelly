package com.wheelly.fragments;

import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

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
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.TrackChangedEvent;
import com.wheelly.content.OpenGpsTrackRepository;

/**
 * Renders rough map from Open GPS Tracker waypoints cursor
 */
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
	
	private static PolylineOptions PATH3(Cursor waypoints) {
		final PolylineOptions path = new PolylineOptions();
		if(waypoints.moveToFirst()) {
			do {
				final Location l = OpenGpsTrackRepository.LOCATION(waypoints);
				LatLng ll = new LatLng(l.getLatitude(), l.getLongitude());
				if(ll.latitude != 0 || ll.longitude != 0) {
					path.add(ll);
				}
			} while(waypoints.moveToNext());
		}
		return path;
	}
	
	private class PathTask extends AsyncTask<Void, Void, PolylineOptions> {
		private final Cursor waypoints;
		
		public PathTask(Cursor waypoints) {
			this.waypoints = waypoints;
			googleMap.clear();
			
			if(!waypoints.moveToFirst()) return;
			final Location first = OpenGpsTrackRepository.LOCATION(waypoints);
			if(!waypoints.moveToLast()) return;
			final Location last = OpenGpsTrackRepository.LOCATION(waypoints);
			
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
						googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, getResources().getDimensionPixelSize(R.dimen.padding)));
						googleMap.setOnCameraChangeListener(null);
					}
				});
			}
		}
		
		@Override
		protected PolylineOptions doInBackground(Void... paramArrayOfParams) {
			return PATH3(waypoints);
		}
		
		@Override
		protected void onPostExecute(PolylineOptions result) {
			super.onPostExecute(result);
			waypoints.close();
			googleMap.addPolyline(result);
		}
	}
	
	private PathTask pt;
	
	private void drawMap(TrackChangedEvent event) {
		Cursor waypoints = new OpenGpsTrackRepository(getActivity()).waypoints(event.id);
		
		
		if(waypoints.moveToFirst()) {
			getView().setVisibility(View.VISIBLE);
			if(pt != null && !pt.isCancelled()) {
				pt.cancel(true);
			}
			
			pt = new PathTask(waypoints);
			pt.execute();
		} else {
			getView().setVisibility(View.GONE);
		}
	}
}