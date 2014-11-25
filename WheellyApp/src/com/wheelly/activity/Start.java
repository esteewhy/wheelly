package com.wheelly.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.wheelly.R;
import com.wheelly.fragments.StartFragment;

public class Start extends ActionBarActivity {
	
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
				.add(R.id.fragment_container, new StartFragment())
				.commit();
			
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
}