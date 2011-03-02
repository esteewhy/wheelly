package com.wheelly.activity;

import ru.orangesoftware.financisto.view.NodeInflater;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.MileageRepository;

public class MileageListActivity extends ListActivity {

	protected static final int EDIT_MILEAGE_REQUEST = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.mileage_list);
		Cursor cursor = new MileageRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		super.startManagingCursor(cursor);
		super.setListAdapter(
			new SimpleCursorAdapter(this, R.layout.generic_list_item, cursor,
				new String[] {
					"name", "mileage", "calc_cost", "start_time", "calc_amount"
				},
				new int[] {
					R.id.name, R.id.mileage, R.id.cost, R.id.date, R.id.consumption
				}
			)
		);
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.mileage_info, null);
		final ContentValues values = new MileageRepository(new DatabaseHelper(this).getReadableDatabase()).load(id);
		
		NodeInflater ni = new NodeInflater(inflater);
		LinearLayout layout = (LinearLayout)v.findViewById(R.id.list);
		
		ni.new Builder(layout, R.layout.select_entry_simple)
			.withLabel("Started at")
			.withData(values.getAsString("start_time"))
			.create();
		
		ni.new Builder(layout, R.layout.select_entry_simple)
			.withLabel("Finished at")
			.withData(values.getAsString("stop_time"))
			.create();
		
		ni.new Builder(layout, R.layout.select_entry_simple)
			.withLabel("Mileage")
			.withData(values.getAsString("mileage"))
			.create();
		
		View titleView = inflater.inflate(R.layout.mileage_info_title, null);
		((TextView)titleView.findViewById(R.id.label)).setText(values.getAsString("name"));
		
		final Dialog dialog = new AlertDialog.Builder(this)
			.setCustomTitle(titleView)
			.setView(v)
			.create();
		
		dialog.setCanceledOnTouchOutside(true);
		
		((Button)v.findViewById(R.id.bEdit)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
				Intent intent = new Intent(MileageListActivity.this, MileageActivity.class);
				intent.putExtra(MileageActivity.MILEAGE_ID_EXTRA, id);
				intent.putExtra("values", values);
				MileageListActivity.this.startActivityForResult(intent, EDIT_MILEAGE_REQUEST);
			}
		});
	
		((Button)v.findViewById(R.id.bClose)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}
}