package com.wheelly.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.widget.SimpleCursorAdapter;

import com.google.common.base.Strings;
import com.wheelly.R;
import com.wheelly.activity.Heartbeat;
import com.wheelly.app.ListConfiguration;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.fragments.InfoDialogFragment.Options;
import com.wheelly.service.Synchronizer;

public class HeartbeatListFragment extends ConfigurableListFragment {
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration();
		
		cfg.ConfirmDeleteResourceId = R.string.delete_heartbeat_confirm;
		cfg.EmptyTextResourceId = R.string.no_heartbeats;
		cfg.ItemActivityClass = Heartbeat.class;
		cfg.LocationFacetTable = "heartbeats";
		cfg.ContentUri = Heartbeats.CONTENT_URI;
		cfg.ListProjection = Heartbeats.ListProjection;
		cfg.FilterExpr = Heartbeats.FilterExpr;
		
		return cfg;
	}
	
	@Override
	public SimpleCursorAdapter createListAdapter() {
		final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("fuel_capacity", 60);
		return
			new SimpleCursorAdapter(getActivity(), R.layout.heartbeat_list_item, null,
				new String[] {
					"odometer", "_created", "fuel", "fuel", "place", "sync_state"
				},
				new int[] {
					R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge, R.id.place, R.id.indicator
				},
				0
			) {{
				setViewBinder(new SimpleCursorAdapter.ViewBinder(){
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
						case R.id.fuelGauge:
							ProgressBar pb = (ProgressBar)view; 
							pb.setProgress(cursor.getInt(columnIndex));
							pb.setMax(fuelCapacity);
							return true;
						case R.id.indicator:
							final int status = cursor.getInt(columnIndex);
							view.setBackgroundColor(getResources().getColor(getStatusColor(status)));
							return true;
						}
						return false;
					}
				});
			}};
	}
	
	private int getStatusColor(int status) {
		switch(status) {
		case 1:
			return R.color.sync_succeeded;
		case 2:
			return R.color.sync_outdated;
		case 3:
			return R.color.sync_conflict;
		}
		return R.color.sync_unknown;
	}
	
	@Override
	public Options configureViewItemDialog() {
		return
			new InfoDialogFragment.Options() {{
				
				fields.put(R.string.odometer_input_label, "odometer");
				fields.put(R.string.fuel_input_label, "fuel");
				
				titleField = "place";
				dataField = "_created";
				iconResId = R.drawable.ic_tab_cam_selected;
			}};
	}
	
	@Override
	public CursorLoader createViewItemCursorLoader(long id) {
		return
			new CursorLoader(
				getActivity(),
				Heartbeats.CONTENT_URI,
				Heartbeats.ListProjection,
				"h." + BaseColumns._ID + " = ?",
				new String[] { Long.toString(id) },
				"h." + BaseColumns._ID + " DESC LIMIT 1"
			);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(!super.onContextItemSelected(item)) {
			switch(item.getItemId()) {
			case R.id.ctx_menu_sync:
				final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
				new Synchronizer(getActivity(), getFragmentManager()).execute(mi.id);
				return true;
			case R.id.ctx_menu_sync_pull:
			case R.id.ctx_menu_sync_push: {
				final ContentValues values = new ContentValues();
				values.put(BaseColumns._ID, ((AdapterContextMenuInfo)item.getMenuInfo()).id);
				values.put("sync_state", item.getItemId() == R.id.ctx_menu_sync_pull
					? Timeline.SYNC_STATE_READY : Timeline.SYNC_STATE_CHANGED);
				new HeartbeatBroker(getActivity()).updateOrInsert(values);
				return true;
			}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		//inflater.inflate(R.menu.heartbeats_menu, menu);
		
		menu.add(Menu.NONE, 5, Menu.NONE, "Sync")
			.setIcon(android.R.drawable.ic_menu_upload)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					//new WheellyProviderUtils(getActivity()).resetSync(-1);
					new Synchronizer(getActivity(), getFragmentManager()).execute(-1);
					return true;
				}
			});
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.heartbeats);
		
		AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
		if(0 < new HeartbeatBroker(getActivity()).referenceCount(mi.id)) {
			menu.removeItem(R.id.ctx_menu_delete);
		}
		final Cursor c = (Cursor) ((SimpleCursorAdapter)getListAdapter()).getItem(mi.position);
		final long syncState = c.getLong(c.getColumnIndexOrThrow("sync_state"));
		
		if(syncState != Timeline.SYNC_STATE_CONFLICT) {
			menu.add(Menu.NONE, R.id.ctx_menu_sync, Menu.NONE, "Sync");
		} else {
			menu.add(Menu.NONE, R.id.ctx_menu_sync_pull, Menu.NONE, "Pull");
			menu.add(Menu.NONE, R.id.ctx_menu_sync_push, Menu.NONE, "Push");
		}
	}
}