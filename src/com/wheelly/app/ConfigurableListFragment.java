package com.wheelly.app;

import java.io.File;

import org.openintents.calendarpicker.contract.CalendarPickerConstants;

import ru.orangesoftware.financisto.activity.LocationsListActivity;

import com.wheelly.IFilterHolder;
import com.wheelly.R;
import com.wheelly.activity.Preferences;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.util.BackupUtils;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.F;
import com.wheelly.util.FilterUtils.FilterResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public abstract class ConfigurableListFragment extends ListFragment
	implements LoaderCallbacks<Cursor> {
	
	private ListConfiguration configuration;
	
	protected abstract ListConfiguration configure();
	
	private ListConfiguration getConfiguration() {
		return null == configuration ? (configuration = configure()) : configuration;
	}
	
	private static final int LIST_LOADER = 0x01;
	private static final int NEW_REQUEST = 1;
	protected static final int EDIT_REQUEST = 2;
	private static final int DELETE_REQUEST = 3;
	
	//private ProgressDialog progressDialog;
	private StatusBarControls c;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final FragmentActivity ctx = getActivity();
		final ListConfiguration cfg = getConfiguration();
		
		setEmptyText(getString(cfg.EmptyTextResourceId));
		setListAdapter(cfg.createListAdapter(ctx));
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		
		final ContentValues globalFiler = ((IFilterHolder)ctx.getApplicationContext()).getFilter();
		
		// Set up status bar (if present).
		c = new StatusBarControls(ctx);
		c.AddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				c.AddButton.setEnabled(false);
				Intent intent = new Intent(ctx, cfg.ItemActivityClass);
				startActivityForResult(intent, NEW_REQUEST);
			}
		});
		c.TransferButton.setVisibility(View.GONE);
		c.TemplateButton.setVisibility(View.GONE);
		
		c.FilterButton.setLocationConstraint(cfg.LocationFacetTable);
		c.FilterButton.SetOnFilterChangedListener(new com.wheelly.widget.FilterButton.OnFilterChangedListener() {
			@Override
			public void onFilterChanged(ContentValues value) {
				final Bundle args = new Bundle();
				args.putParcelable("filter", value);
				getLoaderManager().restartLoader(LIST_LOADER, args,
						ConfigurableListFragment.this);
				
				globalFiler.clear();
				globalFiler.putAll(value);
			}
		});
		c.TotalLayout.setVisibility(View.GONE);
	}
	
	@Override
	public void onResume() {
		final ContentValues filter = ((IFilterHolder)getActivity().getApplicationContext()).getFilter();
		c.FilterButton.setFilter(filter);
		if(null == getLoaderManager().getLoader(LIST_LOADER)) {
			getLoaderManager().initLoader(LIST_LOADER, null, this);
		}
		super.onResume();
	}
	
	protected void viewItem(final long id) {
		final ListConfiguration cfg = getConfiguration();
		final InfoDialogFragment.Options dialogCfg = cfg.configureViewItemDialog();
		
		dialogCfg.onClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == Dialog.BUTTON_POSITIVE) {
					editItem(id);
				}
			};
		};
		
		dialogCfg.loader = cfg.createViewItemCursorLoader(getActivity(), id);
		
		new InfoDialogFragment(dialogCfg).show(getFragmentManager(), "dialog");
	}
	
	protected void editItem(final long id) {
		Intent intent = new Intent(getActivity(), getConfiguration().ItemActivityClass);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_REQUEST);
	}
	
	protected void deleteItem(final long id) {
		getActivity().getContentResolver().delete(
			getConfiguration().ContentUri,
			BaseColumns._ID + " = ?",
			new String[] { Long.toString(id) }
		);
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
		case EDIT_REQUEST:
			((SimpleCursorAdapter)getListAdapter()).notifyDataSetChanged();
			break;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.common_menu, menu);
		
		final boolean backupEnabled = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		menu.findItem(R.id.opt_menu_backup).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_restore).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_locations).setIntent(new Intent(this.getActivity(), LocationsListActivity.class));
		menu.findItem(R.id.opt_menu_preferences).setIntent(new Intent(this.getActivity(), Preferences.class));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final ListConfiguration cfg = getConfiguration();
		
		final String backupPath= Environment.getExternalStorageDirectory().getPath() + File.separator + "wheelly.db";
		
		switch (item.getItemId()) {
			case R.id.opt_menu_add:
				Intent intent = new Intent(getActivity(), cfg.ItemActivityClass);
				startActivityForResult(intent, NEW_REQUEST);
				return true;
			case R.id.opt_menu_backup:
				return BackupUtils.backupDatabase(
					new DatabaseHelper(ConfigurableListFragment.this.getActivity())
						.getReadableDatabase()
						.getPath(),
					backupPath);
			case R.id.opt_menu_restore:
				return BackupUtils.backupDatabase(
						backupPath,
					new DatabaseHelper(ConfigurableListFragment.this.getActivity())
						.getReadableDatabase()
						.getPath()
					);
			case R.id.opt_menu_calendar:
				long calendar_id = 1;
				Uri u = com.wheelly.content.EventContentProvider.constructUri(calendar_id);
				Intent i = new Intent(Intent.ACTION_PICK, u);
				downloadLaunchCheck(i, REQUEST_CODE_DATE_SELECTION);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
//	private static final int DIALOG_CALENDARPICKER_DOWNLOAD = 1;
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
		getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
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
					.setMessage(getConfiguration().ConfirmDeleteResourceId)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							deleteItem(mi.id);
							onActivityResult(DELETE_REQUEST, Activity.RESULT_OK, null);
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
		
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setListShown(false);
			}
		});
		
		final ListConfiguration cfg = getConfiguration();
		
		if(args != null && args.containsKey("filter")
				&& (filter = args.getParcelable("filter")).size() > 0) {
			final FilterResult sql = FilterUtils.updateSqlFromFilter(
				filter, cfg.FilterExpr);
			
			return new CursorLoader(getActivity(),
				cfg.ContentUri, cfg.ListProjection,
				sql.Where, sql.Values, 	sql.Order);
		}
		
		return new CursorLoader(getActivity(),
			cfg.ContentUri, cfg.ListProjection,
			null, null,
			cfg.FilterExpr.get(F.SORT_ORDER) + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		((CursorAdapter) getListAdapter()).swapCursor(data);
		
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setListShown(true);
			}
		});
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		((CursorAdapter) getListAdapter()).swapCursor(null);
	}
}