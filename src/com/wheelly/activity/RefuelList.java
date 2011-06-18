package com.wheelly.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.wheelly.R;
import com.wheelly.app.StatusBarControls;
import com.wheelly.app.FilterButton.OnFilterChangedListener;
import com.wheelly.content.TransactionRepository;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.F;
import com.wheelly.util.FilterUtils.FilterResult;

public class RefuelList extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.refuel_list);
	}
	
	public static class RefuelListFragment extends ListFragment
		implements LoaderCallbacks<Cursor> {
		
		private static final int REFUEL_LIST_LOADER = 0x02;
		private static final int NEW_REQUEST = 1;
		private static final int EDIT_REQUEST = 2;
		
		private StatusBarControls c;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			final FragmentActivity ctx = getActivity();
			
			setEmptyText(getString(R.string.no_refuels));
			
			getLoaderManager().initLoader(REFUEL_LIST_LOADER, null, this);
			
			setListAdapter(
				new SimpleCursorAdapter(getActivity(), R.layout.refuel_list_item, null,
					new String[] {
						"name", "mileage", "cost", "_created", "amount", "place"
					},
					new int[] {
						R.id.place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.place
					},
					0
				) {
					@Override
					public void setViewText(TextView v, String text) {
						switch(v.getId()) {
						case R.id.mileage:
						case R.id.fuel:
							v.setText("".equals(text) ? text : String.format("%+.2f", Float.parseFloat(text)));
							break;
						default: super.setViewText(v, text);
						}
					}
				}
			);
			
			registerForContextMenu(getListView());
			setHasOptionsMenu(true);
			
			// Set up status bar (if present).
			c = new StatusBarControls(ctx);
			c.AddButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					c.AddButton.setEnabled(false);
					startActivityForResult(new Intent(ctx, Refuel.class), NEW_REQUEST);
				}
			});
			c.TransferButton.setVisibility(View.GONE);
			c.TemplateButton.setVisibility(View.GONE);
			
			c.FilterButton.setLocationConstraint("refuels");
			c.FilterButton.SetOnFilterChangedListener(new OnFilterChangedListener() {
				@Override
				public void onFilterChanged(ContentValues value) {
					final Bundle args = new Bundle();
					args.putParcelable("filter", value);
					getLoaderManager().restartLoader(REFUEL_LIST_LOADER, args,
							RefuelListFragment.this);
				}
			});
			c.TotalLayout.setVisibility(View.GONE);
		}
		
		@Override
		public void onListItemClick(ListView listView, View view, int position, final long id) {
			Intent intent = new Intent(getActivity(), Refuel.class);
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
			inflater.inflate(R.menu.refuels_menu, menu);
			
			if(!new TransactionRepository(getActivity()).checkAvailability()) {
				menu.findItem(R.id.opt_menu_install_financisto).setVisible(true);
				Toast.makeText(getActivity(), R.string.advertise_financisto, Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.opt_menu_add:
					Intent intent = new Intent(getActivity(), Refuel.class);
					startActivityForResult(intent, NEW_REQUEST);
					return true;
				case R.id.opt_menu_install_financisto:
					Intent marketIntent = new Intent(Intent.ACTION_VIEW)
						.setData(Uri.parse("market://details?id=ru.orangesoftware.financisto"));
					startActivity(marketIntent);
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
			menu.setHeaderTitle("Refuels");
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
						.setMessage(R.string.delete_refuel_confirm)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								getActivity().getContentResolver().delete(
									Refuels.CONTENT_URI,
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
					filter, Refuels.FilterExpr);
				
				return new CursorLoader(getActivity(),
					Refuels.CONTENT_URI, Refuels.Columns,
					sql.Where, sql.Values, 	sql.Order);
			}
			
			return new CursorLoader(getActivity(),
				Refuels.CONTENT_URI, Refuels.Columns,
				null, null,
				Refuels.FilterExpr.get(F.SORT_ORDER) + " DESC");
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