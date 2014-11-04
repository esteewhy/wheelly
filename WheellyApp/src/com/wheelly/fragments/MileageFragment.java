package com.wheelly.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.issue40537.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.app.LocationInput;
import com.wheelly.app.TrackInput;
import com.wheelly.app.TrackInput.OnTrackChangedListener;
import com.wheelly.app.TripControlBar;
import com.wheelly.bus.TrackChangedEvent;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.MileageBroker;
import com.wheelly.service.WorkflowNotifier;
import com.wheelly.widget.MileageInput;
import com.wheelly.content.TrackRepository;

/**
 * Edit single trip properties and manipulate associated heartbeats.
 */
public class MileageFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.mileage_edit, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		//components
		final Intent intent = getActivity().getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final ContentValues values = new MileageBroker(getActivity()).loadOrCreate(id);
		
		final Controls c = new Controls();
		
		c.Mileage.setAmount(values.getAsLong("mileage"));
		c.Destination.setValue(values.getAsLong("location_id"));
		c.Track.setOnTrackChangedListener(new OnTrackChangedListener() {
			@Override
			public void onTrackChanged(long trackId) {
				// @todo Compare to default
				if(c.Mileage.getAmount() == 0) {
					c.Mileage.setAmount(trackId > 0
						? (long)Math.ceil(new TrackRepository(getActivity()).getDistance(trackId))
						: 0);
				}
				
				BusProvider.getInstance().post(new TrackChangedEvent(trackId));
			}
		});
		c.Track.setValue(values.getAsLong("track_id"));
		
		final TripControlBar.Value heartbeats = new TripControlBar.Value();
		heartbeats.TrackId = values.getAsLong("track_id");
		BusProvider.getInstance().post(new TrackChangedEvent(heartbeats.TrackId));
		
		HeartbeatBroker broker = new HeartbeatBroker(getActivity());
		heartbeats.StartHeartbeat = broker.loadOrCreate(values.getAsLong("start_heartbeat_id"));
		heartbeats.StopHeartbeat = broker.loadOrCreate(values.getAsLong("stop_heartbeat_id"));
		c.Heartbeats.setOnValueChangedListener(new TripControlBar.OnValueChangedListener() {
			@Override
			public void onValueChanged(TripControlBar.Value value) {
				if(c.Mileage.getAmount() == 0
						&& value.StartHeartbeat != null
						&& value.StartHeartbeat.containsKey("odometer")
						&& value.StopHeartbeat != null
						&& value.StopHeartbeat.containsKey("odometer")) {
					
					final long distance =
							value.StopHeartbeat.getAsLong("odometer")
							- value.StartHeartbeat.getAsLong("odometer");
					
					if(distance > 0) {
						c.Mileage.setAmount(distance);
					}
				}
				
				if(c.Track.getValue() == 0
						&& value.TrackId > 0) {
					c.Track.setValue(value.TrackId);
				}
				
				if(c.Destination.getValue() <= 0
						&& value.StopHeartbeat != null
						&& value.StopHeartbeat.containsKey("place_id")) {
					c.Destination.setValue(value.StopHeartbeat.getAsLong("place_id"));
				}
			}
		});
		c.Heartbeats.setValue(heartbeats);
		
		c.Save.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					values.put("mileage",		c.Mileage.getAmount());
					values.put("location_id",	c.Destination.getValue());
					
					final TripControlBar.Value heartbeats = c.Heartbeats.getValue();
					
					final long startHeartbeatId =
						null != heartbeats.StartHeartbeat
							&& heartbeats.StartHeartbeat.containsKey(BaseColumns._ID)
						? heartbeats.StartHeartbeat.getAsLong(BaseColumns._ID)
						: -1;
					
					final long stopHeartbeatId =
						null != heartbeats.StopHeartbeat
							&& heartbeats.StopHeartbeat.containsKey(BaseColumns._ID)
						? heartbeats.StopHeartbeat.getAsLong(BaseColumns._ID)
						: -1;
					
					if(startHeartbeatId > 0) {
						values.put("start_heartbeat_id", startHeartbeatId);
					}
					
					if(stopHeartbeatId > 0) {
						values.put("stop_heartbeat_id", stopHeartbeatId);
					}
					
					final long trackId = c.Track.getValue();
					values.put("track_id", trackId > 0 ? trackId : heartbeats.TrackId);
					
					final long id = new MileageBroker(getActivity()).updateOrInsert(values);
					intent.putExtra(BaseColumns._ID, id);
					
					final WorkflowNotifier n = new WorkflowNotifier(getActivity());
					
					if(startHeartbeatId < 0 || stopHeartbeatId < 0) {
						n.notifyAboutPendingMileage(id);
					} else {
						n.canceNotificationForMileage(id);
					}
					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			});
		
		c.Cancel.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						getActivity().setResult(Activity.RESULT_CANCELED);
						getActivity().finish();
					}
				});
		
		if(intent.hasExtra("ui_command")) {
			final int uiCommand = intent.getIntExtra("ui_command", 0);
			c.Heartbeats.performUICommand(uiCommand);
		}
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final MileageInput Mileage;
		final LocationInput Destination;
		final TrackInput Track;
		final TripControlBar Heartbeats; 
		final View Save;
		final View Cancel;
		
		public Controls() {
			final FragmentManager fm = getChildFragmentManager();
			final View view = getView();
			
			Mileage		= (MileageInput)view.findViewById(R.id.mileage);
			Destination	= (LocationInput)fm.findFragmentById(R.id.place);
			Track		= (TrackInput)fm.findFragmentById(R.id.track);
			Heartbeats	= (TripControlBar)fm.findFragmentById(R.id.heartbeats);
			Save		= (View)view.findViewById(R.id.bSave);
			Cancel		= (View)view.findViewById(R.id.bSaveAndNew);
		}
	}
}