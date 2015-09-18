package com.wheelly.db;

import android.content.ContentResolver;
import android.net.Uri;

public final class LocationsSchema {
	public static final String CONTENT_AUTHORITY = "com.wheelly.locations";
	private static final Uri BASE_CONTENT_URI =
		Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_LOCATIONS = "locations";

	public static final String Select =
		"SELECT * FROM locations";
	
	public static final String Single =
		"SELECT * FROM locations WHERE _id = ? LIMIT 1";
	
	public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();
	public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.location";
	public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.locations";
}