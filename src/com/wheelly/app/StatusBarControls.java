package com.wheelly.app;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.wheelly.R;

/**
 * Reusable holder of references to status_bar layout controls.
 */
public final class StatusBarControls {
	public final ImageButton AddButton;
	public final ImageButton TransferButton;
	public final ImageButton TemplateButton;
	public final ImageButton FilterButton;
	public final ViewGroup TotalLayout;
		
	public StatusBarControls(Activity v) {
		AddButton		= (ImageButton)v.findViewById(R.id.bAdd);
		TransferButton	= (ImageButton)v.findViewById(R.id.bTransfer);
		TemplateButton	= (ImageButton)v.findViewById(R.id.bTemplate);
		FilterButton	= (ImageButton)v.findViewById(R.id.bFilter);
		TotalLayout		= (ViewGroup)v.findViewById(R.id.totalLayout);
	}
}