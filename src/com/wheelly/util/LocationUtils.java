package com.wheelly.util;

import android.content.ContentValues;
import android.database.Cursor;

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
}