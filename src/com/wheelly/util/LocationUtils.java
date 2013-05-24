package com.wheelly.util;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationUtils {
	public static String locationToText(String provider, double latitude, double longitude, float accuracy, String resolvedAddress) {
		if (resolvedAddress != null) {
			return resolvedAddress;
		} else {
			return String.format("%1$s, Lat: %2$2f, Lon: %3$2f%4$s",
				provider,
				latitude,
				longitude,
				accuracy > 0 ? String.format(", ± %1$2f m", accuracy) : ""
			);
		}
	}
	
	public static String locationToText(Cursor cursor) {
		return
			locationToText(
				cursor.getString(cursor.getColumnIndexOrThrow("provider")),
				cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
				cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
				cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")),
				cursor.getString(cursor.getColumnIndexOrThrow("resolved_address"))
			);
	}
	
	public static String locationToText(ContentValues location) {
		return
			locationToText(
				location.getAsString("provider"),
				location.getAsDouble("latitude"),
				location.getAsDouble("longitude"),
				location.getAsFloat("accuracy"),
				location.getAsString("resolved_address")
			);
	}
	
	public static String formatDistance(float distance) {
		return String.format("%1$.1f km", distance * .001);
	}
	
	public static Location getLastKnownLocation(Context context) {
		LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		
		Criteria criteria = new Criteria(); 
		criteria.setPowerRequirement(Criteria.POWER_LOW); 
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); 
		criteria.setAltitudeRequired(false); 
		criteria.setBearingRequired(false); 
		criteria.setSpeedRequired(false); 
		criteria.setCostAllowed(false); 
		String provider = lm.getBestProvider(criteria, true);
		
		return provider != null
			? lm.getLastKnownLocation(provider)
			: null;
	}
	
	public static void obtainLocation(Context context, final LocationListener listener) {
		final LocationClient locationClient = new LocationClient(context,
			new GooglePlayServicesClient.ConnectionCallbacks() {
				@Override
				public void onConnected(Bundle paramBundle) {}
				
				@Override
				public void onDisconnected() {}
			},
			new GooglePlayServicesClient.OnConnectionFailedListener() {
				@Override
				public void onConnectionFailed(ConnectionResult paramConnectionResult) {}
			}
		);
		
		locationClient.registerConnectionCallbacks(new ConnectionCallbacks() {
			@Override
			public void onDisconnected() { }
			
			@Override
			public void onConnected(Bundle paramBundle) {
				final LocationRequest req = new LocationRequest();
				req.setInterval(5000);
				req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				req.setFastestInterval(1000);
				locationClient.requestLocationUpdates(req, new LocationListener() {
					@Override
					public void onLocationChanged(Location paramLocation) {
						listener.onLocationChanged(paramLocation);
						
						if(locationClient.isConnected()) {
							locationClient.removeLocationUpdates(this);
							locationClient.disconnect();
						}
					}
				});
			}
		});
		locationClient.connect();
	}
}