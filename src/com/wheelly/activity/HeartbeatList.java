package com.wheelly.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.support.v4.widget.SimpleCursorAdapter;
import com.wheelly.R;
import com.wheelly.util.FilterUtils.F;
import com.wheelly.app.InfoDialogFragment;
import com.wheelly.app.StatusBarControls;
import com.wheelly.app.FilterButton.OnFilterChangedListener;
import com.wheelly.db.DatabaseSchema.Heartbeats;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.FilterResult;

public class HeartbeatList extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heartbeat_list);
	}
	
	public static class HeartbeatListFragment extends ListFragment
		implements LoaderCallbacks<Cursor> {
		
		private static final int HEARTBEAT_LIST_LOADER = 0x03;
		private static final int NEW_REQUEST = 1;
		private static final int EDIT_REQUEST = 2;
		
		private StatusBarControls c;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			final FragmentActivity ctx = getActivity();
			
			setEmptyText(getString(R.string.no_heartbeats));
			getLoaderManager().initLoader(HEARTBEAT_LIST_LOADER, null, this);
			final int fuelCapacity = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("fuel_capacity", 60);
			
			setListAdapter(
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
				}});
			
			registerForContextMenu(getListView());
			setHasOptionsMenu(true);
			
			// Set up status bar (if present).
			c = new StatusBarControls(ctx);
			c.AddButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					c.AddButton.setEnabled(false);
					startActivityForResult(new Intent(ctx, Heartbeat.class), NEW_REQUEST);
				}
			});
			c.TransferButton.setVisibility(View.GONE);
			c.TemplateButton.setVisibility(View.GONE);
			
			c.FilterButton.setLocationConstraint("heartbeats");
			c.FilterButton.SetOnFilterChangedListener(new OnFilterChangedListener() {
				@Override
				public void onFilterChanged(ContentValues value) {
					final Bundle args = new Bundle();
					args.putParcelable("filter", value);
					getLoaderManager().restartLoader(HEARTBEAT_LIST_LOADER, args,
							HeartbeatListFragment.this);
				}
			});
			c.TotalLayout.setVisibility(View.GONE);
		}
		
		private void viewItem(final long id) {
			new InfoDialogFragment(
				new InfoDialogFragment.Options() {{
					
					fields.put(R.string.odometer_input_label, "odometer");
					fields.put(R.string.fuel_input_label, "fuel");
					
					titleField = "place";
					dataField = "_created";
					iconResId = R.drawable.heartbeat;
					
					onClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == Dialog.BUTTON_POSITIVE) {
								editItem(id);
							}
						};
					};
					
					loader = new CursorLoader(
						getActivity(),
						Heartbeats.CONTENT_URI,
						Heartbeats.ListProjection,
						"h." + BaseColumns._ID + " = ?",
						new String[] { Long.toString(id) },
						"h." + BaseColumns._ID + " DESC LIMIT 1");
				}}
			).show(getFragmentManager(), "dialog");
		}
		
		private void editItem(final long id) {
			Intent intent = new Intent(getActivity(), Heartbeat.class);
			intent.putExtra(BaseColumns._ID, id);
			startActivityForResult(intent, EDIT_REQUEST);
		}
		
		@Override
		public void onListItemClick(ListView listView, View view, int position, final long id) {
			viewItem(id);
		}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch(requestCode) {
			case NEW_REQUEST:
				c.AddButton.setEnabled(true);
				break;
			}
			((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			inflater.inflate(R.menu.heartbeats_menu, menu);
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.opt_menu_add:
					Intent intent = new Intent(getActivity(), Heartbeat.class);
					startActivityForResult(intent, NEW_REQUEST);
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
			getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
			menu.setHeaderTitle("Heartbeat");
			
			if(0 < new HeartbeatBroker(getActivity()).referenceCount(mi.id)) {
				menu.removeItem(R.id.ctx_menu_delete);
			}
			
			super.onCreateContextMenu(menu, v, menuInfo);
		}
		
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			super.onContextItemSelected(item);
			
			final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
			
			switch (item.getItemId()) {
				case R.id.ctx_menu_view: {
					viewItem(mi.id);
					return true;
				} 			
				case R.id.ctx_menu_edit:
					editItem(mi.id);
					return true;
				case R.id.ctx_menu_delete:
					new AlertDialog.Builder(getActivity())
						.setMessage(R.string.delete_heartbeat_confirm)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								getActivity().getContentResolver().delete(
									Heartbeats.CONTENT_URI,
									BaseColumns._ID + " = ?",
									new String[] { Long.toString(mi.id) });
								
								onActivityResult(0, RESULT_OK, null);
							}
						})
						.setNegativeButton(R.string.no, null)
						.show();
					return true;
			}
			return false;
		}
		
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final ContentValues filter;
			
			if(args != null && args.containsKey("filter")
					&& (filter = args.getParcelable("filter")).size() > 0) {
				final FilterResult sql = FilterUtils.updateSqlFromFilter(
					filter, Heartbeats.FilterExpr);
				
				return new CursorLoader(getActivity(),
					Heartbeats.CONTENT_URI, Heartbeats.ListProjection,
					sql.Where, sql.Values, 	sql.Order);
			}
			
			return new CursorLoader(getActivity(),
				Heartbeats.CONTENT_URI, Heartbeats.ListProjection,
				null, null,
				Heartbeats.FilterExpr.get(F.SORT_ORDER) + " DESC");
		}
	
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			((CursorAdapter) getListAdapter()).swapCursor(data);
		}
	
		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			((CursorAdapter) getListAdapter()).swapCursor(null);
		}
	}
}