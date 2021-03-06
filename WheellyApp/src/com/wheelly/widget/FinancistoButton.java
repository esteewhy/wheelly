package com.wheelly.widget;

import com.wheelly.R;
import com.wheelly.content.TransactionRepository;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.issue40537.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public final class FinancistoButton extends Fragment {
	private static final int NEW_TRANSACTION_REQUEST = 1;

	private Button SyncButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		final View v = inflater.inflate(R.layout.financisto_sync, container, true);
		
		SyncButton = (Button)v.findViewById(R.id.btn_financisto);
		SyncButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				long id = getValue();
				
				Intent intent = new Intent(TransactionRepository.FINANCISTO_ACTION);
				if(id > 0) {
					intent.putExtra("tranId", id);
				}
				prepopulate(intent);
				startActivityForResult(intent, NEW_TRANSACTION_REQUEST);
			}
		});
		
		return v;
	}
	
	private void prepopulate(Intent intent) {
		final ContentValues values = getValues();
		
		if(null == values) {
			return;
		}
		
		if(values.containsKey("cost")) {
			intent.putExtra("accountAmount", (long)(values.getAsFloat("cost") * 100));
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == NEW_TRANSACTION_REQUEST) {
			setValue(data.getLongExtra("_id", 0));
		}
	}
	
	public long getValue() {
		return (Long)SyncButton.getTag(R.id.tag_transaction_id);
	}
	
	public void setValue(long transactionId) {
		SyncButton.setText(
			Html.fromHtml(
				String.format(
					getResources().getString(
						transactionId > 0
							? R.string.transaction_edit
							: R.string.transaction_create),
					transactionId))
		);
		SyncButton.setTag(R.id.tag_transaction_id, transactionId);
	}
	
	public ContentValues getValues() {
		return (ContentValues)SyncButton.getTag(R.id.tag_transaction_values);
	}
	
	public void setValues(ContentValues values) {
		SyncButton.setTag(R.id.tag_transaction_values, values);
	}
}