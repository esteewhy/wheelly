package com.wheelly.bus;

import android.location.Location;

public class LocationChangedEvent {
	public final Location location;
	
	public LocationChangedEvent(Location location) {
		this.location = location;
	}
}