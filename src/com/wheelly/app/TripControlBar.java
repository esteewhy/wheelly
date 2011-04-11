package com.wheelly.app;

import java.util.concurrent.atomic.AtomicInteger;

import com.wheelly.R;
import com.wheelly.activity.Heartbeat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;

public class TripControlBar extends Fragment {
	
	Controls c;
	static final AtomicInteger TRIP_CONTROL_REQUEST = new AtomicInteger(3000);
	
	int editStartHeartbeatRequestId;
	int editStopHeartbeatRequestId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.start_and_stop_buttons, container, true);
		c = new Controls(view);
		this.editStartHeartbeatRequestId = TRIP_CONTROL_REQUEST.incrementAndGet();
		this.editStopHeartbeatRequestId = TRIP_CONTROL_REQUEST.incrementAndGet();
		
		c.StartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), Heartbeat.class);
				intent.putExtra(BaseColumns._ID, (Long)v.getTag());
				startActivityForResult(intent, editStartHeartbeatRequestId);
			}
		});
		
		c.StopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), Heartbeat.class);
				intent.putExtra(BaseColumns._ID, (Long)v.getTag());
				startActivityForResult(intent, editStopHeartbeatRequestId);
			}
		});
		
		return view;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			long id = data.getLongExtra(BaseColumns._ID, 0); 
			TripControlBarValue value = this.getValue();
			
			if(requestCode == this.editStartHeartbeatRequestId) {
				value.StartId = id;
			} else if (requestCode == this.editStopHeartbeatRequestId) {
				value.StopId = id;
			}
			
			this.setValue(value);
		}
	}
	
	public TripControlBarValue getValue() {
		TripControlBarValue result = new TripControlBarValue();
		result.StartId = (Long)c.StartButton.getTag();
		result.StopId = (Long)c.StopButton.getTag();
		return result;
	}
	
	public void setValue(TripControlBarValue value) {
		c.StartButton.setTag(value.StartId);
		c.StopButton.setTag(value.StopId);
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
	
	static class Controls {
		final Button StartButton;
		final Button StopButton;
		
		public Controls(View view) {
			this.StartButton = (Button)view.findViewById(R.id.bStart);
			this.StopButton = (Button)view.findViewById(R.id.bStop);
		}
	}
}
