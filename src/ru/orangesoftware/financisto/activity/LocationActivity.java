/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.LocationRepository;

import ru.orangesoftware.financisto.utils.AddressGeocoder;
import ru.orangesoftware.financisto.utils.Utils;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.MapView.ReticleDrawMode;

public class LocationActivity extends MapActivity {
	
	public static final String LOCATION_ID_EXTRA = "locationId";
	static final int MENU_SATELLITE = Menu.FIRST + 1;
	static final int MENU_MOVE_MARKER_TO_MAP_CENTER = Menu.FIRST + 3;
	static final int MENU_GO_TO_MARKER = Menu.FIRST + 4;

	MapView mapView;
	LocationOvelay locationOverlay;
	MyLocationOverlay myLocationOverlay;
	TextView location;
	Vibrator vibrator;
	
	ContentValues myLocation = new ContentValues();
	
    @SuppressWarnings("unused")
	@Override 
    public void onCreate(Bundle icicle) { 
        super.onCreate(icicle); 
        setContentView(R.layout.location);

        vibrator = (Vibrator)getSystemService(Activity.VIBRATOR_SERVICE);
        mapView = (MapView)findViewById(R.id.mapview);
        location = (TextView)findViewById(R.id.location);
                
		Intent intent = getIntent();
		if (intent != null) {
			long locationId = intent.getLongExtra(LOCATION_ID_EXTRA, -1);
			if (locationId != -1) {
				myLocation = new LocationRepository(new DatabaseHelper(this).getReadableDatabase()).load(locationId);
				EditText name = (EditText)findViewById(R.id.name);
				name.setText(myLocation.getAsString("name"));
				String resolvedAddress = myLocation.getAsString("resolvedAddress"); 
				if (resolvedAddress != null) {
					location.setText(resolvedAddress);
				}
			}
		}
		
		mapView.setBuiltInZoomControls(true);
		mapView.setReticleDrawMode(ReticleDrawMode.DRAW_RETICLE_OVER);
		if (!myLocation.containsKey("_id") || myLocation.getAsLong("_id") == -1) {
			if (true) {
				new Handler().postDelayed(new Runnable(){
					@Override
					public void run() {
						myLocationOverlay = new FixedMyLocationOverlay(LocationActivity.this, mapView){
							@Override
							protected boolean dispatchTap() {
								GeoPoint point = removeMyLocationOverlay();
								initializeLocationOverlay(point);
								return true;
							}
						};
						myLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
							mapView.getController().animateTo(myLocationOverlay.getMyLocation());
						}});
						mapView.getOverlays().add(myLocationOverlay);
						mapView.getController().setZoom(18);
						myLocationOverlay.enableMyLocation();
						Toast toast = Toast.makeText(LocationActivity.this, R.string.tap_location, Toast.LENGTH_LONG);
						toast.show();
					}
	        	}, 1000);
        	} else {
        		initializeLocationOverlay(mapView.getMapCenter());
        	}
        } else {
        	int lat = (int)(myLocation.getAsDouble("latitude") * 1E6);
        	int lon = (int)(myLocation.getAsDouble("longitude") * 1E6);
        	GeoPoint point = new GeoPoint(lat, lon);
        	initializeLocationOverlay(point);
        }
                  
        Button bOK = (Button)findViewById(R.id.okButton);
        bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				EditText name = (EditText)findViewById(R.id.name);
				if (Utils.checkEditText(name, "name", true, 100)) {
					new SaveLocationTask().execute(Utils.text(name));
				}
			}
        });
    }
    
    private class SaveLocationTask extends AsyncTask<String, Integer, Void> {
		@Override
		protected Void doInBackground(String... params) {
			GeoPoint p = null;
			String provider = null;
			float accuracy = 0;
			if (myLocationOverlay != null) {
				p = myLocationOverlay.getMyLocation();
				Location lastFix = myLocationOverlay.getLastFix();
				if (lastFix != null) {
					provider = lastFix.getProvider();
					accuracy = lastFix.getAccuracy();
				}
			} else if (locationOverlay != null) {
				LocationItem li = locationOverlay.getItem(0);
				p = li.getPoint();
				provider = "manual";
			} 
			if (p == null) {
				p = mapView.getMapCenter();
				provider = "map";
			}
			myLocation.put("name", params[0]);
			myLocation.put("accuracy", accuracy);
			myLocation.put("provider", provider);
			myLocation.put("latitude", (double)p.getLatitudeE6() / 1E6);
			myLocation.put("longitude", (double)p.getLongitudeE6() / 1E6);
			myLocation.put("datetime", System.currentTimeMillis());
			publishProgress(R.string.resolving_address);
			AddressGeocoder geocoder = new AddressGeocoder(LocationActivity.this);
			myLocation.put("resolved_address",
				geocoder.resolveAddressFromLocation(
					myLocation.getAsDouble("latitude"),
					myLocation.getAsDouble("longitude")
				));
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			long id = new LocationRepository(new DatabaseHelper(LocationActivity.this).getWritableDatabase()).insert(myLocation);
			Intent data = new Intent();
			data.putExtra(LOCATION_ID_EXTRA, id);
			setResult(RESULT_OK, data);
			finish();
		}

		@Override
		protected void onPreExecute() {
			findViewById(R.id.name).setEnabled(false);
			findViewById(R.id.okButton).setEnabled(false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			Toast toast = Toast.makeText(LocationActivity.this, values[0], Toast.LENGTH_SHORT);
			toast.show();
		}
		
    }

    private void initializeLocationOverlay(GeoPoint point) {
    	if (point == null) {
    		point = mapView.getMapCenter();
    	}
    	LocationItem locationItem = new LocationItem(point, "", "");
        Drawable drawable = getResources().getDrawable(R.drawable.marker);
        locationOverlay = new LocationOvelay(drawable);
        locationOverlay.setItem(locationItem);
        mapView.getController().setCenter(locationItem.getPoint());
        mapView.getOverlays().add(locationOverlay);
	}
    
	private GeoPoint removeMyLocationOverlay() {
		myLocationOverlay.disableMyLocation();
		mapView.getOverlays().remove(myLocationOverlay);
		GeoPoint point = myLocationOverlay.getMyLocation();
		myLocationOverlay = null;
		return point;
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menuItem = menu.add(0, MENU_SATELLITE, 0, 
				mapView.isSatellite() ? R.string.satellite_off : R.string.satellite_on);
		menuItem.setIcon(android.R.drawable.ic_menu_mapmode);
		menuItem = menu.add(0, MENU_MOVE_MARKER_TO_MAP_CENTER, 0, R.string.move_marker_to_center);
		menuItem.setIcon(R.drawable.ic_menu_goto);
		menuItem = menu.add(0, MENU_GO_TO_MARKER, 0, R.string.move_go_to_marker);
		menuItem.setIcon(android.R.drawable.ic_menu_revert);		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_SATELLITE:
			if (mapView.isSatellite()) {
				item.setTitle(R.string.satellite_on);
				mapView.setSatellite(false);
			} else {
				item.setTitle(R.string.satellite_off);
				mapView.setSatellite(true);
			}
			break;
		case MENU_MOVE_MARKER_TO_MAP_CENTER:
			if (myLocationOverlay != null) {
				removeMyLocationOverlay();
			}
			if (locationOverlay == null) {
				initializeLocationOverlay(mapView.getMapCenter());
			} else {
				LocationItem locationItem = new LocationItem(mapView.getMapCenter(), "", "");
				locationOverlay.setItem(locationItem);
				mapView.invalidate();
			}
			break;
		case MENU_GO_TO_MARKER:
			if (myLocationOverlay != null) {
				GeoPoint myLocation = myLocationOverlay.getMyLocation();
				if (myLocation != null) {
					mapView.getController().animateTo(myLocation);
				}
			} else if (locationOverlay != null) {
				GeoPoint p = locationOverlay.getItem(0).getPoint();
				if (p != null) {
					mapView.getController().animateTo(p);
				}
			}
			break;
		}
		return false;
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        if (myLocationOverlay != null) {
        	myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onStop() {
    	if (myLocationOverlay != null) {
    		myLocationOverlay.disableMyLocation();
    	}
        super.onStop();
    }	
	
    private class LocationItem extends OverlayItem {

		public LocationItem(GeoPoint point, String title, String snippet) {
			super(point, title, snippet);
		}
    	
    }
    
    private class LocationOvelay extends ItemizedOverlay<LocationItem> {
    	final int wd2, h;
    	LocationItem locationItem;
    	
		public LocationOvelay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));	
			h = defaultMarker.getIntrinsicHeight();
			wd2 = defaultMarker.getIntrinsicWidth()/2;
		}
		
		public void setItem(LocationItem locationItem) {
			this.locationItem = locationItem;
			populate();
		}

		@Override
		protected LocationItem createItem(int i) {
			return locationItem;
		}

		@Override
		public int size() {
			return locationItem != null ? 1 : 0;
		}

		private boolean capturedItem = false;
		private Point p;
		private int dx;
		private int dy;

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				if (locationItem != null) {
					p = mapView.getProjection().toPixels(locationItem.getPoint(), p);
					int x = (int)event.getX();
					int y = (int)event.getY();
					if (x > p.x-wd2 && x < p.x+wd2 && y > p.y-h && y < p.y) {					
						capturedItem = true;
						dx = x - p.x;
						dy = p.y - y;
						vibrator.vibrate(40);
					} else {
						capturedItem = false;
					}
				}
			} else if (action == MotionEvent.ACTION_UP) {
				capturedItem = false;
			} else if (action == MotionEvent.ACTION_MOVE) {
				if (capturedItem) {
					GeoPoint p = mapView.getProjection().fromPixels((int)event.getX()-dx, (int)event.getY()+dy);
					locationItem = new LocationItem(p, "", "");
					populate();
					location.setText(p.toString());
					return true;
				}
			}
			return super.onTouchEvent(event, mapView);
		}
	}
}