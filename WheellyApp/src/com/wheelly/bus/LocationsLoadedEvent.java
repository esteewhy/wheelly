package com.wheelly.bus;

import android.database.Cursor;

public class LocationsLoadedEvent {
	public final Cursor cursor;
	
	public LocationsLoadedEvent(Cursor cursor) {
		this.cursor = cursor;
	}
}