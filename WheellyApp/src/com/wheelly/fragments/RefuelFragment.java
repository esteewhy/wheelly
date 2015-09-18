package com.wheelly.fragments;

import android.content.ContentValues;
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
		return inflater.inflate(R.layout.refuel_edit, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Bundle args = getArgumentsOrDefault();
		final long id = args.getLong(BaseColumns._ID, -1);
		final RefuelBroker broker = new RefuelBroker(getActivity());
		final ContentValues refuel = broker.loadOrCreate(id);
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
					refuel.putAll(heartbeat);
					refuel.put("amount", (float)c.Amount.getAmount() / 100);
					refuel.put("unit_price", (float)c.Price.getAmount() / 100);
					refuel.put("cost", (float)c.Cost.getAmount() / 100);
					refuel.put("transaction_id", c.Financisto.getValue());
					
					args.putLong(BaseColumns._ID,
						broker.updateOrInsert(refuel)
					);
					finish(args);
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