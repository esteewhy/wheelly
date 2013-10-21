package com.wheelly.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.wheelly.R;
import com.wheelly.fragments.MileageFragment;

public class Mileage extends FragmentActivity {
	
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
				.add(R.id.fragment_container, new MileageFragment())
				.commit();
			
			ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);
		}
	}
}