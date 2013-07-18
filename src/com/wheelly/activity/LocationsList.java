package com.wheelly.activity;

import ru.orangesoftware.financisto.activity.LocationActivity;

import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.LocationChoosenEvent;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.fragments.LocationsListFragment;
import com.wheelly.fragments.LocationsMapFragment;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

@SuppressLint("NewApi")
public class LocationsList extends FragmentActivity {
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mViewPager = new ViewPager(this);
		mViewPager.setId(R.id.pager);

		setContentView(mViewPager);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);

		mTabsAdapter = new TabsAdapter(this, mViewPager);
		mTabsAdapter.addTab(bar.newTab().setText("List"),
				LocationsListFragment.class, null);
		mTabsAdapter.addTab(bar.newTab().setText("Map"),
				LocationsMapFragment.class, null);
		
		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		} else {
			bar.setSelectedNavigationItem(getSharedPreferences("gui", Context.MODE_PRIVATE).getInt("locations_selected_tab", 0));
		}
		
		BusProvider.getInstance().register(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
	}
	
	@Override
	protected void onDestroy() {
		getSharedPreferences("gui", Context.MODE_PRIVATE)
			.edit()
			.putInt("locations_selected_tab", getActionBar().getSelectedNavigationIndex())
			.commit();
		
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// NavUtils.navigateUpFromSameTask(this);
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onResume() {
		if(null == getLoaderManager().getLoader(0)) {
			getSupportLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
				@Override
				public Loader<Cursor> onCreateLoader(int paramInt, Bundle paramBundle) {
					return new CursorLoader(LocationsList.this, Locations.CONTENT_URI, null, null, null, null);
				}
				
				@Override
				public void onLoadFinished(Loader<Cursor> paramLoader, Cursor cursor) {
					BusProvider.getInstance().post(new LocationsLoadedEvent(cursor));
					
					if(getIntent().hasExtra(LocationActivity.LOCATION_ID_EXTRA)) {
						final long id = getIntent().getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
						BusProvider.getInstance().post(new LocationSelectedEvent(id, this));
					}
				}
				
				@Override
				public void onLoaderReset(Loader<Cursor> paramLoader) {
					BusProvider.getInstance().post(new LocationsLoadedEvent(null));
				}
			});
		}
		super.onResume();
	}
	
	@Subscribe public void onLocationChoosen(LocationChoosenEvent event) {
		final Intent intent = new Intent();
		intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, event.id);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}
}