package com.wheelly.activity;

import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.wheelly.R;
import com.wheelly.fragments.LocationsListFragment;
import com.wheelly.fragments.LocationsMapFragment;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            //NavUtils.navigateUpFromSameTask(this);
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}