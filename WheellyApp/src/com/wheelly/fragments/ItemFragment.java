package com.wheelly.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.issue40537.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import com.wheelly.R;

public abstract class ItemFragment extends Fragment {
	protected OnClickListener onSave;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.item_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.bSave:
			if(null != onSave) {
				onSave.onClick(null);
				return true;
			}
			break;
		case android.R.id.home:
			getActivity().setResult(Activity.RESULT_CANCELED);
			getActivity().finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}