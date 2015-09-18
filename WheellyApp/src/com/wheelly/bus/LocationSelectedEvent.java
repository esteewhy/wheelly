package com.wheelly.bus;

public class LocationSelectedEvent {
	public final long id;
	public final Object sender;
	
	public LocationSelectedEvent(long id, Object sender) {
		this.id = id;
		this.sender = sender;
	}
}