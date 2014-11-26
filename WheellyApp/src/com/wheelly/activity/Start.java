package com.wheelly.activity;

import android.support.v4.app.Fragment;
import com.wheelly.fragments.StartFragment;

public class Start extends ItemActivity {
	
	@Override
	protected Fragment getFragment() {
		return new StartFragment();
	}
}