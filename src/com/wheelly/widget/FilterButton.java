package com.wheelly.widget;

import com.wheelly.IFilterHolder;
import com.wheelly.R;
import com.wheelly.activity.FilterDialog;
import com.wheelly.util.FilterUtils.F;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;

/**
 * Self-contained control which presents filter dialog and notifies underlying activity when filter values change.
 */
public class FilterButton extends ImageButton implements IFilterHolder {
	
	public FilterButton(Context context) {
		super(context);
		initialize();
	}
	
	public FilterButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public FilterButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	private void initialize() {
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		setImageResource(R.drawable.ic_menu_filter_off);
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setEnabled(false);
				filter.put(F.LOCATION_CONSTRAINT, locationConstraint);
				new FilterDialog(new ContentValues(filter), new OnFilterChangedListener() {
					
					@Override
					public void onFilterChanged(ContentValues value) {
						if (null == value) {
							setFilter(null);
						} else {
							filter.remove(F.LOCATION_CONSTRAINT);
							setFilter(value);
						}
						
						setEnabled(true);
					}
				})
					.show(((FragmentActivity)getContext()).getSupportFragmentManager(), "filter");
			}
		});
	}
	
	private final ContentValues filter = new ContentValues();
	public String locationConstraint = null; 
	
	@Override
	public Parcelable onSaveInstanceState() {
		Bundle outState = new Bundle();
		outState.putString(F.LOCATION_CONSTRAINT, locationConstraint);
		outState.putParcelable("instanceState", super.onSaveInstanceState());
		return outState;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if(state instanceof Bundle) {
			final Bundle savedInstanceState = (Bundle)state;
			locationConstraint = savedInstanceState.getString(F.LOCATION_CONSTRAINT);
			super.onRestoreInstanceState(savedInstanceState.getParcelable("instanceState"));
		} else {
			super.onRestoreInstanceState(state);
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
			
			setImageResource(reset ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
			
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