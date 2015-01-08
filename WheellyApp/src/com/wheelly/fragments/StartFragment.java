package com.wheelly.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.service.OpenGpsTracker;
import com.wheelly.service.WorkflowNotifier;
import com.wheelly.service.Tracker.TrackListener;

public class StartFragment extends ItemFragment {
	OnMenuItemClickListener onStart;
	OnMenuItemClickListener onConfigStartMenuItem;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.start_edit, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Bundle args = getArgumentsOrDefault();
		final long id = args.getLong(BaseColumns._ID, -1);
		final ContentValues values = new HeartbeatBroker(getActivity()).loadOrCreate(id);
		final HeartbeatInput input = (HeartbeatInput)getChildFragmentManager().findFragmentById(R.id.heartbeat);
		input.setValues(values);
		
		onSave =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					final boolean isNew = !values.containsKey(BaseColumns._ID) || values.getAsLong(BaseColumns._ID) < 0;
					values.putAll(input.getValues());
					values.put("type", 1);
					final boolean hasStop = values.containsKey("stop_id");
					values.remove("stop_id");
					final long id = new HeartbeatBroker(getActivity()).updateOrInsert(values);
					
					if(isNew && !hasStop) {
						prepareStopHeartbeat(values, 0);
					}
					
					args.putLong(BaseColumns._ID, id);
					finish(args);
				}
			};
		
		onStart =
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(final MenuItem v) {
						if(new OpenGpsTracker(getActivity())
						.setStartTrackListener(new TrackListener() {
							@Override
							public void onStartTrack(long trackId) {
								prepareStopHeartbeat(input.getValues(), trackId);
								v.setEnabled(false);
							}
							
							@Override
							public void onTrackStopped() {
								//v.setEnabled(true);
							}
						})
						.Start()) {
							//v.setEnabled(false);
						}
						return true;
					}
				};
		onConfigStartMenuItem =
			new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					item.setVisible(true);
					return true;
				}
			};
	}
	
	private void prepareStopHeartbeat(ContentValues values, long trackId) {
		ContentValues stopHeartbeat = new ContentValues();
		stopHeartbeat.putAll(values);
		
		if(trackId > 0) {
			stopHeartbeat.put("track_id", trackId);
		}
		
		stopHeartbeat.put("type", 34);
		stopHeartbeat.remove("_id");
		final long stopId = new HeartbeatBroker(getActivity()).updateOrInsert(stopHeartbeat);
		final WorkflowNotifier n = new WorkflowNotifier(getActivity());
		n.notifyAboutPendingMileage(stopId);
		values.put("stop_id", stopId);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		final MenuItem mi = menu.findItem(R.id.opt_menu_start);
		if(null != onConfigStartMenuItem && null != mi) {
			onConfigStartMenuItem.onMenuItemClick(mi);
		}
		super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(R.id.opt_menu_start == item.getItemId() && null != onStart) {
			onStart.onMenuItemClick(item);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}