package com.wheelly.fragments;

import java.util.List;

import pl.mg6.android.maps.extensions.demo.DemoClusterOptionsProvider;
import ru.orangesoftware.financisto.activity.LocationActivity;

import com.androidmapsextensions.*;
import com.androidmapsextensions.GoogleMap.*;
import com.google.android.apps.mytracks.MapContextActionCallback;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.*;
import com.wheelly.db.LocationBroker;
import com.wheelly.util.LocationUtils;
import com.wheelly.util.LocationUtils.AddressResolveCallback;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.*;
import android.widget.EditText;

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
			googleMap = getExtendedMap();
			googleMap.setClustering(new ClusteringSettings()
				.clusterOptionsProvider(new DemoClusterOptionsProvider(getResources()))
				.addMarkersDynamically(true)
			);
			googleMap.setMyLocationEnabled(true);

			googleMap.getUiSettings().setMyLocationButtonEnabled(true);
			googleMap.getUiSettings().setAllGesturesEnabled(true);
			googleMap.setIndoorEnabled(true);
			
			googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {
				@Override
				public boolean onMarkerClick(Marker marker) {
					if(!marker.isCluster()) {
						final long id = (Long)marker.getData();
						BusProvider.getInstance().post(new LocationSelectedEvent(id, LocationsMapFragment.this));
						return false;
					} else {
						List<Marker> markers = marker.getMarkers();
						Builder builder = LatLngBounds.builder();
						for (Marker m : markers) {
							builder.include(m.getPosition());
						}
						LatLngBounds bounds = builder.build();
						googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, getResources().getDimensionPixelSize(R.dimen.padding)));
						return true;
					}
				}
			});
			
			googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				
				@Override
				public void onInfoWindowClick(Marker marker) {
					final long id = (Long)marker.getData();
					if (isResumed()) {
						
						if(inSelectMode) {
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
			MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
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
            googleMap = getExtendedMap();
        }
        
        ApiAdapterFactory.getApiAdapter().configureMapViewContextualMenu(this, new MapContextActionCallback() {
			private EditText editText; 
			
			@Override
			public void onResetMarker(Marker marker) {
				final long id = (Long)marker.getData();
				final ContentValues location = new LocationBroker(getActivity()).loadOrCreate(id);
				final LatLng l = new LatLng(location.getAsDouble("latitude"), location.getAsDouble("longitude"));
				marker.setPosition(l);
			}
			
			@Override
			public void onPrepare(Menu menu, Marker marker) {
				configureTitleWidget(marker);
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
				
				menu.add(0, 1, 0, R.string.save)
					.setIcon(android.R.drawable.ic_menu_save)
					.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			
			private void configureTitleWidget(Marker marker) {
				if(null != marker && null != editText) {
					if(null != marker.getData()) {
						final long id = (Long)marker.getData();
						editText.setText(new LocationBroker(getActivity()).loadOrCreate(id).getAsString("name"));
					} else {
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
			}
			
			@Override
			public boolean onClick(MenuItem item, Marker marker) {
				if(null != marker && !marker.isCluster()) {
					final ContentValues myLocation = new ContentValues();
					
					Long id = (Long)marker.getData();
					
					if(null != id) {
						myLocation.put(BaseColumns._ID, id);
					} else {
						myLocation.put("provider", "fusion");
					}
					
					if(null != editText) {
						myLocation.put("name", editText.getText().toString());
					}
					
					myLocation.put("latitude", marker.getPosition().latitude);
					myLocation.put("longitude", marker.getPosition().longitude);
					myLocation.put("datetime", System.currentTimeMillis());
					id = new LocationBroker(getActivity()).updateOrInsert(myLocation);
					myLocation.put(BaseColumns._ID, id);
					
					BusProvider.getInstance().post(new LocationSelectedEvent(id, LocationsMapFragment.this));
					
					LocationUtils.resolveAddress(getActivity(), marker.getPosition(), myLocation.getAsString("name"),
						new AddressResolveCallback() {
							@Override
							public void onResolved(Address address) {
								if(null != address) {
									myLocation.put("resolved_address", LocationUtils.formatAddress(address));
								}
								
								new LocationBroker(getActivity()).updateOrInsert(myLocation);
							}
						});
					
					return true;
				}
				return false;
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
						return null != selected ? CameraUpdateFactory.newLatLngZoom(selected, 17) : null;
					}
				};
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		BusProvider.getInstance().unregister(this);
	}
	
	private Aggregate plotMarkers(Cursor c) {
		final FragmentActivity ctx = getActivity();
		
		if(c.moveToFirst()) {
			final int latIdx = c.getColumnIndex("latitude");
			final int lonIdx = c.getColumnIndex("longitude");
			final int nameIdx = c.getColumnIndex("name");
			final int addressIdx = c.getColumnIndex("resolved_address");
			final int idIdx = c.getColumnIndex(BaseColumns._ID);
			final int colorIdx = c.getColumnIndex("color");
			final Aggregate delegate = buildCameraUpdateDelegate();
			
			do {
				final LatLng l = new LatLng(c.getDouble(latIdx), c.getDouble(lonIdx));
				final long id = c.getLong(idIdx);
				final MarkerOptions markerOptions = new MarkerOptions()
					.position(l)
					.title(c.getString(nameIdx))
					.snippet(c.getString(addressIdx))
					.draggable(true);
				
				final String argb = c.getString(colorIdx);
				
				if(!TextUtils.isEmpty(argb)) {
					float[] hsv = new float[3];
					Color.colorToHSV(Color.parseColor(argb), hsv);
					markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hsv[0]));
				}
				
				ctx.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						googleMap.addMarker(markerOptions).setData(id);
					}
				});
				
				delegate.seed(l, id);
			} while(c.moveToNext());
			
			return delegate;
		}
		return null;
	}
	
	private void focusMarkers(final Aggregate delegate, boolean delay) {
		if(null != delegate) {
			final CameraUpdate update = delegate.result();
			
			if(null != update) {
				if(!delay) {
					googleMap.animateCamera(update);
				} else {
					//TODO above might sometimes give an IllegalStateEx ..
					googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
						@Override
						public void onCameraChange(CameraPosition paramCameraPosition) {
							googleMap.animateCamera(update);
							googleMap.setOnCameraChangeListener(null);
						}
					});
				}
			}
		}
	}
	
	@Subscribe
	public void onLoadFinished(LocationsLoadedEvent event) {
		googleMap.clear();
		
		if(null != event.cursor) {
			/*
			new AsyncTask<Cursor, Void, Aggregate>() {
				@Override
				protected Aggregate doInBackground(Cursor... params) {
					return plotMarkers(params[0]);
				}
				
				@Override
				protected void onPostExecute(final Aggregate delegate) {
					focusMarkers(delegate, false);
				};
			}.execute(event.cursor);
			*/
			focusMarkers(plotMarkers(event.cursor), true);
		}
	}
	
	@Subscribe
	public void onSelectionChanged(final LocationSelectedEvent event) {
		if(this != event.sender) {
			final ContentValues c = new LocationBroker(getActivity()).loadOrCreate(event.id);
			
			if(c.containsKey("latitude") && c.containsKey("longitude")) {
				final LatLng selected = new LatLng(c.getAsDouble("latitude"), c.getAsDouble("longitude"));
				googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selected, 17));
			}
		}
	}
}