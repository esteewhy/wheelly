package com.wheelly.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import com.wheelly.db.LocationBroker;

public class OpenGpsTrackRepository {
	private final Uri BASE = Uri.parse("content://nl.sogeti.android.gpstracker/tracks");
	private final Context context;
	
	public OpenGpsTrackRepository(Context context) {
		this.context = context;
	}
	
	public Cursor list() {
		return context.getContentResolver().query(BASE, null, null, null, "creationtime DESC");
	}
	
	public Cursor waypoints(long trackId) {
		return context.getContentResolver()
			.query(Uri.withAppendedPath(BASE, trackId + "/waypoints"),
				new String[] { "waypoints._id", "time", "longitude", "latitude", "altitude" },
				null,
				null,
				null);
	}
	
	/*
	 * Retrieves track distance in kilometers.
	 */
	public float getDistance(long trackId) {
		final Cursor cursor = waypoints(trackId);
		try {
			if(cursor.moveToFirst()) {
				Location lastLocation = null;
				float mDistanceTraveled = 0;
				do {
                  final Location currentLocation = LOCATION(cursor);
                  
                  // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                  if(currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d) {
                     continue;
                  }
                  
                  if(lastLocation != null) {
                     float travelPart = lastLocation.distanceTo( currentLocation );
                     mDistanceTraveled += travelPart;
                  }
                  lastLocation = currentLocation;
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
				Uri.withAppendedPath(BASE, Long.toString(trackId)),
			values,
			null,
			null
		);
	}
	
	public static Location LOCATION(Cursor c) {
		final Location l = new Location(OpenGpsTrackRepository.class.getName());
        l.setTime(c.getLong(1));
        l.setLongitude(c.getDouble(2));
        l.setLatitude(c.getDouble(3));
        l.setAltitude(c.getDouble(4));
        return l;
	}
}