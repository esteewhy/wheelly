package com.wheelly.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.wheelly.R;

public class Tracker {
	final Intent serviceIntent;
	final Context context;
	
	private TrackListener listener;

	public static interface TrackListener {
		void onStartTrack(long trackId);
		void onTrackStopped();
	}
	
	public Tracker setStartTrackListener(TrackListener listener) {
		this.listener = listener;
		return this;
	}

	public Tracker(final Context context) {
		this.context = context;
		
		this.serviceIntent = new Intent() {{
			setComponent(new ComponentName(
				context.getString(R.string.mytracks_service_package),
				context.getString(R.string.mytracks_service_class))
			);
		}};
	}
	
	/**
	 * Checks if My Tracks service is available.
	 */
	public boolean checkAvailability() {
		return context.getPackageManager()
			.queryIntentServices(
				this.serviceIntent,
				PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
	}
	
	public boolean Start() {
		final ComponentName name = context.startService(serviceIntent);
		
		if(null != name) {
			return context.bindService(serviceIntent, new ServiceConnection() {
				@Override
				public void onServiceDisconnected(ComponentName name) {
				}
				
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					synchronized (Tracker.this) {
						try {
							final ITrackRecordingService svc = ITrackRecordingService.Stub.asInterface(service);
							long trackId = svc.startNewTrack();
							
							//TODO investigate why the latter returns 0
							if(trackId <= 0) {
								trackId = svc.getRecordingTrackId();
							}
							
							if(null != listener) {
								listener.onStartTrack(trackId);
							}
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							context.unbindService(this);
						}
					}
				}
			}, 0/*Activity.BIND_AUTO_CREATE*/);
		}
		
		return false;
	}
	
	public void Stop(final long trackId) {
		context.bindService(serviceIntent, new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				synchronized (Tracker.this) {
					try {
						final ITrackRecordingService svc = ITrackRecordingService.Stub.asInterface(service);
						if(svc.getRecordingTrackId() > 0 && svc.isRecording()) {
							svc.endCurrentTrack();
						}
						
						if(listener != null) {
							listener.onTrackStopped();
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch(IllegalStateException e) {
						e.printStackTrace();
					} finally {
						context.unbindService(this);
						context.stopService(serviceIntent);
					}
				}
			}
		}, Activity.BIND_AUTO_CREATE);
	}
}