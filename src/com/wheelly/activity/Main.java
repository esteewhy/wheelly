package com.wheelly.activity;

import ru.orangesoftware.financisto.activity.LocationsListActivity;

import com.wheelly.R;
import com.wheelly.app.AndiCarImporter;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;
import android.widget.TabHost;

public class Main extends TabActivity implements TabHost.OnTabChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		final TabHost tabHost = getTabHost();
		final Resources res = getResources();
		
		tabHost.addTab(tabHost.newTabSpec("mileages")
			.setIndicator(getString(R.string.mileages), res.getDrawable(R.drawable.ic_tab_mileage))
			.setContent(new Intent(this, MileageList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("refuels")
			.setIndicator(getString(R.string.refuels), res.getDrawable(R.drawable.ic_tab_refuel))
			.setContent(new Intent(this, RefuelList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("heartbeats")
			.setIndicator(getString(R.string.heartbeats), res.getDrawable(R.drawable.ic_tab_heartbeat))
			.setContent(new Intent(this, HeartbeatList.class))
		);
		
		tabHost.setOnTabChangedListener(this);
		
		new AndiCarImporter(this).attemptImporting();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.locations)
			.setIcon(R.drawable.menu_entities_locations)
			.setIntent(new Intent(this, LocationsListActivity.class));
		
		menu.add(Menu.NONE, 2, Menu.NONE, R.string.preferences)
			.setIcon(android.R.drawable.ic_menu_preferences)
			.setIntent(new Intent(this, Preferences.class));
		
		return true;
	}
	
	@Override
	public void onTabChanged(String arg0) {
		// TODO Auto-generated method stub
		
	}
}