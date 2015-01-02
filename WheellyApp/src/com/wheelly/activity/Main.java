package com.wheelly.activity;

import com.wheelly.R;
import com.wheelly.fragments.DispatchFragment;
import com.wheelly.fragments.EventListFragment;
import com.wheelly.fragments.EventListFragment.OnOpenItemListener;
import com.wheelly.fragments.ItemFragment.OnFragmentResultListener;
import com.wheelly.service.WorkflowNotifier;

import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

public class Main extends ActionBarActivity implements OnOpenItemListener, OnFragmentResultListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		final FragmentManager fm = getSupportFragmentManager();
		
		EventListFragment lf = (EventListFragment)fm.findFragmentByTag("list");
		if(null == lf) {
			lf = new EventListFragment();
			fm.beginTransaction()
				.replace(R.id.list_container, lf, "list")
				.commit();
		}
		
		lf.setOnOpenItemListener(this);
		
		DispatchFragment df = (DispatchFragment)fm.findFragmentByTag("details");
		final boolean dualPane = getResources().getBoolean(R.bool.dual_pane);
		if(null != df) {
			if(!dualPane) {
				fm.beginTransaction().hide(df).commit();
				fm.beginTransaction()
					.hide(lf)
					.show(df)
					.addToBackStack(null)
					.commit();
			} else {
				if(fm.getBackStackEntryCount() > 0) {
					fm.popBackStackImmediate();
					fm.beginTransaction()
						.replace(R.id.detail_container, df, "details")
						.commit();
				}
			}
			df.setOnFinishedListener(this);
		}
		
		fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				getSupportActionBar().setDisplayHomeAsUpEnabled(fm.getBackStackEntryCount() > 0);
				ActivityCompat.invalidateOptionsMenu(Main.this);
			}
		});
		
		
		
		new WorkflowNotifier(this).notifyAboutPendingMileages();
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		if(getIntent().hasExtra(BaseColumns._ID)) {
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			onOpenItem(getIntent().getExtras());
		}
	}
	
	@Override
	public void onOpenItem(Bundle args) {
		final FragmentManager fm = getSupportFragmentManager();
		
		DispatchFragment f = new DispatchFragment();
		f.setArguments(args);
		f.setOnFinishedListener(this);
		
		FragmentTransaction t = fm.beginTransaction()
			.replace(R.id.detail_container, f, "details");
		
		if(!getResources().getBoolean(R.bool.dual_pane)) {
			t.hide(fm.findFragmentByTag("list"));
			t.addToBackStack(null);
		}
		t.commit();
	}
	
	@Override
	public void fragmentFinished(Bundle extras) {
		getSupportFragmentManager().popBackStack();
	}

	@Override
	public void fragmentCancelled() {
		getSupportFragmentManager().popBackStack();
	}
}