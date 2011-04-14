package com.wheelly.activity;

import com.wheelly.R;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class Main extends TabActivity implements TabHost.OnTabChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		final TabHost tabHost = super.getTabHost();
		final Resources res = getResources();
		
		tabHost.addTab(tabHost.newTabSpec("mileages")
			.setIndicator("Mileages", res.getDrawable(R.drawable.ic_tab_mileage)
			).setContent(new Intent(this, MileageList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("heartbeats")
			.setIndicator("Heartbeats", res.getDrawable(R.drawable.ic_tab_heartbeat)
			).setContent(new Intent(this, HeartbeatList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("refuels")
			.setIndicator("Refuels", res.getDrawable(R.drawable.ic_tab_refuel)
			).setContent(new Intent(this, RefuelList.class))
		);
		
		tabHost.setOnTabChangedListener(this);
	}

	@Override
	public void onTabChanged(String arg0) {
		// TODO Auto-generated method stub
		
	}
}
