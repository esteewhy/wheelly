package com.wheelly.app;

import ru.orangesoftware.financisto.activity.LocationActivity;
import ru.orangesoftware.financisto.utils.Utils;

import com.google.android.gms.location.LocationListener;
import com.wheelly.R;
import com.wheelly.activity.LocationsList;
import com.wheelly.db.DatabaseSchema.Locations;
import com.wheelly.db.LocationBroker;
import com.wheelly.util.LocationUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.issue40537.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * Location selection and creation control.
 */
public final class LocationInput extends Fragment {
	
	private static final int NEW_LOCATION_REQUEST = 4002;
	private static final int EDIT_LOCATION_REQUEST = 4003;
	
	private long selectedLocationId = 0;
	private Controls c;
	private Location cachedLocation;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		final Activity ctx = getActivity();
		View v = inflater.inflate(R.layout.select_location, container, true);
		
		v.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View paramView) {
				Intent intent = new Intent(ctx, LocationsList.class);
				intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, selectedLocationId);
				startActivityForResult(intent, EDIT_LOCATION_REQUEST);
				return true;
			}
		});
		
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(getActivity(), Locations.CONTENT_URI, null, null, null, null);
					}
					
					@Override
					public void onLoadFinished(Loader<Cursor> arg0, final Cursor locationCursor) {
						new AlertDialog.Builder(getActivity())
							.setSingleChoiceItems(buildAdapter(locationCursor),
								Utils.moveCursor(locationCursor, BaseColumns._ID, selectedLocationId),
								new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
										locationCursor.moveToPosition(which);
										setLocationFromCursor(locationCursor);
									}
								}
							)
							.setTitle(R.string.location)
							.show();
					}
					
					@Override
					public void onLoaderReset(Loader<Cursor> arg0) { }
				});
			}
		});
		
		c = new Controls(v);
		c.locationAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ctx, LocationActivity.class);
				startActivityForResult(intent, NEW_LOCATION_REQUEST);
			}
		});
		
		return v;
	}
	
	@SuppressLint("NewApi")
	private ListAdapter buildAdapter(Cursor c) {
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
			R.layout.location_item,
			c,
			new String[] {"name", "resolved_address", "name" },
			new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 },
			0);
		
		adapter.setViewBinder(new LocationViewBinder(cachedLocation));
		
		LocationUtils.obtainLocation(getActivity(), new LocationListener() {
			@Override
			public void onLocationChanged(Location paramLocation) {
				cachedLocation = ((LocationViewBinder)adapter.getViewBinder()).location = paramLocation;
				adapter.notifyDataSetChanged();
			}
		});
		
		return adapter;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case NEW_LOCATION_REQUEST:
				case EDIT_LOCATION_REQUEST:
					long locationId = data.getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					if (locationId > 0) {
						setValue(locationId);
					}
					break;
			}
		}
	}
	
	public long getValue() {
		return selectedLocationId;
	}
	
	public void setValue(long locationId) {
		c.labelView.setBackgroundColor(Color.TRANSPARENT);
		
		if (locationId <= 0) {
			c.labelView.setText(R.string.location_input_label);
			c.locationText.setText(R.string.no_fix);
			
			LocationUtils.obtainLocation(getActivity(), new com.google.android.gms.location.LocationListener() {
				@Override
				public void onLocationChanged(Location lastFix) {
					cachedLocation = lastFix;
					final long locationId = new LocationBroker(getActivity()).getNearest(lastFix, 10e+9f);
					
					if(locationId > 0) {
						setValue(locationId);
					} else {
						c.locationText.setText(LocationUtils.locationToText(
							lastFix.getProvider(), 
							lastFix.getLatitude(),
							lastFix.getLongitude(), 
							lastFix.hasAccuracy() ? lastFix.getAccuracy() : 0,
							null)
						);
					}

				}
			});
		} else {
			ContentValues location = new LocationBroker(getActivity()).loadOrCreate(locationId);
			
			if(location.size() > 0) {
				c.labelView.setText(location.getAsString("name"));
				c.locationText.setText(location.getAsString("resolved_address"));
				
				final String argb = location.getAsString("color");
				
				if(!TextUtils.isEmpty(argb)) {
					c.labelView.setBackgroundColor(Color.parseColor(argb));
				}
				
				selectedLocationId = locationId;
			}
		}
	}
	
	/**
	 * Attempts to select location from current cursor position.
	 */
	private void setLocationFromCursor(Cursor locationCursor) {
		ContentValues location = LocationBroker.deserialize(locationCursor);
		//c.locationText.setText(LocationUtils.locationToText(location));
		c.labelView.setText(location.getAsString("name"));
		c.locationText.setText(location.getAsString("resolved_address"));
		selectedLocationId = location.getAsLong(BaseColumns._ID);
	}
	
	private static class Controls {
		final TextView locationText;
		final TextView labelView;
		final ImageView locationAdd;
		
		public Controls(View v) {
			this.locationText	= (TextView)v.findViewById(R.id.data);
			this.labelView 		= (TextView)v.findViewById(R.id.label);
			this.locationAdd	= (ImageView)v.findViewById(R.id.plus_minus);
		}
	}
}