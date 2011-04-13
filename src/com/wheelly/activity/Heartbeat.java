package com.wheelly.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;

/**
 * Complete heartbeat editing UI.
 * 
 * @author esteewhy
 */
public final class Heartbeat extends FragmentActivity {
	
	/**
	 * Construct UI and wire up events.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.heartbeat_edit);
		
		//components
		final Intent intent = this.getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final HeartbeatRepository repository = new HeartbeatRepository(new DatabaseHelper(Heartbeat.this).getReadableDatabase());
		final ContentValues values = id > 0 ? repository.load(id) : repository.getDefaults();
		
		final Controls c = new Controls(this);
		
		c.Heartbeat.setValues(values);
		
		c.SaveButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues values = c.Heartbeat.getValues();
					
					final HeartbeatRepository repository = new HeartbeatRepository(
						new DatabaseHelper(Heartbeat.this).getWritableDatabase()
					);
					
					if(id > 0) {
						repository.update(values);
					} else {
						intent.putExtra(BaseColumns._ID, repository.insert(values));
					}
					
					setResult(RESULT_OK, intent);
					finish();
				}
			});
		
		c.CancelButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						setResult(RESULT_CANCELED);
						finish();
					}
				});
	}
	
	/**
	 * Holds UI controls references.
	 */
	static class Controls {
		final HeartbeatInput Heartbeat;
		final Button SaveButton;
		final Button CancelButton;
		
		public Controls(FragmentActivity view) {
			Heartbeat		= (HeartbeatInput)view.getSupportFragmentManager().findFragmentById(R.id.heartbeat);
			SaveButton		= (Button)view.findViewById(R.id.bSave);
			CancelButton	= (Button)view.findViewById(R.id.bSaveAndNew);
		}
	}
}