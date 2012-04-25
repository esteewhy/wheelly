package com.wheelly.app;

import java.util.GregorianCalendar;

import org.openintents.calendarpicker.contract.CalendarPickerConstants;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.widget.SimpleCursorAdapter;

import com.google.android.apps.mytracks.io.sendtogoogle.AccountChooserActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.wheelly.R;
import com.wheelly.activity.Heartbeat;
import com.wheelly.app.InfoDialogFragment.Options;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.HeartbeatBroker;

public class HeartbeatListFragment extends ConfigurableListFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.heartbeat_list, null);
	}
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration() {
			
			@Override
			public SimpleCursorAdapter createListAdapter(final Context context) {
				final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(context).getInt("fuel_capacity", 60);
				return
					new SimpleCursorAdapter(getActivity(), R.layout.heartbeat_list_item, null,
						new String[] {
							"odometer", "_created", "fuel", "fuel", "place", "icons", "status"
						},
						new int[] {
							R.id.odometer, R.id.date, R.id.fuelAmt, R.id.fuelGauge, R.id.place, R.id.icon_refuel, R.id.indicator
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
								case R.id.icon_refuel:
									int mask = cursor.getInt(columnIndex);
									final View v = (View)view.getParent();
									v.findViewById(R.id.icon_refuel).setVisibility((mask & 4) > 0 ? View.VISIBLE : View.GONE);
									v.findViewById(R.id.icon_start).setVisibility((mask & 2) > 0 ? View.VISIBLE : View.GONE);
									v.findViewById(R.id.icon_stop).setVisibility((mask & 1) > 0 ? View.VISIBLE : View.GONE);
									return true;
								case R.id.indicator:
									final int status = cursor.getInt(columnIndex);
									view.setBackgroundColor(context.getResources().getColor(
										status > 0
											? R.color.sync_succeeded
											: status < 0
												? R.color.sync_failed
												: R.color.sync_unknown)
									);
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
		};
		
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
	public boolean onContextItemSelected(MenuItem item) {
		if(!super.onContextItemSelected(item)) {
			if(item.getItemId() == R.id.ctx_menu_sync) {
				final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
				Intent intent = new Intent(getActivity(), AccountChooserActivity.class);
				SendRequest req = new SendRequest(mi.id, false, false, true);
				req.setSendDocs(true);
				intent.putExtra(SendRequest.SEND_REQUEST_KEY, req);
				startActivity(intent);
				return true;
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.heartbeats_menu, menu);
		
		menu.add(Menu.NONE, 5, Menu.NONE, "Sync")
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent(getActivity(), AccountChooserActivity.class);
					SendRequest req = new SendRequest(-1, false, false, true);
					req.setSendDocs(true);
					intent.putExtra(SendRequest.SEND_REQUEST_KEY, req);
					startActivity(intent);
					return true;
				}
			});
		
		menu.add(Menu.NONE, 7, Menu.NONE, "Calendar")
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem paramMenuItem) {
					long calendar_id = 1;
					Uri u = com.wheelly.content.EventContentProvider.constructUri(calendar_id);
					Intent i = new Intent(Intent.ACTION_PICK, u);
					downloadLaunchCheck(i, REQUEST_CODE_DATE_SELECTION);

					return true;
				}
			});
	}
	
	private static final int DIALOG_CALENDARPICKER_DOWNLOAD = 1;
	private static final int REQUEST_CODE_DATE_SELECTION = 1;

	// ========================================================================
	void downloadLaunchCheck(Intent intent, int request_code) {
		if (CalendarPickerConstants.DownloadInfo.isIntentAvailable(this.getActivity(), intent))
			if (request_code >= 0)
				startActivityForResult(intent, request_code);
			else
				startActivity(intent);
//		else
//			getActivity().showDialog(DIALOG_CALENDARPICKER_DOWNLOAD);
	}
	
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.heartbeats);
		
		AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
		if(0 < new HeartbeatBroker(getActivity()).referenceCount(mi.id)) {
			menu.removeItem(R.id.ctx_menu_delete);
		}
		
		menu.add(Menu.NONE, R.id.ctx_menu_sync, Menu.NONE, "Sync");
	}
}