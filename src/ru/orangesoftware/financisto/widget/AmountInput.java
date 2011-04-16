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
package ru.orangesoftware.financisto.widget;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import com.wheelly.R;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class AmountInput extends Fragment {

	public static final String EXTRA_AMOUNT = "amount";
	public static final String EXTRA_CURRENCY = "currency";

	private static final AtomicInteger EDIT_AMOUNT_REQUEST = new AtomicInteger(2000);

	private int decimals;

	Controls c;
	
	private int requestId;
	private OnAmountChangedListener onAmountChangedListener;

    public static interface OnAmountChangedListener {
		void onAmountChanged(long oldAmount, long newAmount);
	}

	public void setOnAmountChangedListener(
			OnAmountChangedListener onAmountChangedListener) {
		this.onAmountChangedListener = onAmountChangedListener;
	}

	final TextWatcher textWatcher = new TextWatcher() {
		long oldAmount;

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
	
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState) {
		
		requestId = EDIT_AMOUNT_REQUEST.incrementAndGet();
		
		LinearLayout v = new LinearLayout(getActivity());
		v.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams lpWrapWrap = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lpWrapWrap.weight = 1;
		
		inflater.inflate(R.layout.amount_input, v, true);
		c = new Controls(v);
		
		c.AmountInput.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), QuickAmountInput.class);
				intent.putExtra(EXTRA_AMOUNT, getAbsAmountString());
				startActivityForResult(intent, requestId);
			}
		});
		
		c.CalculatorInput.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), CalculatorInput.class);
				intent.putExtra(EXTRA_AMOUNT, getAbsAmountString());
				startActivityForResult(intent, requestId);
			}
		});
		
		c.primary.setKeyListener(keyListener);
		c.primary.addTextChangedListener(textWatcher);
		c.primary.setOnFocusChangeListener(selectAllOnFocusListener);
		
		c.secondary.setKeyListener(new DigitsKeyListener(false, false){
			
			@Override
			public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DEL) {
					if (content.length() == 0) {
						c.primary.requestFocus();
						int pos = c.primary.getText().length(); 
						c.primary.setSelection(pos, pos);
						return true;
					}
				}
				return super.onKeyDown(view, content, keyCode, event);
			}

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_PHONE;
			}			

		});
		c.secondary.addTextChangedListener(textWatcher);
		c.secondary.setOnFocusChangeListener(selectAllOnFocusListener);
		
		return v;
	}
	
	private static final char[] acceptedChars = new char[]{'0','1','2','3','4','5','6','7','8','9'};
	private static final char[] commaChars = new char[]{'.', ','};
	
	private final NumberKeyListener keyListener = new NumberKeyListener() {
		
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			if (end - start == 1) {
				char c = source.charAt(0);
				if (c == '.' || c == ',') {
					onDotOrComma();
					return "";
				}
			}
			return super.filter(source, start, end, dest, dstart, dend);
		}

		@Override
		public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
			char c = event.getMatch(commaChars);
			if (c == '.' || c == ',') {
				onDotOrComma();
				return true;
			}
			return super.onKeyDown(view, content, keyCode, event);
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

	protected void onDotOrComma() {
		c.secondary.requestFocus();
	}

	public int getDecimals() {
		return decimals;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == requestId) {
			String amount = data.getStringExtra(EXTRA_AMOUNT);
			if (amount != null) {
				try {
					BigDecimal d = new BigDecimal(amount).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					setAmount(d.unscaledValue().longValue());
				} catch (NumberFormatException ex) {
				}
			}
		}
	}

	public void setAmount(long amount) {
		long absAmount = Math.abs(amount);
		long x = absAmount / 100;
		long y = absAmount - 100 * x;
		c.primary.setText(String.valueOf(x));
		c.secondary.setText(String.format("%02d", y));
	}

	public long getAmount() {
		String p = c.primary.getText().toString();
		String s = c.secondary.getText().toString();
		long x = 100 * toLong(p);
		long y = toLong(s);
		return x + (s.length() == 1 ? 10 * y : y);
	}

	private String getAbsAmountString() {
		String p = c.primary.getText().toString().trim();
		String s = c.secondary.getText().toString().trim();
        return (Utils.isNotEmpty(p) ? p : "0") + "."
                + (Utils.isNotEmpty(s) ? s : "0");
	}

	private long toLong(String s) {
		return s == null || s.length() == 0 ? 0 : Long.parseLong(s);
	}

	public void setColor(int color) {
		c.primary.setTextColor(color);
		c.secondary.setTextColor(color);
	}
	
	static class Controls {
		public final EditText primary;
		public final EditText secondary;
		public final View AmountInput;
		public final View CalculatorInput;
		
		public Controls(View v){
			primary = (EditText)v.findViewById(R.id.primary);
			secondary = (EditText)v.findViewById(R.id.secondary);
			AmountInput = v.findViewById(R.id.amount_input);
			CalculatorInput = v.findViewById(R.id.calculator);
		}
	}
}