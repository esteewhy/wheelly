package com.wheelly.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.RefuelBroker;
import com.wheelly.widget.FinancistoButton;

import ru.orangesoftware.financisto.widget.AmountInput;

public class RefuelFragment extends ItemFragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.refuel_edit, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Intent intent = getActivity().getIntent();
		final long id = intent.getLongExtra(BaseColumns._ID, 0);
		final ContentValues refuel = new RefuelBroker(getActivity()).loadOrCreate(id);
		final Controls c = new Controls();
		
		c.Heartbeat.setValues(refuel);
		c.Amount.setAmount((long)Math.round(refuel.getAsFloat("amount") * 100));
		c.Price.setAmount((long)Math.round(refuel.getAsFloat("unit_price") * 100));
		c.Cost.setAmount((long)Math.round(refuel.getAsFloat("cost") * 100));
		c.Financisto.setValue((long)refuel.getAsLong("transaction_id"));
		c.Financisto.setValues(refuel);
		
		onSave =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues heartbeat = c.Heartbeat.getValues();
					refuel.put("amount", (float)c.Amount.getAmount() / 100);
					refuel.put("unit_price", (float)c.Price.getAmount() / 100);
					refuel.put("cost", (float)c.Cost.getAmount() / 100);
					refuel.put("transaction_id", c.Financisto.getValue());
					
					intent.putExtra(BaseColumns._ID,
						new RefuelBroker(getActivity())
							.updateOrInsert(refuel, heartbeat)
					);
					
					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			};
	}
	
	/**
	 * Encapsulates UI objects.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final AmountInput Amount;
		final AmountInput Price;
		final AmountInput Cost;
		final FinancistoButton Financisto;
		
		public Controls() {
			final FragmentManager fm = getChildFragmentManager();
			
			Heartbeat	= (HeartbeatInput)fm.findFragmentById(R.id.heartbeat);
			Amount		= (AmountInput)fm.findFragmentById(R.id.amount);
			Price		= (AmountInput)fm.findFragmentById(R.id.price);
			Cost		= (AmountInput)fm.findFragmentById(R.id.cost);
			Financisto	= (FinancistoButton)fm.findFragmentById(R.id.financisto);
		}
	}
}