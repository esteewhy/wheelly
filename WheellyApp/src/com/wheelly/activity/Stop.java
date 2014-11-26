package com.wheelly.activity;

import android.support.v4.app.Fragment;
import com.wheelly.fragments.StopFragment;

public class Stop extends ItemActivity {
	@Override
	protected Fragment getFragment() {
		return new StopFragment();
	}
}