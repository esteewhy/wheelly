package com.wheelly.app;

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
public class FilterButton extends Fragment {
	private static final int FILTER_REQUEST = 6;
	final private ContentValues filter = new ContentValues();
	ImageButton btn;
	public String locationConstraint = null; 
	
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState) {
		btn = new ImageButton(this.getActivity());
		btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		btn.setImageResource(R.drawable.ic_menu_filter_off);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btn.setEnabled(false);
				Intent intent = new Intent(getActivity(), Filter.class);
				filter.put(F.LOCATION_CONSTRAINT, locationConstraint);
				FilterUtils.filterToIntent(filter, intent);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		return btn;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case FILTER_REQUEST:
			if (resultCode == Activity.RESULT_FIRST_USER) {
				filter.clear();
			} else if (resultCode == Activity.RESULT_OK) {
				FilterUtils.intentToFilter(data, filter);
			}
			filter.remove(F.LOCATION_CONSTRAINT);
			btn.setImageResource(filter.size() == 0 ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
			
			if(null != listener) {
				listener.onFilterChanged(filter);
			}
			
			btn.setEnabled(true);
			break;
		}
	}
	
	public ContentValues getFilter() {
		return filter;
	}
	
	public void setFilter(ContentValues filter) {
		this.filter.clear();
		this.filter.putAll(filter);
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