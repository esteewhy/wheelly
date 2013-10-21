package com.wheelly.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.util.FloatMath;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.wheelly.R;
import com.wheelly.activity.Mileage;
import com.wheelly.app.ListConfiguration;
import com.wheelly.app.TripControlBar;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.fragments.InfoDialogFragment.Options;
import com.wheelly.service.MyTracksTracker;
import com.wheelly.service.WorkflowNotifier;

public class MileageListFragment extends ConfigurableListFragment {
	boolean suggestInstall;
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration();
		
		cfg.ConfirmDeleteResourceId = R.string.delete_mileage_confirm;
		cfg.EmptyTextResourceId = R.string.no_mileages;
		cfg.ItemActivityClass = Mileage.class;
		cfg.LocationFacetTable = "mileages";
		cfg.ContentUri = Mileages.CONTENT_URI;
		cfg.ListProjection = Mileages.ListProjection;
		cfg.FilterExpr = Mileages.FilterExpr;
		
		return cfg;
	}
	
	@Override
	public SimpleCursorAdapter createListAdapter() {
		final SimpleCursorAdapter adapter =
			new SimpleCursorAdapter(getActivity(), R.layout.mileage_list_item, null,
				new String[] {
					"start_place", "stop_place", "mileage", "cost", "_created", "fuel", "destination", "state", "leg",
				},
				new int[] {
					R.id.start_place, R.id.stop_place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.destination, R.id.indicator, R.id.leg
				},
				0//CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
			) {
				@Override
				public void setViewText(TextView v, String text) {
					switch(v.getId()) {
					case R.id.date:
						v.setText(com.wheelly.util.DateUtils.formatVarying(text));
						break;
					case R.id.mileage:
						v.setText("+".concat(Integer.toString((int)FloatMath.ceil(Float.parseFloat(text)))));
						break;
					case R.id.indicator:
						final int status = Integer.parseInt(text);
						v.setBackgroundColor(getResources().getColor(getStatusColor(status)));
						break;
					default: super.setViewText(v, text);
					}
				}
			};
		
		adapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View v, Cursor c, int columnIndex) {
				switch(v.getId()) {
				case R.id.leg:
					final int[] res = new int[] { R.drawable.leg0, R.drawable.leg1, R.drawable.leg2, R.drawable.leg3 };
					int legType = c.getInt(columnIndex);
					if(legType <= 3) {
						((ImageView)v).setImageResource(res[legType]);
					}
					return true;
				case R.id.start_place: {
					final String argb = c.getString(c.getColumnIndex("start_color"));
					((TextView)v).setBackgroundColor(Strings.isNullOrEmpty(argb) ? Color.TRANSPARENT : Color.parseColor(argb));
					break;
				}
				case R.id.stop_place: {
					final String argb = c.getString(c.getColumnIndex("stop_color"));
					((TextView)v).setBackgroundColor(Strings.isNullOrEmpty(argb) ? Color.TRANSPARENT : Color.parseColor(argb));
					break;
				}
				}
				
				return false;
			}
		});
		return adapter;
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

	private static int getStatusColor(int status) {
		switch(status) {
		case Mileages.STATE_ACTIVE:
			return R.color.sync_succeeded;
		case Mileages.STATE_TRACKING:
			return R.color.sync_conflict;
		}
		return R.color.sync_unknown;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//Advise user installing MyTracks app.
		suggestInstall =
			null != savedInstanceState && savedInstanceState.containsKey("suggestInstall")
				? savedInstanceState.getBoolean("suggestInstall")
				: !new MyTracksTracker(getActivity()).checkAvailability();
		
		if(suggestInstall) {
			Toast.makeText(getActivity(), R.string.advertise_mytracks, Toast.LENGTH_LONG).show();
		}
		
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("suggestInstall", suggestInstall);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mileages_menu, menu);
		menu.findItem(R.id.opt_menu_install_mytracks).setVisible(suggestInstall);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(R.string.mileages);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.opt_menu_install_mytracks:
			Intent marketIntent = new Intent(Intent.ACTION_VIEW)
				.setData(Uri.parse("market://details?id=com.google.android.maps.mytracks"));
			startActivity(marketIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void deleteItem(long id) {
		super.deleteItem(id);
		new WorkflowNotifier(getActivity()).canceNotificationForMileage(id);
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		
		final Cursor c = (Cursor) ((SimpleCursorAdapter)getListAdapter()).getItem(position);
		final int state = c.getInt(c.getColumnIndexOrThrow("state"));
		
		switch(state) {
		case Mileages.STATE_ACTIVE:
			Intent intent = new Intent(getActivity(), Mileage.class);
			intent.putExtra(BaseColumns._ID, id);
			intent.putExtra("ui_command", TripControlBar.UI_STOP);
			startActivityForResult(intent, EDIT_REQUEST);
			break;
		case Mileages.STATE_TRACKING:
			editItem(id);
			break;
		default:
			super.onListItemClick(listView, view, position, id);
		}
	}
}