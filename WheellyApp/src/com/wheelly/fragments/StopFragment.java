package com.wheelly.fragments;

import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.app.TrackInput;
import com.wheelly.app.TrackInput.OnTrackChangedListener;
import com.wheelly.bus.TrackChangedEvent;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.MileageBroker;
import com.wheelly.service.OpenGpsTracker;
import com.wheelly.service.WorkflowNotifier;
import com.wheelly.service.Tracker.TrackListener;
import com.wheelly.util.DateUtils;
import com.wheelly.widget.MileageInput;
import com.wheelly.widget.MileageInput.OnAmountChangedListener;
import com.wheelly.content.TrackRepository;

public class StopFragment extends ItemFragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.stop_edit, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Intent intent = getActivity().getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final ContentValues values = new MileageBroker(getActivity()).loadOrCreate(id);
		final long start = values.getAsLong("odometer");
		
		final Controls c = new Controls();
		c.bind(values);
		c.Heartbeat.c.OdometerEditText.setOnAmountChangedListener(new OnAmountChangedListener() {
			@Override
			public void onAmountChanged(long oldAmount, long newAmount) {
				if(oldAmount != newAmount) {
					c.Distance.setAmount(newAmount - start);
				}
			}
		});
		c.Track.setOnTrackChangedListener(new OnTrackChangedListener() {
			@Override
			public void onTrackChanged(long trackId) {
				// @todo Compare to default
				if(c.Distance.getAmount() == 0) {
					c.Distance.setAmount(trackId > 0
						? (long)Math.ceil(new TrackRepository(getActivity()).getDistance(trackId))
						: 0);
				}
				
				BusProvider.getInstance().post(new TrackChangedEvent(trackId));
			}
		});
		onSave =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					values.put("distance", c.Distance.getAmount());
					
					final long trackId = c.Track.getValue();
					values.put("track_id", trackId > 0 ? trackId : c.Track.getValue());
					values.putAll(c.Heartbeat.getValues());
					values.remove("type");;
					values.put("type", 2);
					final long id = new HeartbeatBroker(getActivity()).updateOrInsert(values);
					intent.putExtra(BaseColumns._ID, id);
					
					final WorkflowNotifier n = new WorkflowNotifier(getActivity());
					n.canceNotificationForMileage(id);
					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			};
		
		c.StopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final long trackId = values.getAsLong("track_id");
				new OpenGpsTracker(getActivity())
				.setStartTrackListener(new TrackListener() {
					@Override
					public void onTrackStopped() {
						values.put("type", 2);
						final float distance = new TrackRepository(getActivity()).getDistance(trackId);
						values.put("distance", distance);
						values.put("odometer", start + distance);
						values.put("_created", DateUtils.dbFormat.format(new Date()));
						c.bind(values);
					}
					
					@Override
					public void onStartTrack(long trackId) {}
				})
				.Stop(trackId);
			}
		});
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final MileageInput Distance;
		final TrackInput Track;
		final Button StopButton;
		
		public Controls() {
			final FragmentManager fm = getChildFragmentManager();
			final View view = getView();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			Distance		= (MileageInput)view.findViewById(R.id.mileage);
			Track		= (TrackInput)fm.findFragmentById(R.id.track);
			StopButton	= (Button)view.findViewById(R.id.bStop);		
		}
		
		void bind(ContentValues values) {
			Heartbeat.setValues(values);
			Distance.setAmount(values.getAsLong("distance"));
			Track.setValue(values.getAsLong("track_id"));
			BusProvider.getInstance().post(new TrackChangedEvent(values.getAsLong("track_id")));
			StopButton.setEnabled(values.getAsInteger("type") == 34);
		}
	}
}