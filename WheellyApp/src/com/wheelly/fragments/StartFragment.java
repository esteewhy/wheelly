package com.wheelly.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.service.OpenGpsTracker;
import com.wheelly.service.WorkflowNotifier;
import com.wheelly.service.Tracker.TrackListener;

public class StartFragment extends ItemFragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.start_edit, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Intent intent = getActivity().getIntent();
		final ContentValues values = new HeartbeatBroker(getActivity()).loadOrCreate(intent.getLongExtra(BaseColumns._ID, 0));
		
		final Controls c = new Controls();
		
		c.Heartbeat.setValues(values);
		onSave =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					final boolean isNew = !values.containsKey(BaseColumns._ID) || values.getAsLong(BaseColumns._ID) < 0;
					values.putAll(c.Heartbeat.getValues());
					values.put("type", 1);
					final long id = new HeartbeatBroker(getActivity()).updateOrInsert(values);
					intent.putExtra(BaseColumns._ID, id);
					
					if(isNew) {
						final ContentValues stopHeartbeat = new ContentValues();
						stopHeartbeat.putAll(values);
						stopHeartbeat.put("type", 34);
						stopHeartbeat.remove(BaseColumns._ID);
						final WorkflowNotifier n = new WorkflowNotifier(getActivity());
						n.notifyAboutPendingMileage(new HeartbeatBroker(getActivity()).updateOrInsert(stopHeartbeat));
					}
					
					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			};
		
		c.StartButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(final View v) {
						if(new OpenGpsTracker(getActivity())
						.setStartTrackListener(new TrackListener() {
							@Override
							public void onStartTrack(long trackId) {
								ContentValues stopHeartbeat = new ContentValues();
								stopHeartbeat.putAll(values);
								stopHeartbeat.put("track_id", trackId);
								stopHeartbeat.put("type", 34);
								stopHeartbeat.remove("_id");
								final WorkflowNotifier n = new WorkflowNotifier(getActivity());
								n.notifyAboutPendingMileage(new HeartbeatBroker(getActivity()).updateOrInsert(stopHeartbeat));
								v.setEnabled(false);
							}
							
							@Override
							public void onTrackStopped() {
								v.setEnabled(true);
							}
						})
						.Start()) {
							v.setEnabled(false);
						}
					}
				});
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final Button StartButton;
		
		public Controls() {
			final FragmentManager fm = getChildFragmentManager();
			final View view = getView();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			StartButton	= (Button)view.findViewById(R.id.bStart);		
		}
	}
}