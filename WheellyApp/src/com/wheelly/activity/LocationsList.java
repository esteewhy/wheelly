package com.wheelly.activity;

import ru.orangesoftware.financisto.activity.LocationActivity;

import com.squareup.otto.Subscribe;
import com.squareup.otto.sample.BusProvider;
import com.wheelly.R;
import com.wheelly.bus.LocationChoosenEvent;
import com.wheelly.bus.LocationSelectedEvent;
import com.wheelly.bus.LocationsLoadedEvent;
import com.wheelly.db.LocationsSchema;
import com.wheelly.fragments.LocationsListFragment;
import com.wheelly.fragments.LocationsMapFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

@SuppressLint("NewApi")
public class LocationsList extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_list);
		initUIAsViewPager(savedInstanceState);
		BusProvider.getInstance().register(this);
	}
	
	private void initUIAsViewPager(Bundle savedInstanceState) {
		ViewPager mViewPager = (ViewPager)findViewById(R.id.pager);
		
		if(null == mViewPager) return;
		
		final ActionBar bar = getSupportActionBar();
		
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		
		TabsAdapter mTabsAdapter = new TabsAdapter(this, mViewPager);
		mTabsAdapter.addTab(bar.newTab().setText("List"),
			LocationsListFragment.class, null);
		mTabsAdapter.addTab(bar.newTab().setText("Map"),
			LocationsMapFragment.class, null);
		
		int index = -1;
		if(savedInstanceState != null) {
			index = savedInstanceState.getInt("tab", 0);
		}
		if(index < 0) {
			index = getSharedPreferences("gui", Context.MODE_PRIVATE).getInt("locations_selected_tab", 0);
		}
		if(index >= 0) {
			bar.setSelectedNavigationItem(index);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	@Override
	protected void onDestroy() {
		final int index = getSupportActionBar().getSelectedNavigationIndex();
		if(index > 0) {
			getSharedPreferences("gui", Context.MODE_PRIVATE)
				.edit()
				.putInt("locations_selected_tab", index)
				.commit();
		}
		
		BusProvider.getInstance().unregister(this);
		
		super.onDestroy();
	}
	
	@Override
	public void onResume() {
		if(null == getLoaderManager().getLoader(0)) {
			getSupportLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
				@Override
				public Loader<Cursor> onCreateLoader(int paramInt, Bundle paramBundle) {
					return new CursorLoader(LocationsList.this, LocationsSchema.CONTENT_URI, null, null, null, null);
				}
				
				@Override
				public void onLoadFinished(Loader<Cursor> paramLoader, Cursor cursor) {
					BusProvider.getInstance().post(new LocationsLoadedEvent(cursor));
					
					if(getIntent().hasExtra(LocationActivity.LOCATION_ID_EXTRA)) {
						final long id = getIntent().getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
						
						if(id >= 0 && getContentResolver().query(ContentUris.withAppendedId(LocationsSchema.CONTENT_URI, id), null, null, null, null).moveToFirst()) {
							BusProvider.getInstance().post(new LocationSelectedEvent(id, this));
						} else {
							getIntent().putExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
						}
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