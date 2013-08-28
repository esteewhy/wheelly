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
package com.wheelly.fragments;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.google.api.client.util.Strings;
import com.wheelly.R;
import com.wheelly.app.LocationViewBinder;
import com.wheelly.db.LocationBroker;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.F;

import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.view.NodeInflater;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FilterDialog extends DialogFragment {	
	private LayoutInflater layoutInflater;
	private OnFilterChangedListener listener;
	private ContentValues filter;
	
	private DateFormat df;
	private static final int PERIOD_REQUEST = 1;
	
	private String filterValueNotFound;
	private ActivityLayout x;
	private Controls c;
	
	public FilterDialog(ContentValues filter, OnFilterChangedListener listener) {
		this.filter = filter;
		this.listener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View v = layoutInflater.inflate(R.layout.blotter_filter, null);
		
		final SimpleCursorAdapter adapter =
			new SimpleCursorAdapter(getActivity(),
				R.layout.location_item,
				null, 
				new String[] {"name", "resolved_address", "name" },
				new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 },
				0
			);
		
		adapter.setViewBinder(new LocationViewBinder(null));
		
		x = new ActivityLayout(new NodeInflater(layoutInflater),
			new ActivityLayoutListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
					case R.id.period:
						Intent intent = new Intent(getActivity(), DateFilterActivity.class);
						FilterUtils.filterToIntent(filter, intent);
						startActivityForResult(intent, PERIOD_REQUEST);
						break;
					case R.id.period_clear:
						filter.remove(F.PERIOD);
						c.period.setText(R.string.no_filter);
						break;
					case R.id.location: {
						long locationId = filter.containsKey(F.LOCATION) ? filter.getAsLong(F.LOCATION) : -1;
						final long selectedId = c != null ? locationId : -1;
						final String entityType = filter.getAsString(F.LOCATION_CONSTRAINT);
						
						getActivity().getSupportLoaderManager().initLoader(
							Strings.isNullOrEmpty(entityType) ? 0 : entityType.hashCode(), null,
							new LoaderCallbacks<Cursor>() {
								@Override
								public Loader<Cursor> onCreateLoader(int arg0,
										Bundle arg1) {
									return
										new CursorLoader(getActivity(),
											Uri.withAppendedPath(Locations.CONTENT_URI, entityType),
											null, null, null, null);
								}
								
								@Override
								public void onLoadFinished(Loader<Cursor> loader,
										Cursor locationCursor) {
									adapter.changeCursor(locationCursor);
									
									x.select(getActivity(),
											R.id.location,
											R.string.location,
											locationCursor,
											adapter,
											BaseColumns._ID,
											selectedId);
								}
								
								@Override
								public void onLoaderReset(Loader<Cursor> arg0) { }
							});
					} break;
					case R.id.location_clear:
						filter.remove(F.LOCATION);
						c.location.setText(R.string.no_filter);
						break;
					}
				}
				
				@Override
				public void onSelectedId(int id, long selectedId) {
					switch (id) {
					case R.id.location:
						filter.put(F.LOCATION, selectedId);
						updateLocationFromFilter(filter);
						break;
					}
				}
				
				@Override
				public void onSelected(int arg0,
						List<? extends MultiChoiceItem> arg1) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onSelectedPos(int arg0, int arg1) {
					// TODO Auto-generated method stub
				}
		});
		
		c = new Controls(x, v);
		
		df = DateUtils.getShortDateFormat(getActivity());
		filterValueNotFound = getString(R.string.filter_value_not_found);

		final Dialog d = new AlertDialog.Builder(getActivity())
			.setView(v)
			.setCancelable(true)
			.create();
		
		c.bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dismiss();
				listener.onFilterChanged(filter);
			}
		});
		
		c.bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				d.cancel();
			}
		});
		
		c.bNoFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dismiss();
				listener.onFilterChanged(null);
			}
		});		
		
		if(null != filter) {
			updatePeriodFromFilter(filter);
			updateLocationFromFilter(filter);
		}
		
		return d;
	}
	
	private void updateLocationFromFilter(ContentValues filter) {
		long locationId = filter.containsKey(F.LOCATION)
			? filter.getAsLong(F.LOCATION)
			: 0;
		
		if (locationId > 0) {
			final ContentValues location = new LocationBroker(getActivity()).loadOrCreate(locationId);
			c.location.setText(location != null ? location.getAsString("name") : filterValueNotFound);
		} else {
			c.location.setText(R.string.no_filter);
		}
	}

	private void updatePeriodFromFilter(ContentValues filter) {
		String s = filter.getAsString(F.PERIOD);
		if (s != null) {
			String[] tokens = s.split(",");
			PeriodType type = PeriodType.valueOf(tokens[0]);
			if (type == PeriodType.CUSTOM) {
				long periodFrom = Long.valueOf(tokens[1]);
				long periodTo = Long.valueOf(tokens[2]);
				c.period.setText(df.format(new Date(periodFrom)) + "-" + df.format(new Date(periodTo)));
			} else {
				c.period.setText(type.titleId);
			}
		} else {
			c.period.setText(R.string.no_filter);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PERIOD_REQUEST) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				filter.remove(F.PERIOD);
				c.period.setText(R.string.no_filter);
			} else if (resultCode == Activity.RESULT_OK) {
				filter.put(F.PERIOD, data.getStringExtra(F.PERIOD));
				updatePeriodFromFilter(filter);
			}
		}
	}
	
	private static class Controls {
		final TextView period;
		final TextView location;
		
		final Button bOk;
		final Button bCancel;
		final ImageButton bNoFilter;
		
		public Controls(ActivityLayout x, View v) {
			LinearLayout layout = (LinearLayout)v.findViewById(R.id.layout);
			period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
			location = x.addFilterNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
			
			bOk = (Button)v.findViewById(R.id.bOK);
			bCancel = (Button)v.findViewById(R.id.bCancel);
			bNoFilter = (ImageButton)v.findViewById(R.id.bNoFilter);
		}
	}
	
	private OnCancelListener cancelListener;
	
	public void setOnCancelListener(OnCancelListener listener) {
		this.cancelListener = listener;
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		
		if(null != this.cancelListener) {
			this.cancelListener.onCancel(dialog);
		}
	}
	
	public static interface OnFilterChangedListener {
		void onFilterChanged(ContentValues value);
	}
}