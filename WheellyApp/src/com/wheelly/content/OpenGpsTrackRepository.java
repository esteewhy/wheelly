package com.wheelly.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.wheelly.db.LocationBroker;

public class OpenGpsTrackRepository {
	private final Context context;
	
	public OpenGpsTrackRepository(Context context) {
		this.context = context;
	}
	
	public Cursor list() {
		return context.getContentResolver().query(
			Uri.parse("content://nl.sogeti.android.gpstracker/tracks"), null, null, null, "creationtime DESC");
	}
	
	/*
	 * Retrieves track distance in kilometers.
	 */
	public float getDistance(long trackId) {
		final Cursor cursor = context.getContentResolver()
			.query(Uri.parse("content://nl.sogeti.android.gpstracker/tracks/" + trackId + "/waypoints"),
				new String[] { "_id", "time", "longitude", "latitude", "altitude" },
				null,
				null,
				null);
		try {
			if(cursor.moveToFirst()) {
				Location lastLocation = null;
				float mDistanceTraveled = 0;
				do {
                  final Location currentLocation = new Location( this.getClass().getName() );
                  currentLocation.setTime(cursor.getLong(1));
                  currentLocation.setLongitude(cursor.getDouble(2));
                  currentLocation.setLatitude(cursor.getDouble(3));
                  currentLocation.setAltitude(cursor.getDouble(4));
                  
                  // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                  if(currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d) {
                     continue;
                  }
                  
                  if(lastLocation != null) {
                     float travelPart = lastLocation.distanceTo( currentLocation );
                     long timePart = currentLocation.getTime() - lastLocation.getTime();
                     mDistanceTraveled += travelPart;
                  }
				} while(cursor.moveToNext());
				return mDistanceTraveled;
			}
		} finally {
			cursor.close();
		}
		return 0;
	}
	
	/*
	 * Renames newly recorded track using start and stop location names.
	 */
	public void renameTrack(long trackId, long start_place_id, long stop_place_id) {
		final LocationBroker broker = new LocationBroker(context);
		final String name =
			broker.loadOrCreate(start_place_id).getAsString("name")
			+ " - "
			+ broker.loadOrCreate(stop_place_id).getAsString("name");
		ContentValues values = new ContentValues();
		values.put("name", name);
		context.getContentResolver().update(
				Uri.parse("content://nl.sogeti.android.gpstracker/tracks"),
			values,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(trackId) }
		);
	}
}