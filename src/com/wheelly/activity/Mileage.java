package com.wheelly.activity;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import com.wheelly.R;
import com.wheelly.app.TripControlBar;
import com.wheelly.app.TripControlBarValue;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.MileageRepository;
import com.wheelly.db.MileageBroker;
import com.wheelly.widget.MileageInput;

import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.model.*;

/**
 * Edit single trip properties and manipulate associated heartbeats.
 */
public class Mileage extends FragmentActivity implements ActivityLayoutListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.setContentView(R.layout.mileage_edit);
		
		//components
		final Intent intent = this.getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final MileageRepository repository = new MileageRepository(new DatabaseHelper(Mileage.this).getReadableDatabase());
		final ContentValues values = id > 0 ? repository.load(id) : repository.getDefaults();
		
		final Controls c = new Controls(this);
		
		c.Name.setText(values.getAsString("name"));
		c.Mileage.setAmount(values.getAsLong("mileage"));
		final TripControlBarValue heartbeats = new TripControlBarValue();
		heartbeats.StartId = values.getAsLong("start_heartbeat_id");
		heartbeats.StopId = values.getAsLong("stop_heartbeat_id");
		c.Heartbeats.setValue(heartbeats);
		
		c.Save.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					values.put("mileage", c.Mileage.getAmount());
					final TripControlBarValue heartbeats = c.Heartbeats.getValue();
					values.put("start_heartbeat_id", heartbeats.StartId);
					values.put("stop_heartbeat_id", heartbeats.StopId);
					values.put("name", c.Name.getText().toString());
					
					intent.putExtra(BaseColumns._ID, new MileageBroker(Mileage.this).update(values));
					
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
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		/*if (hasFocus) {
			accountText.requestFocusFromTouch();
		}*/
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelected(int id, ArrayList<? extends MultiChoiceItem> items) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	static class Controls {
		public final EditText Name;
		public final MileageInput Mileage;
		public final TripControlBar Heartbeats; 
		public final Button Save;
		public final Button Cancel;
		
		public Controls(FragmentActivity view) {
			Name		= (EditText)view.findViewById(R.id.payee);
			Mileage		= (MileageInput)view.findViewById(R.id.mileage);
			Heartbeats	= (TripControlBar)view.getSupportFragmentManager().findFragmentById(R.id.heartbeats);
			Save		= (Button)view.findViewById(R.id.bSave);
			Cancel		= (Button)view.findViewById(R.id.bSaveAndNew);
		}
	}
}