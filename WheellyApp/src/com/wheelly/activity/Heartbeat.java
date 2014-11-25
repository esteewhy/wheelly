package com.wheelly.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.view.menu.MenuDialogHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Spinner;
import android.widget.TextView;

import com.wheelly.R;
import com.wheelly.app.HeartbeatInput;
import com.wheelly.db.HeartbeatBroker;

/**
 * Complete heartbeat editing UI.
 */
public class Heartbeat extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.heartbeat_edit);
		final Intent intent = getIntent();
		final ContentValues heartbeat =
			intent.hasExtra("heartbeat")
				? (ContentValues)intent.getParcelableExtra("heartbeat")
				: new HeartbeatBroker(this)
					.loadOrCreate(intent.getLongExtra(BaseColumns._ID, 0));
		
		final Controls c = new Controls(this);
		c.Heartbeat.setValues(heartbeat);
		c.SaveButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					final ContentValues values = c.Heartbeat.getValues();
					intent.putExtra(BaseColumns._ID,
						new HeartbeatBroker(Heartbeat.this).updateOrInsert(values));
					intent.putExtra("heartbeat", values);
					
					setResult(RESULT_OK, intent);
					finish();
				}
			});
		
		c.CancelButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						setResult(RESULT_CANCELED);
						finish();
					}
				});
		
		this.typeSelector = createTypeSelector(heartbeat.getAsInteger("type"),
			new OnItemSelectedListener() {
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
		final List<Map<String, Integer>> list = new ArrayList<Map<String, Integer>>();
		list.add(new HashMap<String, Integer>() {{
			put("type", 1);
			put("icon", R.drawable.hb_start);
			put("text", R.string.start);
		}});
		
		list.add(new HashMap<String, Integer>() {{
			put("type", 2);
			put("icon", R.drawable.hb_stop);
			put("text", R.string.stop);
		}});
		
		list.add(new HashMap<String, Integer>() {{
			put("type", 4);
			put("icon", R.drawable.hb_refuel);
			put("text", R.string.refuels);
		}});
		
		final Spinner types = new Spinner(this);
		final SimpleAdapter adapter = new SimpleAdapter(this, list,
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
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem mi = menu.add(1, 0, 0, "Type");
		MenuItemCompat.setActionView(mi, typeSelector);
		MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * Holds UI controls references.
	 */
	private class Controls {
		final HeartbeatInput Heartbeat;
		final View SaveButton;
		final View CancelButton;
		
		public Controls(FragmentActivity view) {
			Heartbeat		= (HeartbeatInput)getSupportFragmentManager().findFragmentById(R.id.heartbeat);
			SaveButton		= (View)view.findViewById(R.id.bSave);
			CancelButton	= (View)view.findViewById(R.id.bSaveAndNew);
		}
	}
}