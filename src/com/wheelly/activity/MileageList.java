package com.wheelly.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.wheelly.R;
import com.wheelly.app.FilterButton.OnFilterChangedListener;
import com.wheelly.app.StatusBarControlsv4;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.MileageRepository;
import com.wheelly.db.MileageBroker;
import com.wheelly.service.Tracker;

public class MileageList extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mileage_list);
	}
	
	public static class MileageListFragment extends ListFragment {
		private static final int NEW_REQUEST = 1;
		private static final int EDIT_REQUEST = 2;
		
		private SQLiteDatabase db;
		private StatusBarControlsv4 c;
		private boolean suggestInstall = false;
		
		@SuppressWarnings("deprecation")
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			final FragmentActivity ctx = getActivity();
			
			setEmptyText(getString(R.string.no_mileages));
			
			//Advise user installing MyTracks app.
			suggestInstall =
				null != savedInstanceState && savedInstanceState.containsKey("suggestInstall")
					? savedInstanceState.getBoolean("suggestInstall")
					: !new Tracker(ctx).checkAvailability();
			
			if(!suggestInstall) {
				Toast.makeText(ctx, R.string.advertise_mytracks, Toast.LENGTH_LONG).show();
			}

			final Cursor cursor = new MileageRepository(db = new DatabaseHelper(ctx).getReadableDatabase()).list();
			ctx.startManagingCursor(cursor);
			
			setListAdapter(
				new SimpleCursorAdapter(ctx, R.layout.mileage_list_item, cursor,
					new String[] {
						"start_place", "stop_place", "mileage", "cost", "stop_time", "fuel", "destination"
					},
					new int[] {
						R.id.start_place, R.id.stop_place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.destination
					}
				) {
					@Override
					public void setViewText(TextView v, String text) {
						switch(v.getId()) {
						case R.id.mileage:
							int val = Integer.parseInt(text);
							v.setText(Html.fromHtml(String.format("%c<b>%03d</b>", val >= 0 ? '+' : '-', val)));
							break;
						case R.id.fuel:
							v.setText(String.format("%\u002003.2f", Float.parseFloat(text)));
							break;
						default: super.setViewText(v, text);
						}
					}
				});
			registerForContextMenu(getListView());
			setHasOptionsMenu(true);
			
			// Set up status bar (if present).
			c = new StatusBarControlsv4(ctx);
			c.AddButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					c.AddButton.setEnabled(false);
					Intent intent = new Intent(ctx, Mileage.class);
					startActivityForResult(intent, NEW_REQUEST);
				}
			});
			c.TransferButton.setVisibility(View.GONE);
			c.TemplateButton.setVisibility(View.GONE);
			
			c.FilterButton.setLocationConstraint("mileages");
			c.FilterButton.SetOnFilterChangedListener(new OnFilterChangedListener() {
				@Override
				public void onFilterChanged(ContentValues value) {
					getActivity().stopManagingCursor(((SimpleCursorAdapter)MileageListFragment.this.getListAdapter()).getCursor());
					
					MileageRepository repo = new MileageRepository(db);
					Cursor cursor = value.size() == 0 ? repo.list() : repo.list(value);
					getActivity().startManagingCursor(cursor);
					((SimpleCursorAdapter)MileageListFragment.this.getListAdapter()).changeCursor(cursor);
				}
			});
			c.TotalLayout.setVisibility(View.GONE);
		}
		
		@Override
		public void onDestroy() {
			db.close();
			super.onDestroy();
		}
		
		@Override
		public void onListItemClick(ListView listView, View view, int position, final long id) {
			Intent intent = new Intent(getActivity(), Mileage.class);
			intent.putExtra(BaseColumns._ID, id);
			startActivityForResult(intent, EDIT_REQUEST);
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
			inflater.inflate(R.menu.mileages_menu, menu);
			menu.findItem(R.id.opt_menu_install_mytracks).setVisible(suggestInstall);
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.opt_menu_add:
					Intent intent = new Intent(getActivity(), Mileage.class);
					startActivityForResult(intent, NEW_REQUEST);
					return true;
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
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
			menu.setHeaderTitle("Mileages");
			super.onCreateContextMenu(menu, v, menuInfo);
		}
		
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			super.onContextItemSelected(item);
			
			final AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
			
			switch (item.getItemId()) {
				case R.id.ctx_menu_view: {
					//viewItem(mi.position, mi.id);
					return true;
				} 			
				case R.id.ctx_menu_edit:
					onListItemClick(null, null, 0, mi.id);
					return true;
				case R.id.ctx_menu_delete:
					new AlertDialog.Builder(getActivity())
						.setMessage(R.string.delete_mileage_confirm)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								new MileageBroker(getActivity()).delete(mi.id);
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
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putBoolean("suggestInstall", suggestInstall);
		}
	}
}