package com.wheelly.app;

import com.wheelly.IFilterHolder;
import com.wheelly.R;
import com.wheelly.activity.Filter;
import com.wheelly.util.FilterUtils;
import com.wheelly.util.FilterUtils.F;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;

/**
 * Self-contained control which presents filter dialog and notifies underlying activity when filter values change.
 */
public class FilterButton extends Fragment implements IFilterHolder {
	private static final int FILTER_REQUEST = 6;
	private final ContentValues filter = new ContentValues();
	ImageButton btn;
	public String locationConstraint = null; 
	
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState) {
		
		if(null != savedInstanceState && savedInstanceState.containsKey(F.LOCATION_CONSTRAINT)) {
			locationConstraint = savedInstanceState.getString(F.LOCATION_CONSTRAINT);
		}
		
		btn = new ImageButton(this.getActivity());
		btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		btn.setImageResource(R.drawable.ic_menu_filter_off);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btn.setEnabled(false);
				Intent intent = new Intent(getActivity(), Filter.class);
				FilterUtils.filterToIntent(filter, intent);
				intent.putExtra(F.LOCATION_CONSTRAINT, locationConstraint);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		return btn;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(F.LOCATION_CONSTRAINT, locationConstraint);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case FILTER_REQUEST:
			if (resultCode == Activity.RESULT_FIRST_USER) {
				setFilter(null);
			} else if (resultCode == Activity.RESULT_OK) {
				data.removeExtra(F.LOCATION_CONSTRAINT);
				ContentValues newFilter = new ContentValues();
				FilterUtils.intentToFilter(data, newFilter);
				setFilter(newFilter);
			}
			
			btn.setEnabled(true);
			break;
		}
	}
	
	public ContentValues getFilter() {
		return filter;
	}
	
	public void setFilter(ContentValues filter) {
		if(!this.filter.equals(filter)
				&& (this.filter.size() > 0 || null != filter)) {
			boolean reset = null == filter || 0 == filter.size();
			
			this.filter.clear();
			
			if(!reset) {
				this.filter.putAll(filter);
			}
			
			btn.setImageResource(reset ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
			
			if(null != listener) {
				listener.onFilterChanged(this.filter);
			}
		}
	}
	
	public String getLocationConstraint() {
		return locationConstraint;
	}
	
	public void setLocationConstraint(String locationConstraint) {
		this.locationConstraint = locationConstraint;
	}
	
	private OnFilterChangedListener listener;
	
	public void SetOnFilterChangedListener(OnFilterChangedListener listener) {
		this.listener = listener;
	}
	
	public static interface OnFilterChangedListener {
		void onFilterChanged(ContentValues value);
	}
}