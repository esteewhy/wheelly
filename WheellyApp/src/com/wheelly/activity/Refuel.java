package com.wheelly.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.RefuelBroker;
import com.wheelly.widget.FinancistoButton;

import ru.orangesoftware.financisto.widget.AmountInput;

/**
 * Edit refuel properties and manipulate associated heartbeats.
 */
public class Refuel extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.refuel_edit);
		
		//components
		final Intent intent = this.getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final ContentValues refuel = new RefuelBroker(this).loadOrCreate(id);
		final Controls c = new Controls();
		
		c.Heartbeat.setValues(refuel);
		c.Amount.setAmount((long)Math.round(refuel.getAsFloat("amount") * 100));
		c.Price.setAmount((long)Math.round(refuel.getAsFloat("unit_price") * 100));
		c.Cost.setAmount((long)Math.round(refuel.getAsFloat("cost") * 100));
		c.Financisto.setValue((long)refuel.getAsLong("transaction_id"));
		c.Financisto.setValues(refuel);
		
		c.Save.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues heartbeat = c.Heartbeat.getValues();
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

	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final AmountInput Amount;
		final AmountInput Price;
		final AmountInput Cost;
		final View Save;
		final View Cancel;
		final FinancistoButton Financisto;
		
		public Controls() {
			final FragmentManager fm = getSupportFragmentManager();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			Amount		= (AmountInput)fm.findFragmentById(R.id.amount);
			Price		= (AmountInput)fm.findFragmentById(R.id.price);
			Cost		= (AmountInput)fm.findFragmentById(R.id.cost);
			Save		= (View)findViewById(R.id.bSave);
			Cancel		= (View)findViewById(R.id.bSaveAndNew);
			Financisto	= (FinancistoButton)fm.findFragmentById(R.id.financisto);
		}
	}
}