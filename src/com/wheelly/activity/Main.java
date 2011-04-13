package com.wheelly.activity;

import com.wheelly.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class Main extends TabActivity implements TabHost.OnTabChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		final TabHost tabHost = super.getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("mileages")
			.setIndicator("Mileages",
				getResources().getDrawable(R.drawable.mileage)
			).setContent(new Intent(this, MileageList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("heartbeats")
			.setIndicator("Heartbeats",
				getResources().getDrawable(R.drawable.heartbeat)
			).setContent(new Intent(this, HeartbeatList.class))
		);
		
		tabHost.addTab(tabHost.newTabSpec("refuels")
			.setIndicator("Refuels",
				getResources().getDrawable(R.drawable.refuel)
			).setContent(new Intent(this, RefuelList.class))
		);
		
		tabHost.setOnTabChangedListener(this);
	}

	@Override
	public void onTabChanged(String arg0) {
		// TODO Auto-generated method stub
		
	}
}
