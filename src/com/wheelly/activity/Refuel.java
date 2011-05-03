package com.wheelly.activity;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.HeartbeatRepository;
import com.wheelly.db.IRepository;
import com.wheelly.db.RefuelBroker;
import com.wheelly.db.RefuelRepository;
import com.wheelly.widget.FinancistoSync;

import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.widget.AmountInput;

/**
 * Edit refuel properties and manipulate associated heartbeats.
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
		
		final IRepository repository = new RefuelRepository(db);
		final ContentValues refuel = id > 0 ? repository.load(id) : repository.getDefaults();
		final IRepository heartbeatRepository = new HeartbeatRepository(db);
		final long heartbeatId = refuel.getAsLong("heartbeat_id");
		final ContentValues heartbeat = heartbeatId > 0 ? heartbeatRepository.load(heartbeatId) : heartbeatRepository.getDefaults();
		
		final Controls c = new Controls(this);
		
		c.Heartbeat.setValues(heartbeat);
		c.Amount.setAmount((long)Math.round(refuel.getAsFloat("amount") * 100));
		c.Price.setAmount((long)Math.round(refuel.getAsFloat("unit_price") * 100));
		c.Cost.setAmount((long)Math.round(refuel.getAsFloat("cost") * 100));
		c.Financisto.setValue((long)refuel.getAsLong("transaction_id"));
		
		c.Save.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues heartbeat = c.Heartbeat.getValues();
					refuel.put("heartbeat_id", heartbeat.getAsLong(BaseColumns._ID));
					refuel.put("amount", (float)c.Amount.getAmount() / 100);
					refuel.put("unit_price", (float)c.Price.getAmount() / 100);
					refuel.put("cost", (float)c.Cost.getAmount() / 100);
					refuel.put("transaction_id", c.Financisto.getValue());
					
					intent.putExtra(BaseColumns._ID,
						new RefuelBroker(Refuel.this)
							.updateOrInsert(refuel, heartbeat)
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
	private static class Controls {
		final HeartbeatInput Heartbeat;
		final AmountInput Amount;
		final AmountInput Price;
		final AmountInput Cost;
		final Button Save;
		final Button Cancel;
		final FinancistoSync Financisto;
		
		public Controls(FragmentActivity view) {
			final FragmentManager fm = view.getSupportFragmentManager();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			Amount		= (AmountInput)fm.findFragmentById(R.id.amount);
			Price		= (AmountInput)fm.findFragmentById(R.id.price);
			Cost		= (AmountInput)fm.findFragmentById(R.id.cost);
			Save		= (Button)view.findViewById(R.id.bSave);
			Cancel		= (Button)view.findViewById(R.id.bSaveAndNew);
			Financisto	= (FinancistoSync)fm.findFragmentById(R.id.financisto);
		}
	}
}