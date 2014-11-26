package com.wheelly.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import com.wheelly.R;

public abstract class ItemActivity extends ActionBarActivity {
	
	protected abstract Fragment getFragment();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_activity);
		
		if (null != findViewById(R.id.fragment_container)) {
			if (savedInstanceState != null) {
				return;
			}
		
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_container, getFragment())
				.commit();
			
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
}