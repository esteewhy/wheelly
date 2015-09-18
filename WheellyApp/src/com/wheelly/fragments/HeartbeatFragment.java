package com.wheelly.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;

public class HeartbeatFragment extends ItemFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.heartbeat_edit, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Bundle args = getArgumentsOrDefault();
		final ContentValues heartbeat =
			args.containsKey("heartbeat")
				? (ContentValues)args.getParcelable("heartbeat")
				: new HeartbeatBroker(getActivity())
					.loadOrCreate(args.getLong(BaseColumns._ID, -1));
		
		final Controls c = new Controls();
		
		c.Heartbeat.setValues(heartbeat);
		onSave =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues values = c.Heartbeat.getValues();
					args.putLong(BaseColumns._ID,
						new HeartbeatBroker(getActivity()).updateOrInsert(values));
					args.putParcelable("heartbeat", values);
					
					finish(args);
				}
			};
		
		this.typeSelector = createTypeSelector(heartbeat.getAsInteger("type"),
			new OnItemSelectedListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					heartbeat.put("type", ((Map<String, Integer>)((SimpleAdapter)parent.getAdapter()).getItem(position)).get("type"));
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
	}
	
	private View typeSelector;
	
	private View createTypeSelector(int initial, OnItemSelectedListener listener) {
		@SuppressWarnings("serial")
		final List<Map<String, Integer>> list = new ArrayList<Map<String, Integer>>() {{
			add(new HashMap<String, Integer>() {{
				put("type", 1);
				put("icon", R.drawable.hb_start);
				put("text", R.string.start);
			}});
			add(new HashMap<String, Integer>() {{
				put("type", 2);
				put("icon", R.drawable.hb_stop);
				put("text", R.string.stop);
			}});
			add(new HashMap<String, Integer>() {{
				put("type", 4);
				put("icon", R.drawable.hb_refuel);
				put("text", R.string.refuels);
			}});
		}};
		
		final Spinner types = new Spinner(getActivity());
		final SimpleAdapter adapter = new SimpleAdapter(getActivity(), list,
				R.layout.icon_list_item,
				new String[] { "icon", "text" },
				new int[] { android.R.id.icon, android.R.id.text1 }
			);
		types.setAdapter(adapter);
		
		for(Map<String, Integer> item: list) {
			if(initial == item.get("type")) {
				types.setSelection(list.indexOf(item));
				break;
			}
		}
		
		types.setOnItemSelectedListener(listener);
		
		return types;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		final MenuItem mi = menu.add(1, 0, 0, "Type");
		MenuItemCompat.setActionView(mi, typeSelector);
		MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	private class Controls {
		final HeartbeatInput Heartbeat;
		
		public Controls() {
			Heartbeat		= (HeartbeatInput)getChildFragmentManager().findFragmentById(R.id.heartbeat);
		}
	}
}