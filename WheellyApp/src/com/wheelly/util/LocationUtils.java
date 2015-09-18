package com.wheelly.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

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
	
	public static interface AddressResolveCallback {
		public void onResolved(Address address);
	}
	
	public static void resolveAddress(final Context context, final LatLng point, final String name, final AddressResolveCallback callback) {
		new AsyncTask<Void, Void, Address>() {
			@Override
			protected Address doInBackground(Void... paramArrayOfParams) {
				Geocoder g = new Geocoder(context);
				
				try {
					List<Address> result = g.getFromLocation(point.latitude, point.longitude, 1);
					
					if(result.size() > 0) {
						return result.get(0);
					}
				} catch (IOException e) {
					return null;
				}
				
				try {
					List<Address> result = g.getFromLocationName(name, 1);
					
					if(result.size() > 0) {
						return result.get(0);
					}
				} catch (IOException e) {
					return null;
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Address result) {
				callback.onResolved(result);
			}
		}.execute();
	}
	
	public static String formatAddress(Address address) {
		int i = address.getMaxAddressLineIndex();
		List<String> list = new ArrayList<String>(i + 1);
		
		while(i >= 0) {
			list.add(address.getAddressLine(i--));
		}
		return TextUtils.join(", ", list);
	}
}