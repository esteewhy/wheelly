package com.wheelly.activity;

import com.wheelly.R;
import com.wheelly.service.WorkflowNotifier;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

@SuppressLint("NewApi")
public class Main extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		new WorkflowNotifier(this).notifyAboutPendingMileages();
	}
}