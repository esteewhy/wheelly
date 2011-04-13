package com.wheelly.widget;

import com.wheelly.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public final class FuelInput extends LinearLayout {
	
	SeekBar seekBar;
	EditText editText;
	
	public FuelInput(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	public FuelInput(Context context) {
		super(context);
		initialize(context);
	}

	void initialize(Context context) {
		LayoutInflater.from(context).inflate(R.layout.select_entry_amount, this, true);
		
		this.seekBar = ((SeekBar)findViewById(R.id.amount));
		this.editText = ((EditText)findViewById(R.id.primary));
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser) {
					editText.setText(Integer.toString(progress));
				}
			}
		});
		
		editText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(s.length() > 0) {
					seekBar.setProgress(Integer.parseInt(s.toString()));
				}
			}
		});
	}

	
	public int getAmount() {
		return this.seekBar.getProgress();
	}
	

	public void setAmount(int amount) {
		this.editText.setText(Integer.toString(amount));
	}
}
