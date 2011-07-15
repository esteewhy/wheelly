package com.wheelly.app;

import java.util.concurrent.atomic.AtomicInteger;

import com.wheelly.R;
import com.wheelly.activity.HeartbeatDialog;
import com.wheelly.content.TrackRepository;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.service.Tracker;
import com.wheelly.service.Tracker.TrackListener;
import com.wheelly.util.DateUtils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

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
					int requestId = (Integer)v.getTag(R.id.tag_request_code);
					
					// Stop tracking (if active) *before* entering final heartbeat.
					if(requestId == editStopHeartbeatRequestId) {
						stop(v);
					} else if(requestId == editStartHeartbeatAndStartTrackingRequestId) {
						start(v);
					} else {
						startHeartbeatActivity(v, requestId);
					}
				}
			};
		
		c.StartButton.setOnClickListener(listener);
		c.StopButton.setOnClickListener(listener);
		return view;
	}
	
	private void startHeartbeatActivity(View v, int requestId) {
		Intent intent = new Intent(getActivity(), HeartbeatDialog.class);
		intent.putExtra(BaseColumns._ID, (Long)v.getTag(R.id.tag_id));
		
		ContentValues values = (ContentValues)v.getTag(R.id.tag_values);
		if(null != values) {
			intent.putExtra("heartbeat", values);
		}
		
		startActivityForResult(intent, requestId);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if(v == c.StartButton || v == c.StopButton) {
			final Cursor cursor =
				getActivity().getContentResolver()
					.query(
						Heartbeats.CONTENT_URI,
						new String[] {
							"h." + BaseColumns._ID,
							"odometer",
							"fuel",
							"_created"
						},
						Heartbeats.IconColumnExpression + " = 0",
						null,
						"h._created");
			
			if(cursor.moveToFirst()) {
				menu.setHeaderTitle("Orphan heartbeats");
				
				int[] indices = new int[] {
					cursor.getColumnIndex(BaseColumns._ID),
					cursor.getColumnIndex("odometer"),
					cursor.getColumnIndex("fuel"),
					cursor.getColumnIndex("_created"),
				};
				
				long[] ids = new long[cursor.getCount()];
				int idx = 0;
				
				while(cursor.moveToNext()) {
					
					ids[idx] = cursor.getLong(indices[0]);
					
					menu.add(v.getId(), idx++, Menu.NONE,
						DateUtils.formatVarying(cursor.getString(indices[3]))
						+ ": " + cursor.getLong(indices[1])
						+ " / " + cursor.getLong(indices[2]));
				}
				
				v.setTag(ids);
			}
			
			cursor.close();
		}
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int viewId = item.getGroupId();
		
		int action = viewId == c.StartButton.getId()
			? 1 : viewId == c.StopButton.getId() ? 2 : 0;
		
		if(action > 0) {
			final Value val = getValue();
			long[] ids = (long[])(action == 1 ? c.StartButton : c.StopButton).getTag();
			final ContentValues h = new HeartbeatBroker(getActivity()).loadOrCreate(ids[item.getItemId()]);
			
			switch(action) {
			case 1: val.StartHeartbeat = h; break;
			case 2: val.StopHeartbeat = h; break;
			}
			
			setValue(val);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void stop(final View v) {
		final Value val = getValue();
		
		if(val.TrackId < 0) {
			val.TrackId = val.TrackId * -1;
			new Tracker(getActivity())
				.setStartTrackListener(new TrackListener() {
					@Override
					public void onTrackStopped() {
						presetStopMileage(val);
						setValue(val);
						startHeartbeatActivity(v, editStopHeartbeatRequestId);
					}
					
					@Override
					public void onStartTrack(long trackId) {}
				})
				.Stop(val.TrackId);
		} else {
			startHeartbeatActivity(v, editStopHeartbeatRequestId);
		}
	}
	
	/**
	 * Calculate final mileage in case track length is known.
	 */
	private void presetStopMileage(Value val) {
		float distance = new TrackRepository(getActivity()).getDistance(val.TrackId);
		
		if(val.StartHeartbeat != null) {
			// On new mileage there might be no stop heartbeat until after [stop]
			// button been clicked and heartbeat form submitted,
			// so we have to create default heartbeat values in advance
			// to pre-set mileage to.
			if(null == val.StopHeartbeat) {
				val.StopHeartbeat = new HeartbeatBroker(getActivity()).loadOrCreate(-1);
			}
			
			val.StopHeartbeat.put("odometer",
				val.StartHeartbeat.getAsLong("odometer")
				+ (long)Math.ceil(distance));
		}
	}
	
	private void start(final View v) {
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
	}
	
	/**
	 * Update state after edit activities finished.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			ContentValues heartbeat = data.getParcelableExtra("heartbeat");
			
			final long id = heartbeat.containsKey(BaseColumns._ID)
				? heartbeat.getAsLong(BaseColumns._ID)
				: -1;
			
			if(id <= 0 && data.hasExtra(BaseColumns._ID)) {
				heartbeat.put(BaseColumns._ID, data.getLongExtra(BaseColumns._ID, -1));
			}
			
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
		
		if(null != values) {
			button.setTag(R.id.tag_values, values);
		}
		
		if(id > 0 && null != values) {
			button.setText(
				String.format(
					getString(R.string.heartbeat_button_caption),
					values.getAsLong("odometer"),
					values.getAsLong("fuel")));
			unregisterForContextMenu(button);
		} else {
			button.setText(defaultTextResource);
			registerForContextMenu(button);
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