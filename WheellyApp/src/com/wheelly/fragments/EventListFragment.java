package com.wheelly.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.util.FloatMath;
import android.util.Pair;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.wheelly.R;
import com.wheelly.activity.Heartbeat;
import com.wheelly.activity.Mileage;
import com.wheelly.activity.Refuel;
import com.wheelly.app.ListConfiguration;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.MileageBroker;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.fragments.InfoDialogFragment.Options;

public class EventListFragment extends ConfigurableListFragment {
	boolean suggestInstall;
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration();
		cfg.ConfirmDeleteResourceId = R.string.delete_mileage_confirm;
		cfg.EmptyTextResourceId = R.string.no_mileages;
		cfg.ItemActivityClass = Heartbeat.class;
		cfg.LocationFacetTable = null;
		cfg.ContentUri = Timeline.CONTENT_URI;
		cfg.ListProjection = Timeline.ListProjection;
		cfg.FilterExpr = Timeline.FilterExpr;
		
		return cfg;
	}
	
	@Override
	public SimpleCursorAdapter createListAdapter() {
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("fuel_capacity", 60);
		return
			new SimpleCursorAdapter(getActivity(), R.layout.event_list_item, null,
				new String[] {
					"odometer", "_created", "amount", "fuel", "place", "leg"
				},
				new int[] {
					R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge, R.id.place, R.id.leg
				},
				0
			) {
				@Override
				public int getViewTypeCount() {
					return 2;
				}
				
				@Override
				public int getItemViewType(int position) {
					// TODO Auto-generated method stub
					if(position == getSelectedItemPosition()) {
						return super.getItemViewType(position);
					}
					
					return super.getItemViewType(position);
				}
			{
				setViewBinder(new SimpleCursorAdapter.ViewBinder() {
					@Override
					public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
						switch(view.getId()) {
						case R.id.place: {
							final String argb = cursor.getString(cursor.getColumnIndex("color"));
							final TextView tv = (TextView)view; 
							tv.setBackgroundColor(Strings.isNullOrEmpty(argb) ? Color.TRANSPARENT : Color.parseColor(argb));
							
							final int[] icons = new int[] { R.drawable.hb_stop, R.drawable.hb_start, R.drawable.hb_refuel, };
							final int iconIndex = new StringBuilder(Integer.toString(cursor.getInt(cursor.getColumnIndex("icons")), 2)).reverse().indexOf("1");
							tv.setCompoundDrawablesWithIntrinsicBounds(0 <= iconIndex && iconIndex < icons.length ? icons[iconIndex] : 0, 0, 0, 0);
							return false;
						}
						case R.id.date:
							((TextView)view).setText(com.wheelly.util.DateUtils.formatVarying(cursor.getString(columnIndex)));
							return true;
						case R.id.fuelGauge: {
							ProgressBar pb = (ProgressBar)view; 
							pb.setProgress(cursor.getInt(columnIndex));
							final long leg = cursor.getLong(cursor.getColumnIndex("leg"));
							if(4 == leg) {
								final Float fuel = cursor.getFloat(cursor.getColumnIndex("amount"));
								final int progress = pb.getProgress();
								pb.setProgress(progress - (int)FloatMath.ceil(fuel));
								pb.setSecondaryProgress(progress);
							} else {
								pb.setSecondaryProgress(0);
							}
							pb.setMax(fuelCapacity);
							return true;
						}
						case R.id.leg:
							final int[] res = new int[] { R.drawable.leg0, R.drawable.leg2, R.drawable.leg5, R.drawable.leg3, R.drawable.leg4, R.drawable.leg6, R.drawable.leg7 };
							ImageView v = (ImageView)view; 
							v.setImageResource(res[Math.min(cursor.getInt(columnIndex), res.length - 1)]);
							v.setTag(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
							v.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									long id = (Long)v.getTag();
								}
							});
							return true;
						case R.id.odometer: {
							final long leg = cursor.getLong(cursor.getColumnIndex("leg"));
							if(2 == leg || 3 == leg) {
								final Float distance = cursor.getFloat(cursor.getColumnIndex("distance"));
								((TextView)view).setText("+".concat(Integer.toString((int)FloatMath.ceil(distance))));
								return true;
							}
							return false;
						}/*
						case R.id.fuelAmt: {
							final long leg = cursor.getLong(cursor.getColumnIndex("leg"));
							if(2 == leg || 3 == leg) {
								final Float fuel = cursor.getFloat(cursor.getColumnIndex("amount"));
								((TextView)view).setText("-".concat(Integer.toString((int)FloatMath.ceil(fuel))));
								return true;
							}
							return false;
						}*/
						}
						return false;
					}
				});
			}};
	}
	
	@Override
	public Options configureViewItemDialog() {
		return
			new InfoDialogFragment.Options() {{
				
				fields.put(R.string.mileage_input_label, "mileage");
				fields.put(R.string.fuel_burnt, "fuel");
				fields.put(R.string.departure, "start_time");
				fields.put(R.string.origin, "start_place");
				fields.put(R.string.finish, "stop_place");
				fields.put(R.string.fuel_consumption, "consumption");
				
				titleField = "destination";
				dataField = "_created";
				iconResId = R.drawable.ic_tab_jet_selected;
			}};
	}
	
	@Override
	public CursorLoader createViewItemCursorLoader(long id) {
		return
			new CursorLoader(
				getActivity(),
				Mileages.CONTENT_URI,
				Mileages.SINGLE_VIEW_PROJECTION,
				"m." + BaseColumns._ID + " = ?",
				new String[] { Long.toString(id) },
				"m." + BaseColumns._ID + " DESC LIMIT 1"
			);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.events_menu, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.opt_menu_add).setVisible(false);
		
		final boolean backupEnabled = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		menu.findItem(R.id.opt_menu_backup).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_restore).setVisible(backupEnabled);
		
		boolean finish = new MileageBroker(getActivity()).getLastPendingId() >= 0;
		menu.findItem(R.id.opt_menu_mileage_start).setVisible(!finish);
		menu.findItem(R.id.opt_menu_mileage_stop).setVisible(finish);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.opt_menu_mileage_start: {
			Intent intent = new Intent(getActivity(), Mileage.class);
			startActivityForResult(intent, NEW_REQUEST);
		}
		return true;
		case R.id.opt_menu_mileage_stop: {
			Intent intent = new Intent(getActivity(), Mileage.class);
			intent.putExtra(BaseColumns._ID, new MileageBroker(getActivity()).getLastPendingId());
			startActivityForResult(intent, NEW_REQUEST);
		}
		return true;
		case R.id.opt_menu_refuel: {
			Intent intent = new Intent(getActivity(), Refuel.class);
			startActivityForResult(intent, NEW_REQUEST);
		}
		return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Build.VERSION.SDK_INT >= 11) {
			getActivity().invalidateOptionsMenu();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void editItem(long id) {
		final Pair<Integer, Long> relatedItem = new HeartbeatBroker(getActivity()).related(id);
		
		if(null != relatedItem && relatedItem.second >= 0) {
			Class<?> activityClass = Heartbeat.class;
			
			switch(relatedItem.first) {
			case 4:
				activityClass = Refuel.class;
				break;
			case 2:
			case 1:
				activityClass = Mileage.class;
				break;
			}
			Intent intent = new Intent(getActivity(), activityClass);
			intent.putExtra(BaseColumns._ID, relatedItem.second);
			startActivityForResult(intent, EDIT_REQUEST);
			return;
		}
		
		super.editItem(id);
	}
}