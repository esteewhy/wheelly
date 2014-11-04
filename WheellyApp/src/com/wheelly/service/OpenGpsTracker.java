package com.wheelly.service;

import nl.sogeti.android.gpstracker.logger.IGPSLoggerServiceRemote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

public class OpenGpsTracker implements Tracker {
	final Intent serviceIntent;
	final Context context;
	
	private TrackListener listener;
	private final Object lock = new Object();
	
	public OpenGpsTracker setStartTrackListener(TrackListener listener) {
		this.listener = listener;
		return this;
	}

	public OpenGpsTracker(final Context context) {
		this.context = context;
		this.serviceIntent = new Intent("nl.sogeti.android.gpstracker.intent.action.GPSLoggerService");
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
		return context.bindService(serviceIntent,
			new ServiceConnection()
			{
				public void onServiceConnected(ComponentName className, IBinder service)
				{
					synchronized (lock)
					{
						try {
							final IGPSLoggerServiceRemote remote = IGPSLoggerServiceRemote.Stub.asInterface(service);
							
							try {
								long trackId = remote.startLogging();
								if (listener != null)
								{
									listener.onStartTrack(trackId);
								}
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							
						} finally {
							context.unbindService(this);
						}
					}
				}
				
				public void onServiceDisconnected(ComponentName className)
				{
				}
			},
			Context.BIND_AUTO_CREATE
		);
	}
	
	public void Stop(final long trackId) {
		context.bindService(serviceIntent, new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				synchronized (lock) {
					try {
						final IGPSLoggerServiceRemote svc = IGPSLoggerServiceRemote.Stub.asInterface(service);
						if(svc.loggingState() > 0) {
							svc.stopLogging();
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
		}, Context.BIND_AUTO_CREATE);
	}
}