package com.wheelly.app;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.wheelly.R;
import com.wheelly.activity.HeartbeatDialog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A user control consisting of 2 buttons each of which spawn start or stop
 * heartbeat editing or creation.
 */
public class TripControlBar extends Fragment {
	
	private Controls c;
	private static final AtomicInteger MILEAGE_CONTROL_REQUEST = new AtomicInteger(3000);
	
	private ITrackRecordingService mytracksService;
	
	private int editStartHeartbeatRequestId;
	private int editStopHeartbeatRequestId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent() {{
			setComponent(new ComponentName(
				getString(R.string.mytracks_service_package),
				getString(R.string.mytracks_service_class))
			);
		}};
		
		if (!getActivity().bindService(intent, new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				synchronized (TripControlBar.this) {
					mytracksService = ITrackRecordingService.Stub.asInterface(service);
				}
			}
		}, Activity.BIND_AUTO_CREATE)) {
			Log.e("W", "Couldn't bind to service.");
		}
	}
	
	/**
	 * Constructs UI and wire up event handlers.
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		editStartHeartbeatRequestId = MILEAGE_CONTROL_REQUEST.incrementAndGet();
		editStopHeartbeatRequestId = MILEAGE_CONTROL_REQUEST.incrementAndGet();
		
		final View view = inflater.inflate(R.layout.start_and_stop_buttons, container, true);
		c = new Controls(view);
		c.StartButton.setTag(R.id.tag_request_code, editStartHeartbeatRequestId);
		c.StopButton.setTag(R.id.tag_request_code, editStopHeartbeatRequestId);

		final OnClickListener listener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), HeartbeatDialog.class);
					intent.putExtra(BaseColumns._ID, (Long)v.getTag(R.id.tag_id));
					ContentValues values = (ContentValues)v.getTag(R.id.tag_values);
					if(null != values) {
						intent.putExtra("heartbeat", values);
					}
					startActivityForResult(intent, (Integer)v.getTag(R.id.tag_request_code));
				}
			};
		
		c.StartButton.setOnClickListener(listener);
		c.StopButton.setOnClickListener(listener);
		
		return view;
	}
	
	/**
	 * Update state after edit activities finished.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			long id = data.getLongExtra(BaseColumns._ID, 0);
			ContentValues heartbeat = data.getParcelableExtra("heartbeat");
			TripControlBarValue value = this.getValue();
			
			if(requestCode == editStartHeartbeatRequestId) {
				value.StartId = id;
				value.StartHeartbeat = heartbeat;
				
				if(null != mytracksService && value.TrackId <= 0) {
					try {
						value.TrackId = mytracksService.startNewTrack();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (requestCode == editStopHeartbeatRequestId) {
				value.StopId = id;
				value.StopHeartbeat = heartbeat;
				
				if(null != mytracksService && value.TrackId > 0) {
					try {
						if(mytracksService.getRecordingTrackId() == value.TrackId) {
							mytracksService.endCurrentTrack();
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			this.setValue(value);
		}
	}
	
	/**
	 * Returns a pair of heartbeats been edited.
	 */
	public TripControlBarValue getValue() {
		TripControlBarValue result = new TripControlBarValue();
		result.StartId			= (Long)c.StartButton.getTag(R.id.tag_id);
		result.StopId			= (Long)c.StopButton.getTag(R.id.tag_id);
		result.StartHeartbeat	= (ContentValues)c.StartButton.getTag(R.id.tag_values);
		result.StopHeartbeat	= (ContentValues)c.StopButton.getTag(R.id.tag_values);
		result.TrackId			= (Long)c.StartButton.getTag(R.id.tag_track_id);
		return result;
	}
	
	private void initButton(Button button, long id, ContentValues values, int defaultTextResource) {
		if(id > 0 && null != values) {
			button.setTag(R.id.tag_values, values);
			button.setText(
				String.format(
					getString(R.string.heartbeat_button_caption),
					values.getAsLong("odometer"),
					values.getAsLong("fuel")));
		} else {
			button.setText(defaultTextResource);
		}
	}
	
	/**
	 * Initialises both edited heartbeats and updates UI.
	 */
	public void setValue(TripControlBarValue value) {
		c.StartButton.setTag(R.id.tag_track_id, value.TrackId);
		
		c.StartButton.setTag(R.id.tag_id, value.StartId);
		c.StopButton.setTag(R.id.tag_id, value.StopId);
		
		// Store temp values into controls.
		initButton(c.StartButton, value.StartId,  value.StartHeartbeat, R.string.start);
		initButton(c.StopButton, value.StopId, value.StopHeartbeat, R.string.stop);
		
		// Update icons.
		c.StopButton.setEnabled(value.StartId > 0);
		c.StartButton.setCompoundDrawablesWithIntrinsicBounds(
			value.StartId > 0
				? R.drawable.btn_start_disabled
				: R.drawable.btn_start, 0, 0, 0);
		c.StopButton.setCompoundDrawablesWithIntrinsicBounds(
			value.StopId > 0
				? R.drawable.btn_stop_disabled
				: R.drawable.btn_stop, 0, 0, 0);
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private static class Controls {
		final Button StartButton;
		final Button StopButton;
		
		public Controls(View view) {
			this.StartButton = (Button)view.findViewById(R.id.bStart);
			this.StopButton = (Button)view.findViewById(R.id.bStop);
		}
	}
}