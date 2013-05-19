package com.wheelly.widget;

import com.wheelly.R;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public final class FuelInput extends LinearLayout {
	
	public FuelInput(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	public FuelInput(Context context) {
		super(context);
		initialize(context);
	}
	
	Controls c;
	
	private void initialize(Context context) {
		LayoutInflater.from(context).inflate(R.layout.fuel_input, this, true);
		
		c = new Controls(this);
		
		c.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser) {
					c.editText.setText(Integer.toString(progress));
				}
			}
		});
		
		c.editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(s.length() > 0) {
					int amount = Integer.parseInt(s.toString());
					
					if(amount > c.seekBar.getMax()) {
						c.editText.setText(Integer.toString(amount = c.seekBar.getMax()));
						c.editText.setError(getResources().getText(R.string.amount_cannot_exceed_capacity));
					}
					
					c.seekBar.setProgress(amount);
				}
			}
		});
		
		c.seekBar.setMax(PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("fuel_capacity", 60));
	}
	
	public int getAmount() {
		return c.seekBar.getProgress();
	}
	
	public void setAmount(int amount) {
		c.editText.setText(Integer.toString(amount));
	}
	
	private static class Controls {
		final SeekBar seekBar;
		final EditText editText;
		
		public Controls(ViewGroup v) {
			this.seekBar = ((SeekBar)v.findViewById(R.id.amount));
			this.editText = ((EditText)v.findViewById(R.id.primary));
		}
	}
}