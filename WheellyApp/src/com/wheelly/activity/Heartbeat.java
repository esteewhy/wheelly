package com.wheelly.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;

/**
 * Complete heartbeat editing UI.
 */
public class Heartbeat extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.heartbeat_edit);
		
		final Intent intent = getIntent();
		
		final ContentValues heartbeat =
			intent.hasExtra("heartbeat")
				? (ContentValues)intent.getParcelableExtra("heartbeat")
				: new HeartbeatBroker(this)
					.loadOrCreate(intent.getLongExtra(BaseColumns._ID, 0));
		
		final Controls c = new Controls(this);
		c.Heartbeat.setValues(heartbeat);
		c.SaveButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues values = c.Heartbeat.getValues();
					intent.putExtra(BaseColumns._ID,
						new HeartbeatBroker(Heartbeat.this).updateOrInsert(values));
					intent.putExtra("heartbeat", values);
					
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
	private static class Controls {
		final HeartbeatInput Heartbeat;
		final View SaveButton;
		final View CancelButton;
		
		public Controls(FragmentActivity view) {
			Heartbeat		= (HeartbeatInput)view.getSupportFragmentManager().findFragmentById(R.id.heartbeat);
			SaveButton		= (View)view.findViewById(R.id.bSave);
			CancelButton	= (View)view.findViewById(R.id.bSaveAndNew);
		}
	}
}