package com.wheelly.fragments;

import java.io.File;

import org.openintents.calendarpicker.contract.CalendarPickerConstants;

import com.wheelly.IFilterHolder;
import com.wheelly.R;
import com.wheelly.activity.FilterDialog;
import com.wheelly.activity.FilterDialog.OnFilterChangedListener;
import com.wheelly.activity.LocationsList;
import com.wheelly.activity.Preferences;
import com.wheelly.app.ListConfiguration;
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
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
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
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public abstract class ConfigurableListFragment extends ListFragment
	implements LoaderCallbacks<Cursor> {
	
	private ListConfiguration configuration;
	
	protected abstract ListConfiguration configure();
	public abstract SimpleCursorAdapter createListAdapter();
	public abstract CursorLoader createViewItemCursorLoader(long id);
	public abstract InfoDialogFragment.Options configureViewItemDialog();

	private ListConfiguration getConfiguration() {
		return null == configuration ? (configuration = configure()) : configuration;
	}
	
	private static final int LIST_LOADER = 0x01;
	private static final int NEW_REQUEST = 1;
	protected static final int EDIT_REQUEST = 2;
	private static final int DELETE_REQUEST = 3;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(createListAdapter());
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListConfiguration cfg = getConfiguration();
		setEmptyText(getString(cfg.EmptyTextResourceId));
		
		final ContentValues globalFiler = ((IFilterHolder)getActivity().getApplicationContext()).getFilter();
		
		setLocationConstraint(cfg.LocationFacetTable);
		SetOnFilterChangedListener(new OnFilterChangedListener() {
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
		
		final ContentValues filter = ((IFilterHolder)getActivity().getApplicationContext()).getFilter();
		setFilter(filter);
	}
	
	@Override
	public void onResume() {
		if(null == getLoaderManager().getLoader(LIST_LOADER)) {
			getLoaderManager().initLoader(LIST_LOADER, null, this);
		}
		super.onResume();
	}
	
	protected void viewItem(final long id) {
		final InfoDialogFragment.Options dialogCfg = configureViewItemDialog();
		
		dialogCfg.onClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == Dialog.BUTTON_POSITIVE) {
					editItem(id);
				}
			};
		};
		
		dialogCfg.loader = createViewItemCursorLoader(id);
		
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.common_menu, menu);
		
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		final boolean backupEnabled = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		menu.findItem(R.id.opt_menu_backup).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_restore).setVisible(backupEnabled);
		menu.findItem(R.id.opt_menu_locations).setIntent(new Intent(this.getActivity(), LocationsList.class));
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
			case R.id.opt_menu_filter:
				filter.put(F.LOCATION_CONSTRAINT, locationConstraint);
				final FilterDialog fd = new FilterDialog(new ContentValues(filter), new OnFilterChangedListener() {
					
					@Override
					public void onFilterChanged(ContentValues value) {
						if (null == value) {
							setFilter(null);
						} else {
							filter.remove(F.LOCATION_CONSTRAINT);
							setFilter(value);
						}
						
						
					}
				});
				fd.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface paramDialogInterface) {
						
					}
				});
				fd.show(getActivity().getSupportFragmentManager(), "filter");
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
		if(getUserVisibleHint()) {
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
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final ContentValues filter;
		
		setListShown(false);
		
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
	
	// Filter management
	private final ContentValues filter = new ContentValues();
	public String locationConstraint = null; 
	
	public ContentValues getFilter() {
		return filter;
	}
	
	private void setFilter(ContentValues filter) {
		if(!this.filter.equals(filter)
				&& (this.filter.size() > 0 || null != filter)) {
			
			setFilterUnchecked(filter);
			
			if(null != listener) {
				listener.onFilterChanged(this.filter);
			}
		}
	}
	
	private void setFilterUnchecked(ContentValues filter) {
		boolean reset = null == filter || 0 == filter.size();
		
		this.filter.clear();
		
		if(!reset) {
			this.filter.putAll(filter);
		}
		
		//setImageResource(reset ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
	}
	
	private void setLocationConstraint(String locationConstraint) {
		this.locationConstraint = locationConstraint;
	}
	
	private OnFilterChangedListener listener;
	
	private void SetOnFilterChangedListener(OnFilterChangedListener listener) {
		this.listener = listener;
	}
}