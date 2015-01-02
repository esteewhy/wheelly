package com.wheelly.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.issue40537.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wheelly.R;
import com.wheelly.db.HeartbeatBroker;
import com.wheelly.fragments.ItemFragment.OnFragmentResultListener;

public class DispatchFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dispatch, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Bundle args = getArguments();
		
		if(null != args) {
			showItem();
		}
	}
	
	private OnFragmentResultListener finishListener;
	public void setOnFinishedListener(OnFragmentResultListener listener) {
		finishListener = listener;
	}
	
	
	Bundle uiArgs;
	public void setUIArguments(Bundle args) {
		uiArgs = args;
	}
	
	public void showItem() {
		final Bundle args = null != uiArgs ? uiArgs : getArguments();
		int type = 0;
		if(null != args) {
			final long id = args.getLong(BaseColumns._ID);
			final ContentValues values =
				id > 0
					? new HeartbeatBroker(getActivity()).loadOrCreate(id)
					: null;
			type =
				null != values && values.getAsLong(BaseColumns._ID) == id
					? values.getAsInteger("type")
					: args.getInt("type");
		}
		
		ItemFragment f =
				(type & 1) != 0
					? new StartFragment()
					: (type & 2) != 0
						? new StopFragment()
						: (type & 4) != 0
							? new RefuelFragment()
							: new HeartbeatFragment();
		f.setArguments(args);
		f.setOnFinishedListener(finishListener);
		getFragmentManager()
			.beginTransaction()
			.replace(R.id.item_container, f)
			.commit();
	}
}