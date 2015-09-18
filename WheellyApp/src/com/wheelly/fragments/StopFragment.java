package com.wheelly.fragments;

import java.util.Date;

import android.content.ContentValues;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

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
	OnMenuItemClickListener onStop;
	OnMenuItemClickListener onConfigStopMenuItem;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.stop_edit, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Bundle args = getArgumentsOrDefault();
		final long id = args.getLong(BaseColumns._ID, 0);
		final ContentValues values = new MileageBroker(getActivity()).loadOrCreate(id);
		final long start = values.getAsLong("odometer");
		final long startPlaceId = values.getAsLong("place_id");
		
		if(34 == values.getAsInteger("type")) {
			values.put("_created", DateUtils.dbFormat.format(new Date()));
			values.put("place_id", 0);
		}
		
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
					args.putLong(BaseColumns._ID, id);
					
					final WorkflowNotifier n = new WorkflowNotifier(getActivity());
					n.canceNotificationForMileage(id);
					finish(args);
				}
			};
		
		onStop =
			new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem v) {
					final long trackId = values.getAsLong("track_id");
					new OpenGpsTracker(getActivity())
					.setStartTrackListener(new TrackListener() {
						@Override
						public void onTrackStopped() {
							values.putAll(c.Heartbeat.getValues());
							values.put("type", 2);
							final TrackRepository trackRepository = new TrackRepository(getActivity());
							final float distance = trackRepository.getDistance(trackId);
							values.put("distance", distance);
							values.put("odometer", start + distance);
							values.put("_created", DateUtils.dbFormat.format(new Date()));
							trackRepository.renameTrack(trackId, startPlaceId, c.Heartbeat.getValues().getAsLong("place_id"));
							c.bind(values);
						}
						
						@Override
						public void onStartTrack(long trackId) {}
					})
					.Stop(trackId);
					v.setEnabled(false);
					return true;
				}
			};
		onConfigStopMenuItem =
			new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					item.setVisible(values.getAsInteger("type") == 34);
					return true;
				}
			};
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		final MenuItem mi = menu.findItem(R.id.opt_menu_stop);
		if(null != onConfigStopMenuItem && null != mi) {
			onConfigStopMenuItem.onMenuItemClick(mi);
		}
		super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(R.id.opt_menu_stop == item.getItemId() && null != onStop) {
			onStop.onMenuItemClick(item);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final MileageInput Distance;
		final TrackInput Track;
		
		public Controls() {
			final FragmentManager fm = getChildFragmentManager();
			final View view = getView();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			Distance	= (MileageInput)view.findViewById(R.id.mileage);
			Track		= (TrackInput)fm.findFragmentById(R.id.track);
		}
		
		void bind(ContentValues values) {
			Heartbeat.setValues(values);
			Distance.setAmount(values.getAsLong("distance"));
			Track.setValue(values.getAsLong("track_id"));
			BusProvider.getInstance().post(new TrackChangedEvent(values.getAsLong("track_id")));
		}
	}
}