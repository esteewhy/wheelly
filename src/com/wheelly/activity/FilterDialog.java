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
package com.wheelly.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.wheelly.R;
import com.wheelly.app.FilterButton.OnFilterChangedListener;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationRepository;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.F;

import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.utils.DateUtils.PeriodType;
import ru.orangesoftware.financisto.view.NodeInflater;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
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
	private SQLiteDatabase db;
	
	private Cursor locationCursor;
	
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
		final Activity ctx = this.getActivity();
		
		this.locationCursor = new LocationRepository(
			db = new DatabaseHelper(ctx).getReadableDatabase())
				.list(filter.containsKey(F.LOCATION_CONSTRAINT)
					? filter.getAsString(F.LOCATION_CONSTRAINT)
					: "");
		
		ctx.startManagingCursor(locationCursor);
		
		final ListAdapter adapter =
			new SimpleCursorAdapter(ctx,
					android.R.layout.simple_spinner_dropdown_item,
					locationCursor, 
					new String[] {"name"},
					new int[] { android.R.id.text1 }
			);
		
		x = new ActivityLayout(new NodeInflater(layoutInflater),
			new ActivityLayoutListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
					case R.id.period:
						Intent intent = new Intent(ctx, DateFilterActivity.class);
						FilterUtils.filterToIntent(filter, intent);
						startActivityForResult(intent, PERIOD_REQUEST);
						break;
					case R.id.period_clear:
						filter.remove(F.PERIOD);
						c.period.setText(R.string.no_filter);
						break;
					case R.id.location: {
						long locationId = filter.containsKey(F.LOCATION) ? filter.getAsLong(F.LOCATION) : -1;
						long selectedId = c != null ? locationId : -1;
						
						x.select(ctx,
							R.id.location,
							R.string.location,
							locationCursor,
							adapter,
							BaseColumns._ID,
							selectedId);
					} break;
					case R.id.location_clear:
						filter.remove(F.LOCATION);
						c.location.setText(R.string.no_filter);
						break;
					case R.id.sort_order: {
						
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							ctx,
							android.R.layout.simple_spinner_dropdown_item,
							c.sortOrders);
						
						int selectedId = Math.min(1, filter.containsKey(F.SORT_ORDER) ? filter.getAsInteger(F.SORT_ORDER) : 0);
						x.selectPosition(
							ctx,
							R.id.sort_order,
							R.string.sort_order,
							adapter,
							selectedId);
					} break;
					case R.id.sort_order_clear:
						filter.remove(F.SORT_ORDER);
						updateSortOrderFromFilter(filter);
						break;
					}
				}
				
				@Override
				public void onSelectedPos(int id, int selectedPos) {
					switch (id) {
					case R.id.sort_order:
						if(Math.min(selectedPos, 1) > 0) {
							filter.put(F.SORT_ORDER, 1);
						} else {
							filter.remove(F.SORT_ORDER);
						}
						updateSortOrderFromFilter(filter);
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
		});
		
		c = new Controls(x, v);
		
		df = DateUtils.getShortDateFormat(ctx);
		filterValueNotFound = getString(R.string.filter_value_not_found);

		final Dialog d = new AlertDialog.Builder(getActivity())
			.setView(v)
		//.set
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
			updateSortOrderFromFilter(filter);
		}
		
		return d;
	}
	
	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	private void updateSortOrderFromFilter(ContentValues filter) {
		int sortOrder = filter.containsKey(F.SORT_ORDER) ? filter.getAsInteger(F.SORT_ORDER) : 0;
		c.sortOrder.setText(c.sortOrders[sortOrder == 1 ? 1 : 0]);
	}

	private void updateLocationFromFilter(ContentValues filter) {
		long locationId = filter.containsKey(F.LOCATION)
			? filter.getAsLong(F.LOCATION)
			: 0;
		if (locationId > 0 && Utils.moveCursor(locationCursor, BaseColumns._ID, locationId) != -1) {
			ContentValues location = LocationRepository.deserialize(locationCursor);
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
		final String[] sortOrders;
		
		final TextView period;
		final TextView location;
		final TextView sortOrder;
		
		final Button bOk;
		final Button bCancel;
		final ImageButton bNoFilter;
		
		public Controls(ActivityLayout x, View v) {
			sortOrders = v.getResources().getStringArray(R.array.sort_blotter_entries);
			
			LinearLayout layout = (LinearLayout)v.findViewById(R.id.layout);
			period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
			location = x.addFilterNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
			sortOrder = x.addFilterNodeMinus(layout, R.id.sort_order, R.id.sort_order_clear, R.string.sort_order, sortOrders[0]);
			
			bOk = (Button)v.findViewById(R.id.bOK);
			bCancel = (Button)v.findViewById(R.id.bCancel);
			bNoFilter = (ImageButton)v.findViewById(R.id.bNoFilter);
		}
	}
}