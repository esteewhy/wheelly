package com.wheelly.activity;

import com.wheelly.R;
import com.wheelly.app.AndiCarImporter;
import com.wheelly.fragments.EventListFragment;
import com.wheelly.service.WorkflowNotifier;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

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
		
		new AndiCarImporter(this).attemptImporting();
		new WorkflowNotifier(this).notifyAboutPendingMileages();
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