package com.wheelly.activity;

import ru.orangesoftware.financisto.R;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final TabHost tabHost = getTabHost();
		
		tabHost.addTab(tabHost
				.newTabSpec("accounts")
				.setIndicator(
						getString(R.string.accounts),
						getResources().getDrawable(
								R.drawable.ic_tab_accounts
						)
				)
				.setContent(
						new Intent(this,
								MileageListActivity.class
						)
				)
		);
		
		tabHost.setOnTabChangedListener(this);
	}

	@Override
	public void onTabChanged(String arg0) {
		// TODO Auto-generated method stub
		
	}
}
