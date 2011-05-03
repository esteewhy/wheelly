package com.wheelly.activity;

import ru.orangesoftware.financisto.activity.LocationsListActivity;

import com.wheelly.R;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem mi = menu.add(Menu.NONE, 1, Menu.NONE, R.string.location);
		mi.setIcon(R.drawable.menu_entities_locations);
		mi.setIntent(new Intent(this, LocationsListActivity.class));
		return true;
	}
	
	@Override
	public void onTabChanged(String arg0) {
		// TODO Auto-generated method stub
		
	}
}