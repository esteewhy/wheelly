package com.wheelly.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.FloatMath;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.wheelly.R;
import com.wheelly.activity.Heartbeat;
import com.wheelly.activity.LocationsList;
import com.wheelly.activity.Preferences;
import com.wheelly.activity.Refuel;
import com.wheelly.activity.Start;
import com.wheelly.activity.Stop;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.db.MileageBroker;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.util.BackupHelper;

public class EventListFragment extends ListFragment {
	private static final int LIST_LOADER = 0x01;
	protected static final int NEW_REQUEST = 1;
	protected static final int EDIT_REQUEST = 2;
	private static final int DELETE_REQUEST = 3;
	
	final LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			setListShown(false);
			
			return new CursorLoader(getActivity(),
					Timeline.CONTENT_URI, Timeline.ListProjection,
				null, null,
				"h._created DESC, h.odometer DESC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			final CursorAdapter a = (CursorAdapter) getListAdapter();
			a.swapCursor(data);
			a.notifyDataSetChanged();
			
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			((CursorAdapter) getListAdapter()).swapCursor(null);
		}
	};
	
	final OnClickListener heartbeatStarter = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final long id = (Long)v.getTag();
			Intent intent = new Intent(getActivity(), Heartbeat.class);
			intent.putExtra(BaseColumns._ID, id);
			startActivityForResult(intent, EDIT_REQUEST);
		}
	};
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(createListAdapter());
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
	}
	
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
							tv.setBackgroundColor(TextUtils.isEmpty(argb) ? Color.TRANSPARENT : Color.parseColor(argb));
							
							final int[] icons = new int[] { R.drawable.hb_start, R.drawable.hb_stop, R.drawable.hb_refuel, R.drawable.hb_refuel, };
							final int iconIndex = new StringBuilder(Integer.toString(cursor.getInt(cursor.getColumnIndex("type")), 2)).reverse().indexOf("1");
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
							final int imgIndex = Math.min(cursor.getInt(columnIndex), res.length - 1);
							final int imgRes = res[imgIndex];
							ImageView v = (ImageView)view; 
							v.setImageResource(imgRes);
							v.setTag(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
							v.setOnClickListener(heartbeatStarter);
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
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.no_mileages));
	}
	
	@Override
	public void onResume() {
		if(null == getLoaderManager().getLoader(LIST_LOADER)) {
			getLoaderManager().initLoader(LIST_LOADER, null, loaderCallbacks);
		}
		super.onResume();
	}
	
	protected void deleteItem(final long id) {
		getActivity().getContentResolver().delete(
			Heartbeats.CONTENT_URI,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) }
		);
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position, final long id) {
		editItem(id);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.common_menu, menu);
		inflater.inflate(R.menu.events_menu, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		menu.findItem(R.id.opt_menu_locations).setIntent(new Intent(this.getActivity(), LocationsList.class));
		menu.findItem(R.id.opt_menu_preferences).setIntent(new Intent(this.getActivity(), Preferences.class));
		menu.findItem(R.id.opt_menu_add).setVisible(false);
		menu.findItem(R.id.opt_menu_filter).setVisible(false);
		
		final boolean backupEnabled = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		menu.findItem(R.id.opt_menu_backup).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_restore).setVisible(backupEnabled);
		
		boolean finish = new MileageBroker(getActivity()).getLastPendingId() >= 0;
		menu.findItem(R.id.opt_menu_mileage_start).setVisible(!finish);
		menu.findItem(R.id.opt_menu_mileage_stop).setVisible(finish);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context ctx = getActivity();
		
		switch (item.getItemId()) {
			case R.id.opt_menu_add: {
				Intent intent = new Intent(getActivity(), Heartbeat.class);
				startActivityForResult(intent, NEW_REQUEST);
				break;
			}
			case R.id.opt_menu_backup:
				return new BackupHelper(ctx).backup();
			case R.id.opt_menu_restore:
				new BackupHelper(ctx).restore();
				break;
			case R.id.opt_menu_mileage_start: {
				Intent intent = new Intent(getActivity(), Start.class);
				startActivityForResult(intent, NEW_REQUEST);
			}
			break;
			case R.id.opt_menu_mileage_stop: {
				Intent intent = new Intent(getActivity(), Stop.class);
				intent.putExtra(BaseColumns._ID, new MileageBroker(getActivity()).getLastPendingId());
				startActivityForResult(intent, NEW_REQUEST);
			}
			break;
			case R.id.opt_menu_refuel: {
				Intent intent = new Intent(getActivity(), Refuel.class);
				startActivityForResult(intent, NEW_REQUEST);
			}
			break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if(getUserVisibleHint()) {
			final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
			
			switch (item.getItemId()) {
				case R.id.ctx_menu_edit:
					editItem(mi.id);
					return true;
				case R.id.ctx_menu_delete:
					new AlertDialog.Builder(getActivity())
						.setMessage(R.string.delete_mileage_confirm)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								deleteItem(mi.id);
								onActivityResult(DELETE_REQUEST, Activity.RESULT_OK, null);
							}
						})
						.setNegativeButton(android.R.string.no, null)
						.show();
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	
	protected void editItem(long id) {
		final ContentValues values = new HeartbeatBroker(getActivity()).loadOrCreate(id);
		
		if(values.getAsLong("_id") == id) {
			final int type = values.getAsInteger("type");
			
			Class<?> activityClass = 
				(type & 1) != 0
					? Start.class
					: (type & 2) != 0
						? Stop.class
						: (type & 4) != 0
							? Refuel.class
							: Heartbeat.class;
			
			Intent intent = new Intent(getActivity(), activityClass);
			intent.putExtra(BaseColumns._ID, id);
			startActivityForResult(intent, EDIT_REQUEST);
			return;
		}
	}
}