package com.wheelly.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;
import com.wheelly.widget.FuelInput;
import com.wheelly.widget.DateTimeBar;
import com.wheelly.widget.MileageInput;

public final class Heartbeat extends Activity {
	
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
		
		c.OdometerEditText.setAmount(values.getAsLong("odometer"));
		c.FuelAmountEditor.setAmount(values.getAsInteger("fuel"));
		c.CreatedDateTimeBar.setDateTime(values.getAsString("_created"));
		
		c.SaveButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					values.put("odometer", c.OdometerEditText.getAmount());
					values.put("fuel", c.FuelAmountEditor.getAmount());
					values.put("_created", c.CreatedDateTimeBar.getDateTime());
					
					HeartbeatRepository repository = new HeartbeatRepository(
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
	
	static class Controls {
		final MileageInput OdometerEditText;
		final FuelInput FuelAmountEditor;
		final DateTimeBar CreatedDateTimeBar;
		final Button SaveButton;
		final Button CancelButton;
		
		public Controls(Activity view) {
			OdometerEditText	= (MileageInput)view.findViewById(R.id.odometer);
			FuelAmountEditor	= (FuelInput)view.findViewById(R.id.fuel);
			CreatedDateTimeBar	= (DateTimeBar)view.findViewById(R.id.datetimebar);
			SaveButton			= (Button)view.findViewById(R.id.bSave);
			CancelButton		= (Button)view.findViewById(R.id.bSaveAndNew);
		}
	}
}