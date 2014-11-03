package com.google.android.apps.mytracks;

import android.view.Menu;
import android.view.MenuItem;

import com.androidmapsextensions.Marker;

public interface MapContextActionCallback {
	public void onCreate(Menu menu);
	public boolean onClick(MenuItem item, Marker marker);
	public void onPrepare(Menu menu, Marker marker);
	public void onResetMarker(Marker marker);
}