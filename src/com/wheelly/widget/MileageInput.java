/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.wheelly.widget;

import java.util.concurrent.atomic.AtomicInteger;

import android.widget.*;
import com.wheelly.R;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.CalculatorInput;
import ru.orangesoftware.financisto.widget.MultiNumberPicker;
import ru.orangesoftware.financisto.widget.MultiNumberPicker.OnChangedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class MileageInput extends LinearLayout {

	private static final AtomicInteger EDIT_AMOUNT_REQUEST = new AtomicInteger(2000);

	private EditText primary;
	
	private int requestId;
	private OnAmountChangedListener onAmountChangedListener;

	public MileageInput(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public MileageInput(Context context) {
		super(context);
		initialize(context, null);
	}
	
    public static interface OnAmountChangedListener {
		void onAmountChanged(long oldAmount, long newAmount);
	}


	public void setOnAmountChangedListener(
			OnAmountChangedListener onAmountChangedListener) {
		this.onAmountChangedListener = onAmountChangedListener;
	}

	private final TextWatcher textWatcher = new TextWatcher() {
		private long oldAmount;

		@Override
		public void afterTextChanged(Editable s) {
			if (onAmountChangedListener != null) {
				long amount = getAmount();
				onAmountChangedListener.onAmountChanged(oldAmount, amount);
				oldAmount = amount;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			oldAmount = getAmount();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	};

	private void initialize(Context context, AttributeSet attrs) {
		requestId = EDIT_AMOUNT_REQUEST.incrementAndGet();
		LayoutInflater.from(context).inflate(R.layout.mileage_input, this, true);
		
		findViewById(R.id.amount_input).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Context ctx = getContext();
				long amount = getAmount(); 
				
				LinearLayout layout = new LinearLayout(ctx);
				layout.setOrientation(LinearLayout.VERTICAL);
				LinearLayout.LayoutParams lpWrapWrap = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lpWrapWrap.weight = 1;
				
				final Dialog d = new AlertDialog.Builder(ctx)
					.setTitle(Long.toString(amount))
					.setView(layout)
					.create();
				
				// picker
				final MultiNumberPicker picker = new MultiNumberPicker(ctx);
				layout.addView(picker, lpWrapWrap);
				if (amount != 0) {
					picker.setCurrent((int)amount);
				}
				picker.setOnChangeListener(new OnChangedListener(){
					@Override
					public void onChanged(MultiNumberPicker picker, int oldVal, int newVal) {
						d.setTitle(Integer.toString(picker.getCurrent()));
					}
				});
				
				// buttons
				LinearLayout layout2 = new LinearLayout(ctx);
				layout2.setOrientation(LinearLayout.HORIZONTAL);
				Button bOK = new Button(ctx);
				bOK.setText(R.string.ok);
				bOK.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						setAmount(picker.getCurrent());
						d.dismiss();
					}
				});
				layout2.addView(bOK, lpWrapWrap);
				Button bClear = new Button(ctx);
				bClear.setText(R.string.clear);
				bClear.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						picker.setCurrent(0);
					}
				});
				layout2.addView(bClear, lpWrapWrap);
				Button bCancel = new Button(ctx);
				bCancel.setText(R.string.cancel);
				bCancel.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						d.dismiss();
					}
				});
				layout2.addView(bCancel, lpWrapWrap);
				layout.addView(layout2, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				
				d.setCanceledOnTouchOutside(true);
				d.show();
			}
		});
		
		findViewById(R.id.calculator).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getContext(), CalculatorInput.class);
				intent.putExtra(AmountInput.EXTRA_AMOUNT, getAmount());
				((Activity)getContext()).startActivityForResult(intent, requestId);	
			}
		});
		
		primary = (EditText) findViewById(R.id.primary);
		primary.setKeyListener(keyListener);
		primary.addTextChangedListener(textWatcher);
		primary.setOnFocusChangeListener(selectAllOnFocusListener);
	}
	
	private static final char[] acceptedChars = new char[]{'0','1','2','3','4','5','6','7','8','9'};
	
	private final NumberKeyListener keyListener = new NumberKeyListener() {
		
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			return super.filter(source, start, end, dest, dstart, dend);
		}
		
		@Override
		protected char[] getAcceptedChars() {
			return acceptedChars;
		}

		@Override
		public int getInputType() {
			return InputType.TYPE_CLASS_PHONE;
		}
	};
	
	private final View.OnFocusChangeListener selectAllOnFocusListener = new View.OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			EditText t = (EditText) v;
			if (hasFocus) {
				t.selectAll();
			}
		}
	};

	public boolean processActivityResult(int requestCode, Intent data) {
		if (requestCode == requestId) {
			int amount = data.getIntExtra(AmountInput.EXTRA_AMOUNT, 0);
			setAmount(amount);
			return true;
		}
		return false;
	}

	public void setAmount(long amount) {
		primary.setText(String.valueOf(amount));
	}

	public long getAmount() {
		return toLong(primary.getText().toString());
	}

	private long toLong(String s) {
		return s == null || s.length() == 0 ? 0 : Long.parseLong(s);
	}

	public void setColor(int color) {
		primary.setTextColor(color);
	}
}