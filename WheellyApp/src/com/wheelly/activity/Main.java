package com.wheelly.activity;

import com.wheelly.R;
import com.wheelly.app.AndiCarImporter;
import com.wheelly.fragments.EventListFragment;
import com.wheelly.fragments.HeartbeatListFragment;
import com.wheelly.fragments.MileageListFragment;
import com.wheelly.fragments.RefuelListFragment;
import com.wheelly.service.WorkflowNotifier;
import com.wheelly.util.ViewGroupUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

@SuppressLint("NewApi")
public class Main extends ActionBarActivity {
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
		
		final ActionBar bar = getSupportActionBar();
		
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        
        ///setupSpinner();
        
		mTabsAdapter = new TabsAdapter(this, mViewPager);
		
		mTabsAdapter.addTab(bar.newTab()
				.setText(R.string.time)
				.setIcon(R.drawable.ic_tab_cam),
			EventListFragment.class, null);
		
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
		
		bar.setSelectedNavigationItem( 
			Math.min(
				savedInstanceState != null
					? savedInstanceState.getInt("tab", 0)
					: getSharedPreferences("gui", Context.MODE_PRIVATE).getInt("main_selected_tab", 0),
				bar.getTabCount() - 1
			)
		);
		
		new AndiCarImporter(this).attemptImporting();
		new WorkflowNotifier(this).notifyAboutPendingMileages();
	}
	
	//http://www.hasnath.net/blog/actionbar-tab-spinnerlist-navigation-at-the-same-time
	void setupSpinner() {
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        
        // If you're using sherlock, in older versions of android you are not supposed to get a reference to android.R.id.action_bar_title, So here's little hack for that.
        if (titleId == 0) {
            titleId = R.id.action_bar_title;
        }
         
        View titleView = findViewById(titleId);
        ViewGroupUtils.replaceView(titleView, getLayoutInflater().inflate(R.layout.spinner_layout, null));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	@Override
	protected void onDestroy() {
		getSharedPreferences("gui", Context.MODE_PRIVATE)
			.edit()
			.putInt("main_selected_tab", getSupportActionBar().getSelectedNavigationIndex())
			.commit();
		
		super.onDestroy();
	}
}