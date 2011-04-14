package com.wheelly.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.wheelly.R;
import com.wheelly.db.DatabaseHelper;
import com.wheelly.db.RefuelRepository;

public class RefuelList extends ListActivity {

	private static final int NEW_MILEAGE_REQUEST = 1;
	protected static final int EDIT_MILEAGE_REQUEST = 2;
	

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.refuel_list);
		
		final Cursor cursor = new RefuelRepository(new DatabaseHelper(this).getReadableDatabase()).list();
		super.startManagingCursor(cursor);
		
		super.setListAdapter(
			new SimpleCursorAdapter(this, R.layout.refuel_list_item, cursor,
				new String[] {
					"name", "calc_mileage", "cost", "_created", "amount"
				},
				new int[] {
					R.id.name, R.id.mileage, R.id.cost, R.id.date, R.id.consumption
				}
			)
		);
		
		((ImageButton)findViewById(R.id.bAdd)).setOnClickListener(
			new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(RefuelList.this, Refuel.class);
					startActivityForResult(intent, NEW_MILEAGE_REQUEST);
				}
			});
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, final long id) {
		Intent intent = new Intent(RefuelList.this, Refuel.class);
		intent.putExtra(BaseColumns._ID, id);
		startActivityForResult(intent, EDIT_MILEAGE_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		((SimpleCursorAdapter)this.getListAdapter()).getCursor().requery();
	}
}