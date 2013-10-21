package com.wheelly.fragments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ru.orangesoftware.financisto.view.NodeInflater;

import com.wheelly.R;
import android.content.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Generic dialog to display textual properties, and "Edit", "Cancel" buttons.
 * 
 * Configured via jQuery-like 'options' structure.
  */
public class InfoDialogFragment extends DialogFragment {
	private LayoutInflater layoutInflater;
	private NodeInflater nodeInflater;
	private final Options options;
	
	public InfoDialogFragment(Options options) {
		this.options = options;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		nodeInflater = new NodeInflater(layoutInflater);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View v = layoutInflater.inflate(R.layout.info_dialog, null);
		final LinearLayout layout = (LinearLayout)v.findViewById(R.id.list);
		
		final View titleView = layoutInflater.inflate(R.layout.info_dialog_title, null);
		final TextView titleLabel = (TextView)titleView.findViewById(R.id.label);
		final TextView titleData = (TextView)titleView.findViewById(R.id.data);
		final ImageView titleIcon = (ImageView)titleView.findViewById(R.id.icon);
		
		getLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				return options.loader;
			}
			
			@Override
			public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(!data.moveToFirst()) {
							return;
						}
						
						titleLabel.setText(data.getString(data.getColumnIndexOrThrow(options.titleField)));
						titleData.setText(data.getString(data.getColumnIndexOrThrow(options.dataField)));
						titleIcon.setImageResource(options.iconResId);
						
						for(Entry<Integer, String> field : options.fields.entrySet()) {
							add(layout, field.getKey(), data.getString(data.getColumnIndexOrThrow(field.getValue())));
						}
					}
				});
			}
			
			@Override
			public void onLoaderReset(Loader<Cursor> loader) {
			}
		});
		
		final Dialog d = new AlertDialog.Builder(getActivity())
			.setCustomTitle(titleView)
			.setView(v)
			.create();
		
		d.setCanceledOnTouchOutside(true);

		Button bEdit = (Button)v.findViewById(R.id.bEdit);
		bEdit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dismiss();
				
				if(null != options.onClickListener) {
					options.onClickListener.onClick(d, Dialog.BUTTON_POSITIVE);
				}
			}
		});
		
		Button bClose = (Button)v.findViewById(R.id.bClose);
		bClose.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dismiss();
				
				if(null != options.onClickListener) {
					options.onClickListener.onClick(d, Dialog.BUTTON_NEUTRAL);
				}
			}
		});
		
		return d;
	}
	
	private void add(LinearLayout layout, int labelId, String data) {
		nodeInflater.new Builder(layout, R.layout.select_entry_simple)
			.withLabel(labelId)
			.withData(data)
			.create();
	}
	
	public static class Options {
		/**
		 * Hash of 'label resource id' : 'string value' to display in a property grid.
		 */
		public final Map<Integer, String> fields = new LinkedHashMap<Integer, String>();
		/**
		 * Lazy loaded cursor containing a single record to display.
		 */
		public CursorLoader loader;
		/**
		 *  Listener for Edit and Cancel buttons.
		 */
		public DialogInterface.OnClickListener onClickListener;
		/**
		 *  Icon resource to display in a title.
		 */
		public int iconResId;
		/**
		 * Cursor column to use as a main title.
		 */
		public String titleField;
		/**
		 * Sub-title cursor column.
		 */
		public String dataField;
	}
}