package com.wheelly.activity;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;
import com.wheelly.db.IRepository;
import com.wheelly.db.RefuelController;
import com.wheelly.db.RefuelRepository;
import com.wheelly.widget.MileageInput;

import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.model.*;

/**
 * Edit single trip properties and manipulate associated heartbeats.
 */
public class Refuel extends FragmentActivity implements ActivityLayoutListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.setContentView(R.layout.refuel_edit);
		
		//components
		final Intent intent = this.getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final SQLiteDatabase db = new DatabaseHelper(Refuel.this).getReadableDatabase();
		
		final RefuelRepository repository = new RefuelRepository(db);
		final ContentValues values = id > 0 ? repository.load(id) : repository.getDefaults();
		final IRepository heartbeatRepository = new HeartbeatRepository(db);
		final ContentValues heartbeat = heartbeatRepository.getDefaults();
		
		final Controls c = new Controls(this);
		
		c.Name.setText(values.getAsString("name"));
		c.Mileage.setAmount(values.getAsLong("calc_mileage"));
		c.Heartbeat.setValues(heartbeat);
		
		c.Save.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues heartbeat = c.Heartbeat.getValues();
					values.put("calc_mileage", c.Mileage.getAmount());
					values.put("heartbeat_id", heartbeat.getAsLong(BaseColumns._ID));
					values.put("name", c.Name.getText().toString());
					
					intent.putExtra(BaseColumns._ID,
						new RefuelController(Refuel.this)
							.update(values, heartbeat)
					);
					
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
		public final HeartbeatInput Heartbeat;
		public final Button Save;
		public final Button Cancel;
		
		public Controls(FragmentActivity view) {
			Name		= (EditText)view.findViewById(R.id.payee);
			Mileage		= (MileageInput)view.findViewById(R.id.mileage);
			Heartbeat	= (HeartbeatInput)view.getSupportFragmentManager().findFragmentById(R.id.heartbeat);
			Save		= (Button)view.findViewById(R.id.bSave);
			Cancel		= (Button)view.findViewById(R.id.bSaveAndNew);
		}
	}
}