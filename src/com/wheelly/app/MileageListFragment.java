package com.wheelly.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelly.R;
import com.wheelly.activity.Mileage;
import com.wheelly.app.InfoDialogFragment.Options;
import com.wheelly.db.DatabaseSchema.Mileages;
import com.wheelly.service.Tracker;

public class MileageListFragment extends ConfigurableListFragment {
	boolean suggestInstall;
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration() {
			@Override
			public SimpleCursorAdapter createListAdapter(final Context context) {
				return
					new SimpleCursorAdapter(context, R.layout.mileage_list_item, null,
						new String[] {
							"start_place", "stop_place", "mileage", "cost", "_created", "fuel", "destination", "state"
						},
						new int[] {
							R.id.start_place, R.id.stop_place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.destination, R.id.indicator
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
								v.setText("+".concat(Integer.toString((int)Math.ceil(Float.parseFloat(text)))));
								break;
							case R.id.indicator:
								final int status = Integer.parseInt(text);
								v.setBackgroundColor(context.getResources().getColor(getStatusColor(status)));
								break;
							default: super.setViewText(v, text);
							}
						}
					};
			}
			
			@Override
			public Options configureViewItemDialog() {
				return
					new InfoDialogFragment.Options() {{
						
						fields.put(R.string.mileage_input_label, "mileage");
						fields.put(R.string.fuel_consumption, "fuel");
						fields.put(R.string.departure, "start_time");
						fields.put(R.string.origin, "start_place");
						fields.put(R.string.finish, "stop_place");
						
						titleField = "destination";
						dataField = "_created";
						iconResId = R.drawable.ic_tab_jet_selected;
					}};
			}

			
			@Override
			public CursorLoader createViewItemCursorLoader(Context context, long id) {
				return
					new CursorLoader(
						context,
						Mileages.CONTENT_URI,
						Mileages.SINGLE_VIEW_PROJECTION,
						"m." + BaseColumns._ID + " = ?",
						new String[] { Long.toString(id) },
						"m." + BaseColumns._ID + " DESC LIMIT 1"
					);
			}
		};
		
		cfg.ConfirmDeleteResourceId = R.string.delete_mileage_confirm;
		cfg.EmptyTextResourceId = R.string.no_mileages;
		cfg.ItemActivityClass = Mileage.class;
		cfg.LocationFacetTable = "mileages";
		cfg.ContentUri = Mileages.CONTENT_URI;
		cfg.ListProjection = Mileages.ListProjection;
		cfg.FilterExpr = Mileages.FilterExpr;
		
		return cfg;
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.mileage_list, null);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		final Context context = getActivity();
		//Advise user installing MyTracks app.
		suggestInstall =
			null != savedInstanceState && savedInstanceState.containsKey("suggestInstall")
				? savedInstanceState.getBoolean("suggestInstall")
				: !new Tracker(context).checkAvailability();
		
		if(suggestInstall) {
			Toast.makeText(context, R.string.advertise_mytracks, Toast.LENGTH_LONG).show();
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