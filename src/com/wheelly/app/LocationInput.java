package com.wheelly.app;

import ru.orangesoftware.financisto.activity.LocationActivity;
import ru.orangesoftware.financisto.utils.Utils;

import com.wheelly.R;
import com.wheelly.activity.LocationsList;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.db.LocationBroker;
import com.wheelly.util.LocationUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Location selection and creation control.
 */
@SuppressLint("NewApi")
public final class LocationInput extends Fragment {
	
	private static final int NEW_LOCATION_REQUEST = 4002;
	private static final int EDIT_LOCATION_REQUEST = 4003;
	
	private long selectedLocationId = 0;
	private boolean setCurrentLocation;
	private LocationManager locationManager;
	private Location lastFix;
	private Controls c;
	private Cursor locationCursor;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Activity ctx = getActivity();
		locationCursor = getActivity().getContentResolver().query(Locations.CONTENT_URI, null, null, null, null);
		ctx.startManagingCursor(locationCursor);
		
		View v = inflater.inflate(R.layout.select_entry_plus, container, true);
		
		v.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View paramView) {
				Intent intent = new Intent(ctx, LocationsList.class);
				intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, selectedLocationId);
				startActivityForResult(intent, EDIT_LOCATION_REQUEST);
				return true;
			}
		});
		
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				new AlertDialog.Builder(getActivity())
					.setSingleChoiceItems(buildAdapter(locationCursor),
						Utils.moveCursor(locationCursor, BaseColumns._ID, selectedLocationId),
						new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								locationCursor.moveToPosition(which);
								setLocationFromCursor();
							}
						}
					)
					.setTitle(R.string.location)
					.show();
			}
		});
		
		c = new Controls(v);
		c.labelView.setText(R.string.location_input_label);
		c.locationAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ctx, LocationActivity.class);
				startActivityForResult(intent, NEW_LOCATION_REQUEST);
			}
		});
		
		return v;
	}
	
	private ListAdapter buildAdapter(Cursor cursor) {
		final Location location = LocationUtils.getLastKnownLocation(getActivity());
		final Location dest = new Location(location);
		
		return
			new SimpleCursorAdapter(getActivity(),
					R.layout.location_item,
					cursor, 
					new String[] {"name", "resolved_address" },
					new int[] { android.R.id.text1, android.R.id.text2 }, 0
			) {
				@Override
				public void bindView(View view, Context context, Cursor cursor) {
					super.bindView(view, context, cursor);
					
					((TextView)view.findViewById(android.R.id.text1))
						.setTextColor(getResources().getColor(android.R.color.black));
					
					if(null != location) {
						TextView tv = (TextView)view.findViewById(R.id.text3);
						dest.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
						dest.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
						tv.setText(String.valueOf(location.distanceTo(dest)));
					}
				}
			};
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case NEW_LOCATION_REQUEST:
				case EDIT_LOCATION_REQUEST:
					locationCursor.requery();
					long locationId = data.getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					if (locationId > 0) {
						setValue(locationId);
					}
					break;
			}
		}
	}
	
	public long getValue() {
		return selectedLocationId;
	}
	
	public void setValue(long locationId) {
		if (locationId <= 0) {
			if(!selectNearestExistingLocation()) { 
				selectCurrentLocation(false);
			}
		} else {
			if (Utils.moveCursor(locationCursor, BaseColumns._ID, locationId) != -1) {
				setLocationFromCursor();
			}
		}
	}
	
	private boolean selectNearestExistingLocation() {
		final Location mLocation = LocationUtils.getLastKnownLocation(getActivity());
		
		if(null!= mLocation) {
			Toast.makeText(getActivity(), "Obtained location", Toast.LENGTH_LONG).show();
			long locationId = resolveLocation(mLocation);
			
			if(locationId > 0) {
				Toast.makeText(getActivity(), "Resolved location id: " + locationId, Toast.LENGTH_LONG).show();
				setValue(locationId);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Attempts to find a closest to given location among existing in the db.
	 */
	private long resolveLocation(Location location) {
		long locationId = -1;
		float minDistance = 10e+9f;
		
		if(locationCursor.moveToFirst()) {
			do {
				final float distance = location.distanceTo(new Location("existing") {{
					setLongitude(locationCursor.getDouble(locationCursor.getColumnIndex("longitude")));
					setLatitude(locationCursor.getDouble(locationCursor.getColumnIndex("latitude")));
				}});
				
				if(minDistance >= distance) {
					minDistance = distance;
					locationId = locationCursor.getLong(locationCursor.getColumnIndex(BaseColumns._ID)); 
				}
			} while(locationCursor.moveToNext());
		}
		
		return locationId;
	}
	
	/**
	 * Attempts to select location from current cursor position.
	 */
	private void setLocationFromCursor() {
		ContentValues location = LocationBroker.deserialize(locationCursor);
		//c.locationText.setText(LocationUtils.locationToText(location));
		setLocation(location);
	}
	
	private void setLocation(ContentValues location) {
		c.locationText.setText(location.getAsString("name"));
		selectedLocationId = location.getAsLong(BaseColumns._ID);
		setCurrentLocation = false;
	}
	
	protected void selectCurrentLocation(boolean forceUseGps) {
		setCurrentLocation = true;
		selectedLocationId = 0;
		
		// Start listener to find current location
		locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
		String provider = locationManager.getBestProvider(new Criteria(), true);
		
		if (provider != null) {
			lastFix = locationManager.getLastKnownLocation(provider);
		}
		
		if (lastFix != null) {
			setLocation(lastFix);
			connectGps(forceUseGps);
		} else {
			// No enabled providers found, so disable option
			c.locationText.setText(R.string.no_fix);
		}
	}
	
	private class DefaultLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.i(">>>>>>>>>", "onLocationChanged "+location.toString());
			lastFix = location;
			if (setCurrentLocation) {
				setLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	private final LocationListener networkLocationListener = new DefaultLocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			super.onLocationChanged(location);
			locationManager.removeUpdates(networkLocationListener);
		}
	};
	
	private final LocationListener gpsLocationListener = new DefaultLocationListener();
	
	private void setLocation(Location lastFix) {
		if (lastFix.getProvider() == null) {
			c.locationText.setText(R.string.no_fix);
		} else {
			c.locationText.setText(LocationUtils.locationToText(
				lastFix.getProvider(), 
				lastFix.getLatitude(),
				lastFix.getLongitude(), 
				lastFix.hasAccuracy() ? lastFix.getAccuracy() : 0,
				null)
			);
		}
	}
	
	private void connectGps(boolean forceUseGps) {
		if (locationManager != null) {
			boolean useGps = forceUseGps;
			
			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0,
					networkLocationListener
				);
			}
			
			if (useGps && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 1000, 0,
					gpsLocationListener
				);
			}
		}
	}

	private void disconnectGPS() {
		if (locationManager != null) {
			locationManager.removeUpdates(networkLocationListener);
			locationManager.removeUpdates(gpsLocationListener);
		}
	}
	
	@Override
	public void onDestroy() {
		disconnectGPS();
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		disconnectGPS();
		super.onPause();
	}
	
	private static class Controls {
		final TextView locationText;
		final TextView labelView;
		final ImageView locationAdd;
		
		public Controls(View v) {
			this.locationText	= (TextView)v.findViewById(R.id.data);
			this.labelView 		= (TextView)v.findViewById(R.id.label);
			this.locationAdd	= (ImageView)v.findViewById(R.id.plus_minus);
		}
	}
}