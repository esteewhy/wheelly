package com.wheelly.app;

import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.View;
import android.widget.TextView;
import com.google.api.client.util.Strings;
import com.wheelly.R;
import com.wheelly.util.LocationUtils;

public class LocationViewBinder implements ViewBinder {
	public Location location;
	
	public LocationViewBinder(Location location) {
		this.location = location;
	}
	
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		switch(view.getId()) {
		case android.R.id.text1:
			final String argb = cursor.getString(cursor.getColumnIndex("color"));
			((TextView)view).setBackgroundColor(!Strings.isNullOrEmpty(argb) ? Color.parseColor(argb) : Color.TRANSPARENT);
			return false;
		case android.R.id.text2:
			((TextView)view).setText(LocationUtils.locationToText(cursor));
			return true;
		case R.id.text3:
			if(null != location) {
				Location dest = new Location(location);
				dest.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
				dest.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
				((TextView)view.findViewById(R.id.text3)).setText(LocationUtils.formatDistance(location.distanceTo(dest)));
			}
			
			return true;
		}
		return false;
	}
}