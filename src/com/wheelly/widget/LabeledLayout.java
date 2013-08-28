package com.wheelly.widget;

import com.wheelly.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LabeledLayout extends LinearLayout {
	
	private final CharSequence label; 
	
	public LabeledLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.LabeledLayout, 0, 0);
		
		CharSequence text = array.getText(R.styleable.LabeledLayout_label);
		
		if (text == null) text = getResources().getString(android.R.string.unknownName);
		label = text;
		
		array.recycle();
	}

	@Override
	protected void onFinishInflate() {
		int index = getChildCount();
		// Collect children declared in XML.
		View[] children = new View[index];
		while(--index >= 0) {
			children[index] = getChildAt(index);
		}
		// Pressumably, wipe out existing content (still holding reference to it).
		this.detachAllViewsFromParent();
		// Inflate new "template".
		final View template = LayoutInflater.from(getContext())
			.inflate(R.layout.labeled_layout, this, true);
		// Obtain reference to a new container within "template".
		final ViewGroup vg = (ViewGroup)template.findViewById(R.id.layout);
		index = children.length;
		// Push declared children into new container.
		while(--index >= 0) {
			vg.addView(children[index]);
		}
		
		((TextView)template.findViewById(R.id.label)).setText(label);
		
		// They suggest to call it no matter what.
		super.onFinishInflate();
	}
}