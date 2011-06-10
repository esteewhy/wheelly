package com.wheelly.app;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.wheelly.R;

/**
 * Reusable holder of references to status_bar layout controls.
 */
public final class StatusBarControlsv4 {
	public final ImageButton AddButton;
	public final ImageButton TransferButton;
	public final ImageButton TemplateButton;
	public final FilterButton FilterButton;
	public final ViewGroup TotalLayout;
		
	public StatusBarControlsv4(FragmentActivity v) {
		final FragmentManager fm = v.getSupportFragmentManager();
		
		AddButton		= (ImageButton)v.findViewById(R.id.bAdd);
		TransferButton	= (ImageButton)v.findViewById(R.id.bTransfer);
		TemplateButton	= (ImageButton)v.findViewById(R.id.bTemplate);
		FilterButton	= (FilterButton)fm.findFragmentById(R.id.bFilter);
		TotalLayout		= (ViewGroup)v.findViewById(R.id.totalLayout);
	}
}