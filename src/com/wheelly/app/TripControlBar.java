package com.wheelly.app;

import java.util.concurrent.atomic.AtomicInteger;

import com.wheelly.R;
import com.wheelly.activity.HeartbeatDialog;
import com.wheelly.content.TrackRepository;
import com.wheelly.service.Tracker;
import com.wheelly.service.Tracker.TrackListener;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

/**
 * Widget with 2 buttons to launch editing/creating of start or stop heartbeats.
 */
public class TripControlBar extends Fragment {
	
	private Controls c;
	private static final AtomicInteger MILEAGE_CONTROL_REQUEST = new AtomicInteger(3000);
	
	private int editStartHeartbeatRequestId;
	private int editStartHeartbeatAndStartTrackingRequestId;
	private int editStopHeartbeatRequestId;
	private boolean canStartTracking = true;
	
	public static interface OnValueChangedListener {
		void onValueChanged(Value value);
	}
	
	private OnValueChangedListener onValueChangedListener;
	
	public void setOnValueChangedListener(OnValueChangedListener listener) {
		this.onValueChangedListener = listener;
	}
	
	protected void triggerValueChanged(Value value) {
		if(null != onValueChangedListener) {
			onValueChangedListener.onValueChanged(value);
		}
		setValue(value);
	}
	
	/**
	 * Constructs UI and wire up event handlers.
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		editStartHeartbeatRequestId = MILEAGE_CONTROL_REQUEST.incrementAndGet();
		editStopHeartbeatRequestId = MILEAGE_CONTROL_REQUEST.incrementAndGet();
		editStartHeartbeatAndStartTrackingRequestId = MILEAGE_CONTROL_REQUEST.incrementAndGet();
		
		final View view = inflater.inflate(R.layout.start_and_stop_buttons, container, true);
		c = new Controls(view);
		c.StartButton.setTag(R.id.tag_request_code, editStartHeartbeatRequestId);
		c.StopButton.setTag(R.id.tag_request_code, editStopHeartbeatRequestId);

		final OnClickListener listener =
			new OnClickListener() {
				@Override
				public void onClick(final View v) {
					Intent intent = new Intent(getActivity(), HeartbeatDialog.class);
					intent.putExtra(BaseColumns._ID, (Long)v.getTag(R.id.tag_id));
					
					ContentValues values = (ContentValues)v.getTag(R.id.tag_values);
					if(null != values) {
						intent.putExtra("heartbeat", values);
					}
					
					int requestId = (Integer)v.getTag(R.id.tag_request_code);
					
					// Stop tracking (if active) *before* entering final heartbeat.
					if(requestId == editStopHeartbeatRequestId) {
						final Value val = getValue();
						if(val.TrackId < 0) {
							val.TrackId = val.TrackId * -1;
							new Tracker(getActivity())
								.setStartTrackListener(new TrackListener() {
									
									@Override
									public void onTrackStopped() {
										float distance = new TrackRepository(getActivity()).getDistance(val.TrackId);
										
Toast.makeText(getActivity(), Float.toString(distance), 9000).show();
										
										if(val.StartHeartbeat != null && val.StopHeartbeat != null) {
											val.StopHeartbeat.put("odometer",
												val.StartHeartbeat.getAsLong("odometer")
												+ (long)Math.ceil(distance));
										}
										setValue(val);
									}
									
									@Override
									public void onStartTrack(long trackId) {}
								})
								.Stop(val.TrackId);
						}
					} else if(requestId == editStartHeartbeatAndStartTrackingRequestId) {
						final Value val = getValue();
						
						if(new Tracker(getActivity())
							.setStartTrackListener(new TrackListener() {
								@Override
								public void onStartTrack(long trackId) {
									// negative means "tracking in progress".
									val.TrackId = trackId * -1;
									triggerValueChanged(val);
									v.setEnabled(true);
								}
								
								@Override
								public void onTrackStopped() {}
							})
							.Start()) {
							v.setEnabled(false);
						}
						return;
					}
					
					startActivityForResult(intent, requestId);
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
			ContentValues heartbeat = data.getParcelableExtra("heartbeat");
			final Value value = this.getValue();
			
			if(requestCode == editStartHeartbeatRequestId) {
				value.StartHeartbeat = heartbeat;
			} else if (requestCode == editStopHeartbeatRequestId) {
				value.StopHeartbeat = heartbeat;
				
				if(value.TrackId > 0
					&& value.StopHeartbeat.containsKey("place_id")
					&& value.StartHeartbeat.containsKey("place_id")) {
					
					long start_place_id = value.StartHeartbeat.getAsLong("place_id");
					long stop_place_id = value.StopHeartbeat.getAsLong("place_id");
					
					if(start_place_id > 0 && stop_place_id > 0) {
						new TrackRepository(getActivity()).renameTrack(
							value.TrackId,
							start_place_id,
							stop_place_id);
					}
				}
			}
			
			triggerValueChanged(value);
		}
	}
	
	/**
	 * Returns a pair of heartbeats been edited.
	 */
	public Value getValue() {
		final Value result = new Value();
		result.StartHeartbeat	= (ContentValues)c.StartButton.getTag(R.id.tag_values);
		result.StopHeartbeat	= (ContentValues)c.StopButton.getTag(R.id.tag_values);
		result.TrackId			= (Long)c.StartButton.getTag(R.id.tag_track_id);
		return result;
	}
	
	private void initButton(
			Button button,
			long id,
			ContentValues values,
			int defaultTextResource,
			int iconResource,
			int disabledIconResource) {
		
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
		
		// Update icons.
		button.setCompoundDrawablesWithIntrinsicBounds(
			id > 0 ? disabledIconResource : iconResource, 0, 0, 0);
	}
	
	/**
	 * Initialises both edited heartbeats and updates UI.
	 */
	public void setValue(Value value) {
		c.StartButton.setTag(R.id.tag_track_id, value.TrackId);
		
		final long startId = value.StartHeartbeat != null
				&& value.StartHeartbeat.containsKey(BaseColumns._ID)
			? value.StartHeartbeat.getAsLong(BaseColumns._ID)
			: -1;
		
		final long stopId = value.StopHeartbeat != null
				&& value.StopHeartbeat.containsKey(BaseColumns._ID)
			? value.StopHeartbeat.getAsLong(BaseColumns._ID)
			: -1;
		
		this.canStartTracking = startId > 0
			&& value.TrackId == 0
			&& stopId <= 0
			&& new Tracker(getActivity()).checkAvailability();
		
		// Store temp values into controls.
		initButton(c.StartButton, startId,  value.StartHeartbeat,
			R.string.start, R.drawable.btn_start, canStartTracking ? R.drawable.btn_record : R.drawable.btn_start_disabled);
		initButton(c.StopButton, stopId, value.StopHeartbeat,
			R.string.stop, R.drawable.btn_stop, R.drawable.btn_stop_disabled);
		
		c.StopButton.setEnabled(startId > 0);
		
		// Pass request id signaling whether to start tracking after edit activity finishes.
		c.StartButton.setTag(R.id.tag_request_code,
			canStartTracking
				? editStartHeartbeatAndStartTrackingRequestId
				: editStartHeartbeatRequestId);
		
		// Revert "record" button caption to default.
		if(canStartTracking) {
			c.StartButton.setText(R.string.record);
		}
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
	
	public static class Value {
		public ContentValues StartHeartbeat;
		public ContentValues StopHeartbeat;
		
		public long TrackId;
	}
}