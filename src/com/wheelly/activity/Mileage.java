package com.wheelly.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.FloatMath;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import com.wheelly.R;
import com.wheelly.app.LocationInput;
import com.wheelly.app.TrackInput;
import com.wheelly.app.TrackInput.OnTrackChangedListener;
import com.wheelly.app.TripControlBar;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.MileageBroker;
import com.wheelly.widget.MileageInput;
import com.wheelly.content.TrackRepository;

/**
 * Edit single trip properties and manipulate associated heartbeats.
 */
public class Mileage extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.mileage_edit);
		
		//components
		final Intent intent = this.getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final ContentValues values = new MileageBroker(this).loadOrCreate(id);
		
		final Controls c = new Controls(this);
		
		c.Mileage.setAmount(values.getAsLong("mileage"));
		c.Destination.setValue(values.getAsLong("location_id"));
		c.Track.setOnTrackChangedListener(new OnTrackChangedListener() {
			@Override
			public void onTrackChanged(long trackId) {
				// @todo Compare to default
				if(c.Mileage.getAmount() == 0) {
					c.Mileage.setAmount(trackId > 0
						? (long)FloatMath.ceil(new TrackRepository(Mileage.this).getDistance(trackId))
						: 0);
				}
			}
		});
		c.Track.setValue(values.getAsLong("track_id"));
		
		final TripControlBar.Value heartbeats = new TripControlBar.Value();
		heartbeats.TrackId = values.getAsLong("track_id");
		
		HeartbeatBroker broker = new HeartbeatBroker(this);
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
					
					if(null != heartbeats.StartHeartbeat
							&& heartbeats.StartHeartbeat.containsKey(BaseColumns._ID)) {
						values.put("start_heartbeat_id",	heartbeats.StartHeartbeat.getAsLong(BaseColumns._ID));
					}
					
					if(null != heartbeats.StopHeartbeat
							&& heartbeats.StopHeartbeat.containsKey(BaseColumns._ID)) {
						values.put("stop_heartbeat_id",	heartbeats.StopHeartbeat.getAsLong(BaseColumns._ID));
					}
					
					final long trackId = c.Track.getValue();
					values.put("track_id", trackId > 0 ? trackId : heartbeats.TrackId);
					
					intent.putExtra(BaseColumns._ID, new MileageBroker(Mileage.this).updateOrInsert(values));
					
					setResult(RESULT_OK, intent);
					finish();
				}
			});
		
		c.Cancel.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						setResult(RESULT_CANCELED);
						finish();
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
	private static class Controls {
		final MileageInput Mileage;
		final LocationInput Destination;
		final TrackInput Track;
		final TripControlBar Heartbeats; 
		final Button Save;
		final Button Cancel;
		
		public Controls(FragmentActivity view) {
			final FragmentManager fm = view.getSupportFragmentManager();
			
			Mileage		= (MileageInput)view.findViewById(R.id.mileage);
			Destination	= (LocationInput)fm.findFragmentById(R.id.place);
			Track		= (TrackInput)fm.findFragmentById(R.id.track);
			Heartbeats	= (TripControlBar)fm.findFragmentById(R.id.heartbeats);
			Save		= (Button)view.findViewById(R.id.bSave);
			Cancel		= (Button)view.findViewById(R.id.bSaveAndNew);
		}
	}
}