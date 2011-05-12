package com.wheelly.app;

import java.util.concurrent.atomic.AtomicInteger;

import com.wheelly.R;
import com.wheelly.activity.HeartbeatDialog;
import com.wheelly.service.Tracker;
import com.wheelly.service.Tracker.OnStartTrackListener;

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

/**
 * A user control consisting of 2 buttons each of which spawn start or stop
 * heartbeat editing or creation.
 */
public class TripControlBar extends Fragment {
	
	private Controls c;
	private static final AtomicInteger MILEAGE_CONTROL_REQUEST = new AtomicInteger(3000);
	
	private int editStartHeartbeatRequestId;
	private int editStartHeartbeatAndStartTrackingRequestId;
	private int editStopHeartbeatRequestId;
	private boolean canStartTracking = true;
	
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
					Intent intent = new Intent(getActivity(), HeartbeatDialog.class) {{
						putExtra(BaseColumns._ID, (Long)v.getTag(R.id.tag_id));
					}};
					
					ContentValues values = (ContentValues)v.getTag(R.id.tag_values);
					if(null != values) {
						intent.putExtra("heartbeat", values);
					}
					
					int requestId = (Integer)v.getTag(R.id.tag_request_code);
					
					// Stop tracking (if active) *before* entering final heartbeat.
					if(requestId == editStopHeartbeatRequestId) {
						TripControlBarValue val = getValue();
						if(val.TrackId < 0) {
							val.TrackId = val.TrackId * -1;
							new Tracker(getActivity()).Stop(val.TrackId);
							setValue(val);
						}
					} else if(requestId == editStartHeartbeatAndStartTrackingRequestId) {
						final TripControlBarValue val = getValue();
						
						new Tracker(getActivity())
							.setStartTrackListener(new OnStartTrackListener() {
							@Override
							public void onStartTrack(long trackId) {
								// negative means "tracking in progress".
								val.TrackId = trackId * -1;
								setValue(val);
								v.setEnabled(true);
							}
						})
						.Start();
						
						v.setEnabled(false);
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
			long id = data.getLongExtra(BaseColumns._ID, 0);
			ContentValues heartbeat = data.getParcelableExtra("heartbeat");
			final TripControlBarValue value = this.getValue();
			
			if(requestCode == editStartHeartbeatRequestId) {
				value.StartId = id;
				value.StartHeartbeat = heartbeat;
			} else if (requestCode == editStopHeartbeatRequestId) {
				value.StopId = id;
				value.StopHeartbeat = heartbeat;
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
	public void setValue(TripControlBarValue value) {
		c.StartButton.setTag(R.id.tag_track_id, value.TrackId);
		
		c.StartButton.setTag(R.id.tag_id, value.StartId);
		c.StopButton.setTag(R.id.tag_id, value.StopId);
		
		this.canStartTracking = value.StartId > 0 && value.TrackId == 0 && value.StopId == 0;
		
		// Store temp values into controls.
		initButton(c.StartButton, value.StartId,  value.StartHeartbeat,
			R.string.start, R.drawable.btn_start, canStartTracking ? R.drawable.btn_record : R.drawable.btn_start_disabled);
		initButton(c.StopButton, value.StopId, value.StopHeartbeat,
			R.string.stop, R.drawable.btn_stop, R.drawable.btn_stop_disabled);
		
		c.StopButton.setEnabled(value.StartId > 0);
		
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
}