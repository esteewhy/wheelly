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
package ru.orangesoftware.financisto.activity;

import java.text.DateFormat;
import java.util.Calendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

import static ru.orangesoftware.financisto.datetime.DateUtils.is24HourFormat;
import static ru.orangesoftware.financisto.utils.EnumUtils.createSpinnerAdapter;

public class DateFilterActivity extends Activity {
	
	public static final String EXTRA_FILTER_PERIOD_TYPE = "filter_period_type";
	public static final String EXTRA_FILTER_PERIOD_FROM = "filter_period_from";
	public static final String EXTRA_FILTER_PERIOD_TO = "filter_period_to";
	public static final String EXTRA_FILTER_DONT_SHOW_NO_FILTER = "filter_dont_show_no_filter";
	
	private final Calendar cFrom = Calendar.getInstance(); 
	private final Calendar cTo = Calendar.getInstance();
	
	private Spinner spinnerPeriodType;
	private Button buttonPeriodFrom;
	private Button buttonPeriodTo;
	
	private DateFormat df;
	private PeriodType[] periods = PeriodType.allRegular();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.date_filter);

		df = DateUtils.getShortDateFormat(this);
		
		Intent intent = getIntent();
		createPeriodsSpinner();
		
		buttonPeriodFrom = (Button)findViewById(R.id.bPeriodFrom);
		buttonPeriodFrom.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				showDialog(1);
			}
		});		
		buttonPeriodTo = (Button)findViewById(R.id.bPeriodTo);	
		buttonPeriodTo.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				showDialog(2);
			}
		});
		
		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				PeriodType period = periods[spinnerPeriodType.getSelectedItemPosition()];
				data.putExtra("period",
						period.name()
						+ ","
						+ Long.toString(cFrom.getTimeInMillis())
						+ ","
						+ Long.toString(cTo.getTimeInMillis())
				);
				setResult(RESULT_OK, data);
				finish();
			}
		});
		
		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		Button bNoFilter = (Button)findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_FIRST_USER);
				finish();
			}
		});		

		if (intent == null || !intent.hasExtra("period")) {
			reset();
		} else {
			String[] parts = intent.getStringExtra("period").split(",");
			PeriodType type = PeriodType.valueOf(parts[0]);
			if (type == PeriodType.CUSTOM) {
				selectPeriod(Long.valueOf(parts[1]), Long.valueOf(parts[2]));
			} else {
				spinnerPeriodType.setSelection(indexOf(type));
			}
			
			if (intent.getBooleanExtra(EXTRA_FILTER_DONT_SHOW_NO_FILTER, false)) {
				bNoFilter.setVisibility(View.GONE);
			}
		}		
	}
	
    private void createPeriodsSpinner() {
		spinnerPeriodType = (Spinner)findViewById(R.id.period);
        final PeriodType[] periods = PeriodType.values();
        spinnerPeriodType.setAdapter(createSpinnerAdapter(this, periods));
		spinnerPeriodType.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PeriodType period = periods[position];
                if (period == PeriodType.CUSTOM) {
                    selectCustom();
                } else {
                    selectPeriod(period);
                }
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}			
		});		
    }

	
	private void selectPeriod(Period p) {
		spinnerPeriodType.setSelection(indexOf(p.type));
	}

	private void selectPeriod(long from, long to) {
		cFrom.setTimeInMillis(from);
		cTo.setTimeInMillis(to);			
		spinnerPeriodType.setSelection(PeriodType.CUSTOM.ordinal());
	}
	
    private int indexOf(PeriodType type) {
        for (int i = 0; i < periods.length; i++) {
            if (periods[i] == type) {
                return i;
            }
        }
        return 0;
    }
	
	@Override
	protected Dialog onCreateDialog(final int id) {
		final Dialog d = new Dialog(this);
		d.setCancelable(true);
		d.setTitle(id == 1 ? R.string.period_from : R.string.period_to);
		d.setContentView(R.layout.filter_period_select);
		Button bOk = (Button)d.findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setDialogResult(d, id == 1 ? cFrom : cTo);
				d.dismiss();
			}
		});		
		Button bCancel = (Button)d.findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				d.cancel();
			}
		});
		return d;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		prepareDialog(dialog, id == 1 ? cFrom : cTo);
	}

	private void prepareDialog(Dialog dialog, Calendar c) {
		DatePicker dp = (DatePicker)dialog.findViewById(R.id.date);
		dp.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
		TimePicker tp = (TimePicker)dialog.findViewById(R.id.time);
		tp.setIs24HourView(is24HourFormat(this));
        tp.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
		tp.setCurrentMinute(c.get(Calendar.MINUTE));
	}

	private void setDialogResult(Dialog d, Calendar c) {
		DatePicker dp = (DatePicker)d.findViewById(R.id.date);
		c.set(Calendar.YEAR, dp.getYear());
		c.set(Calendar.MONTH, dp.getMonth());
		c.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
		TimePicker tp = (TimePicker)d.findViewById(R.id.time);
		c.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
		c.set(Calendar.MINUTE, tp.getCurrentMinute());
		updateDate();
	}

	private void enableButtons() {
		buttonPeriodFrom.setEnabled(true);
		buttonPeriodTo.setEnabled(true);
	}

	private void disableButtons() {
		buttonPeriodFrom.setEnabled(false);
		buttonPeriodTo.setEnabled(false);
	}

	private void updateDate(Period p) {
		cFrom.setTimeInMillis(p.start);
		cTo.setTimeInMillis(p.end);
		updateDate();
	}
	
	private void updateDate() {
		buttonPeriodFrom.setText(df.format(cFrom.getTime()));
		buttonPeriodTo.setText(df.format(cTo.getTime()));
	}

    private void selectPeriod(PeriodType periodType) {
        disableButtons();
        updateDate(periodType.calculatePeriod());
    }

	protected void selectCustom() {
		updateDate();
		enableButtons();
	}

	private void reset() {
		spinnerPeriodType.setSelection(0);
	}
}