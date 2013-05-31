package com.wheelly.activity;

import com.wheelly.R;
import com.wheelly.app.AndiCarImporter;
import com.wheelly.fragments.HeartbeatListFragment;
import com.wheelly.fragments.MileageListFragment;
import com.wheelly.fragments.RefuelListFragment;
import com.wheelly.service.WorkflowNotifier;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

@SuppressLint("NewApi")
public class Main extends FragmentActivity {
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
        
		mTabsAdapter = new TabsAdapter(this, mViewPager);
		mTabsAdapter.addTab(bar.newTab()
				.setText(R.string.mileages)
				.setIcon(R.drawable.ic_tab_jet),
			MileageListFragment.class, null);
		
		mTabsAdapter.addTab(bar.newTab()
				.setText(R.string.refuels)
				.setIcon(R.drawable.ic_tab_utensils),
			RefuelListFragment.class, null);
		
		mTabsAdapter.addTab(bar.newTab()
				.setText(R.string.heartbeats)
				.setIcon(R.drawable.ic_tab_cam),
			HeartbeatListFragment.class, null);
		
		if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        } else {
        	bar.setSelectedNavigationItem(
        		getSharedPreferences("gui", Context.MODE_PRIVATE)
        			.getInt("main_selected_tab", 0));
        }
		
		new AndiCarImporter(this).attemptImporting();
		new WorkflowNotifier(this).notifyAboutPendingMileages();
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
			.putInt("main_selected_tab", getActionBar().getSelectedNavigationIndex())
			.commit();
		
		super.onDestroy();
	}

	/*
	private static final String CURRENT_TAB_TAG_KEY = "current_tab_tag_key";
	  private TabHost tabHost;
	  private TabManager tabManager;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);
			
		    tabHost = (TabHost) findViewById(android.R.id.tabhost);
		    tabHost.setup();
		    tabManager = new TabManager(this, tabHost, R.id.realtabcontent);
		    TabSpec mapTabSpec = tabHost.newTabSpec("mileages").setIndicator(
		        getString(R.string.mileages), getResources().getDrawable(R.drawable.ic_tab_jet));
		    tabManager.addTab(mapTabSpec, MileageListFragment.class, null);
		    TabSpec chartTabSpec = tabHost.newTabSpec("refuels").setIndicator(
		        getString(R.string.refuels),
		        getResources().getDrawable(R.drawable.ic_tab_utensils));
		    tabManager.addTab(chartTabSpec, RefuelListFragment.class, null);
		    TabSpec statsTabSpec = tabHost.newTabSpec("heartbeats").setIndicator(
		        getString(R.string.track_detail_stats_tab),
		        getResources().getDrawable(R.drawable.ic_tab_cam));
		    tabManager.addTab(statsTabSpec, HeartbeatListFragment.class, null);
		    if (savedInstanceState != null) {
		      tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_TAG_KEY));
		    }

		}

	
	@Override
	  protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putString(CURRENT_TAB_TAG_KEY, tabHost.getCurrentTabTag());
	  }*/
}