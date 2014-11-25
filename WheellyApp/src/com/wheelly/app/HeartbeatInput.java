package com.wheelly.app;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.issue40537.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wheelly.R;
import com.wheelly.widget.FuelInput;
import com.wheelly.widget.DateTimeBar;
import com.wheelly.widget.MileageInput;

/**
 * Reusable heartbeat editing control.
 */
public final class HeartbeatInput extends Fragment {
	
	private Controls c;
	private ContentValues values;
	
	/**
	 * Initialize UI.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.heartbeat_edit_fragment, container);
		this.c = new Controls(view);
		return view;
	}
	
	/**
	 * Gather values from controls.
	 * 
	 * @return original ContentValues, plus updates from controls. 
	 */
	public ContentValues getValues() {
		// Accounts for the case when this gets called before setValues(..)
		if(values == null) {
			values = new ContentValues();
		}
		
		values.put("odometer", c.OdometerEditText.getAmount());
		values.put("fuel", c.FuelAmountEditor.getAmount());
		values.put("_created", c.CreatedDateTimeBar.getDateTime());
		values.put("place_id", c.Place.getValue());
		
		return values;
	}
	
	/**
	 * Assign values to controls. Preserves original values.
	 */
	public void setValues(ContentValues values) {
		this.values = values;
		c.OdometerEditText.setAmount(values.getAsLong("odometer"));
		c.FuelAmountEditor.setAmount(values.getAsInteger("fuel"));
		c.CreatedDateTimeBar.setDateTime(values.getAsString("_created"));
		c.Place.setValue(values.getAsLong("place_id"));
	}
	
	/**
	 * Holds control references.
	 */
	private class Controls {
		final MileageInput OdometerEditText;
		final FuelInput FuelAmountEditor;
		final DateTimeBar CreatedDateTimeBar;
		final LocationInput Place;
		
		public Controls(View view) {
			OdometerEditText	= (MileageInput)view.findViewById(R.id.odometer);
			FuelAmountEditor	= (FuelInput)view.findViewById(R.id.fuel);
			CreatedDateTimeBar	= (DateTimeBar)view.findViewById(R.id.datetimebar);
			Place				= (LocationInput)getChildFragmentManager().findFragmentById(R.id.place);
		}
	}
}