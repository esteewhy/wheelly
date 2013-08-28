package com.google.android.apps.mytracks;

import pl.mg6.android.maps.extensions.Marker;
import android.view.Menu;
import android.view.MenuItem;

public interface MapContextActionCallback {
	public void onCreate(Menu menu);
	public boolean onClick(MenuItem item, Marker marker);
	public void onPrepare(Menu menu, Marker marker);
	public void onResetMarker(Marker marker);
}