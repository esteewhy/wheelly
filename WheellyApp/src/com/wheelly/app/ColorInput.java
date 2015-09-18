package com.wheelly.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.wheelly.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public final class ColorInput extends DialogFragment {
	private final OnSelectColorListener listener;
	
	public ColorInput(OnSelectColorListener listener) {
		this.listener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Activity ctx = getActivity();
		
		final List<HashMap<String, Integer>> list = new ArrayList<HashMap<String, Integer>>();
		
		final TypedArray a = getResources().obtainTypedArray(R.array.colors);
		for(int i = 0; i < a.length(); i++) {
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			map.put("color", a.getColor(i, 0));
			list.add(map);
		}
		a.recycle();
		
		return
			new AlertDialog.Builder(getActivity())
				.setSingleChoiceItems(
					new SimpleAdapter(ctx, list,
						android.R.layout.simple_dropdown_item_1line,
						new String[] { "color" },
						new int[] { android.R.id.text1 }
					) {
						@Override
						public void setViewText(TextView v, String text) {
							int color = Integer.parseInt(text);
							v.setBackgroundColor(color);
							v.setText(String.format("#%06X", (0xFFFFFF & color)));
						}
					},
					-1,
					new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final int color = list.get(which).get("color");
							listener.onSelect(dialog, which, color, String.format("#%06X", (0xFFFFFF & color)));
							dialog.cancel();
						}
					}
				)
				.setTitle(R.string.location)
				.create();
	}
	
	public static interface OnSelectColorListener {
		public void onSelect(DialogInterface dialog, int which, int color, String argb);
	}
}