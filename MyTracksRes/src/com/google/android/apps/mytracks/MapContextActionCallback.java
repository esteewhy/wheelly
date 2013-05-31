package com.google.android.apps.mytracks;

import com.google.android.gms.maps.model.LatLng;

public interface MapContextActionCallback extends ContextualActionModeCallback {
	public boolean onMapLongClick(LatLng point);
	public void onCancel();
}