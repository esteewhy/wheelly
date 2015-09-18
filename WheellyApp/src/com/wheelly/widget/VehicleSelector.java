package com.wheelly.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

public class VehicleSelector extends Spinner {
	public VehicleSelector(Context context) {
		super(context);
		initialize();
	}
	
	public VehicleSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}
	
	public VehicleSelector(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	void initialize() {
        setLongClickable(true);
        
        setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Spinner s = (Spinner)parent;
				if(position == s.getCount() - 1) {
					editItem(new TextWatcher() {
						@Override
						public void beforeTextChanged(
								CharSequence paramCharSequence, int paramInt1,
								int paramInt2, int paramInt3) {
						}

						@Override
						public void onTextChanged(
								CharSequence paramCharSequence, int paramInt1,
								int paramInt2, int paramInt3) {
						}
						
						@Override
						public void afterTextChanged(Editable paramEditable) {
							
						}
					}, s.getSelectedItem().toString());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> paramAdapterView) {
				// TODO Auto-generated method stub
				
			}
		});
        
        setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				Spinner s = (Spinner)view;
				if(s.getSelectedItemPosition() != s.getCount() - 1) {
					editItem(new TextWatcher() {
						@Override
						public void onTextChanged(CharSequence paramCharSequence, int paramInt1,
								int paramInt2, int paramInt3) {
						}
						
						@Override
						public void beforeTextChanged(CharSequence paramCharSequence,
								int paramInt1, int paramInt2, int paramInt3) {
						}
						
						@Override
						public void afterTextChanged(Editable editable) {
							// TODO Auto-generated method stub
							
						}
					}, s.getSelectedItem().toString());
					return true;
				}
				return false;
			}
		});
	}
	
	void editItem(final TextWatcher listener, String text) {
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

		alert.setTitle("Title");
		alert.setMessage("Message");

		// Set an EditText view to get user input 
		final EditText input = new EditText(getContext());
		input.setText(text);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
				listener.afterTextChanged(input.getText());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		
		alert.show();
	}
}