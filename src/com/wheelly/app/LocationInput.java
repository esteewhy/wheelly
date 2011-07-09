package com.wheelly.app;

import ru.orangesoftware.financisto.activity.LocationActivity;
import ru.orangesoftware.financisto.utils.Utils;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationRepository;
import com.wheelly.util.LocationUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Location selection and creation control.
 */
public final class LocationInput extends Fragment {
	
	private static final int NEW_LOCATION_REQUEST = 4002;
	
	private long selectedLocationId = 0;
	private boolean setCurrentLocation;
	private LocationManager locationManager;
	private Location lastFix;
	private Controls c;
	private Cursor locationCursor;
	private SQLiteDatabase db;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Activity ctx = getActivity();  
		locationCursor = new LocationRepository(db = new DatabaseHelper(ctx).getReadableDatabase()).list();
		ctx.startManagingCursor(locationCursor);
		final ListAdapter adapter =
			new SimpleCursorAdapter(ctx,
					android.R.layout.simple_spinner_dropdown_item,
					locationCursor, 
					new String[] {"name"},
					new int[] { android.R.id.text1 }
			);
		
		View v = inflater.inflate(R.layout.select_entry_plus, container, true);
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(ctx)
					.setSingleChoiceItems(adapter,
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
	
	@Override
	public void onDestroyView() {
		db.close();
		super.onDestroyView();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case NEW_LOCATION_REQUEST:
					locationCursor.requery();
					long locationId = data.getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					if (locationId > 0) {
						setValue(locationId);
					}
					break;
			}
		} else {
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
		LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
		
		Criteria criteria = new Criteria(); 
		criteria.setPowerRequirement(Criteria.POWER_LOW); 
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); 
		criteria.setAltitudeRequired(false); 
		criteria.setBearingRequired(false); 
		criteria.setSpeedRequired(false); 
		criteria.setCostAllowed(false); 
		String provider = lm.getBestProvider(criteria, true);
		
		if(provider != null) {
			final Location mLocation = lm.getLastKnownLocation(provider);
			
			if(null!= mLocation) {
				long locationId = resolveLocation(mLocation);
				
				if(locationId > 0) {
					setValue(locationId);
					return true;
				}
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
		ContentValues location = LocationRepository.deserialize(locationCursor);
		//c.locationText.setText(LocationUtils.locationToText(location));
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