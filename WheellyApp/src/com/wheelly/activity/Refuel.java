package com.wheelly.activity;

import android.support.v4.app.Fragment;
import com.wheelly.fragments.RefuelFragment;

public class Refuel extends ItemActivity {
	@Override
	protected Fragment getFragment() {
		return new RefuelFragment();
	}
}