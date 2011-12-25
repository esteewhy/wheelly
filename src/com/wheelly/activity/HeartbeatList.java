package com.wheelly.activity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.support.v4.widget.SimpleCursorAdapter;
import com.wheelly.R;
import com.wheelly.app.ConfigurableListFragment;
import com.wheelly.app.InfoDialogFragment;
import com.wheelly.app.ListConfiguration;
import com.wheelly.app.InfoDialogFragment.Options;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.HeartbeatBroker;

public class HeartbeatList extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heartbeat_list);
	}
	
	public static class HeartbeatListFragment extends ConfigurableListFragment {
		@Override
		protected ListConfiguration configure() {
			ListConfiguration cfg = new ListConfiguration() {
				
				@Override
				public SimpleCursorAdapter createListAdapter(Context context) {
					final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(context).getInt("fuel_capacity", 60);
					return
						new SimpleCursorAdapter(getActivity(), R.layout.heartbeat_list_item, null,
							new String[] {
								"odometer", "_created", "fuel", "fuel", "place", "icons"
							},
							new int[] {
								R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge, R.id.place, R.id.icons
							},
							0
						) {{
							setViewBinder(new SimpleCursorAdapter.ViewBinder(){
								@Override
								public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
									switch(view.getId()) {
									case R.id.date:
										((TextView)view).setText(com.wheelly.util.DateUtils.formatVarying(cursor.getString(columnIndex)));
										return true;
									case R.id.fuelGauge:
										ProgressBar pb = (ProgressBar)view; 
										pb.setProgress(cursor.getInt(columnIndex));
										pb.setMax(fuelCapacity);
										return true;
									case R.id.icons:
										int mask = cursor.getInt(columnIndex);
										view.findViewById(R.id.icon_refuel).setVisibility((mask & 4) > 0 ? View.VISIBLE : View.GONE);
										view.findViewById(R.id.icon_start).setVisibility((mask & 2) > 0 ? View.VISIBLE : View.GONE);
										view.findViewById(R.id.icon_stop).setVisibility((mask & 1) > 0 ? View.VISIBLE : View.GONE);
										return true;
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
							
							fields.put(R.string.odometer_input_label, "odometer");
							fields.put(R.string.fuel_input_label, "fuel");
							
							titleField = "place";
							dataField = "_created";
							iconResId = R.drawable.heartbeat;
						}};
				}
				
				@Override
				public CursorLoader createViewItemCursorLoader(Context context, long id) {
					return
						new CursorLoader(
							context,
							Heartbeats.CONTENT_URI,
							Heartbeats.ListProjection,
							"h." + BaseColumns._ID + " = ?",
							new String[] { Long.toString(id) },
							"h." + BaseColumns._ID + " DESC LIMIT 1"
						);
				}
				
				@Override
				public void onCreateContextMenu(ContextMenu menu,
						ContextMenuInfo menuInfo) {
					AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
					if(0 < new HeartbeatBroker(getActivity()).referenceCount(mi.id)) {
						menu.removeItem(R.id.ctx_menu_delete);
					}
				}
			};
			
			cfg.ConfirmDeleteResourceId = R.string.delete_heartbeat_confirm;
			cfg.ContextMenuHeaderResourceId = R.string.heartbeats;
			cfg.EmptyTextResourceId = R.string.no_heartbeats;
			cfg.OptionsMenuResourceId = R.menu.heartbeats_menu;
			cfg.ItemActivityClass = Heartbeat.class;
			cfg.LocationFacetTable = "heartbeats";
			cfg.ContentUri = Heartbeats.CONTENT_URI;
			cfg.ListProjection = Heartbeats.ListProjection;
			cfg.FilterExpr = Heartbeats.FilterExpr;
			
			return cfg;
		}
	}
}