package com.wheelly.service;

public interface Tracker {
	public static interface TrackListener {
		void onStartTrack(long trackId);
		void onTrackStopped();
	}
	
	public Tracker setStartTrackListener(TrackListener listener);

	/**
	 * Checks if respective service is available.
	 */
	public boolean checkAvailability();
	
	public boolean Start();
	
	public void Stop(final long trackId);
}