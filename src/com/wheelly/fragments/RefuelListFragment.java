package com.wheelly.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelly.R;
import com.wheelly.activity.Refuel;
import com.wheelly.app.ListConfiguration;
import com.wheelly.content.TransactionRepository;
import com.wheelly.db.DatabaseSchema.Refuels;
import com.wheelly.fragments.InfoDialogFragment.Options;

public class RefuelListFragment extends ConfigurableListFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.refuel_list, null);
	}
	
	@Override
	protected ListConfiguration configure() {
		ListConfiguration cfg = new ListConfiguration() {
			@Override
			public SimpleCursorAdapter createListAdapter(Context context) {
				return
					new SimpleCursorAdapter(context, R.layout.refuel_list_item, null,
						new String[] {
							"name", "mileage", "cost", "_created", "amount", "place"
						},
						new int[] {
							R.id.place, R.id.mileage, R.id.cost, R.id.date, R.id.fuel, R.id.place
						},
						0
					) {
						@Override
						public void setViewText(TextView v, String text) {
							switch(v.getId()) {
							case R.id.date:
								v.setText(com.wheelly.util.DateUtils.formatVarying(text));
								break;
							case R.id.mileage:
							case R.id.fuel:
								v.setText(TextUtils.isEmpty(text) ? text : String.format("%+.2f", Float.parseFloat(text)));
								break;
							default: super.setViewText(v, text);
							}
						}
					};
			}
			
			@Override
			public Options configureViewItemDialog() {
				return
					new InfoDialogFragment.Options() {{
						
						fields.put(R.string.odometer_input_label, "mileage");
						fields.put(R.string.fuel_input_label, "amount");
						fields.put(R.string.amount, "cost");
						
						titleField = "place";
						dataField = "_created";
						iconResId = R.drawable.ic_tab_utensils_selected;
					}};
			}
			
			@Override
			public CursorLoader createViewItemCursorLoader(Context context, long id) {
				return
					new CursorLoader(
						context,
						Refuels.CONTENT_URI,
						Refuels.ListProjection,
						"f." + BaseColumns._ID + " = ?",
						new String[] { Long.toString(id) },
						"f." + BaseColumns._ID + " DESC LIMIT 1"
					);
			}
		};
		
		cfg.ConfirmDeleteResourceId = R.string.delete_refuel_confirm;
		cfg.EmptyTextResourceId = R.string.no_refuels;
		cfg.ItemActivityClass = Refuel.class;
		cfg.LocationFacetTable = "refuels";
		cfg.ContentUri = Refuels.CONTENT_URI;
		cfg.ListProjection = Refuels.ListProjection;
		cfg.FilterExpr = Refuels.FilterExpr;
		
		return cfg;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.refuels_menu, menu);
		if(!new TransactionRepository(getActivity()).checkAvailability()) {
			menu.findItem(R.id.opt_menu_install_financisto).setVisible(true);
			Toast.makeText(getActivity(), R.string.advertise_financisto, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(R.string.refuels);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.opt_menu_install_financisto:
			Intent marketIntent = new Intent(Intent.ACTION_VIEW)
				.setData(Uri.parse("market://details?id=ru.orangesoftware.financisto"));
			startActivity(marketIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}