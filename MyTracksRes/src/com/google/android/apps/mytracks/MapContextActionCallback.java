package com.google.android.apps.mytracks;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.model.Marker;

public interface MapContextActionCallback {
	public void onCreate(Menu menu);
	public boolean onClick(MenuItem item, Marker marker);
	public void onPrepare(Menu menu, Marker marker);
}