package com.wheelly.app;

import ru.orangesoftware.financisto.utils.Utils;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.wheelly.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Recorded track selection (using MyTracks app as a source).
 */
public final class TrackInput extends Fragment {
	
	private long selectedTrackId = 0;
	private Controls c;
	private Cursor tracksCursor;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Activity ctx = getActivity();
		
		tracksCursor = ctx.getContentResolver().query(
				TracksColumns.CONTENT_URI, null, null, null, "_id DESC");
		
		ctx.startManagingCursor(tracksCursor);
		final ListAdapter adapter =
			new SimpleCursorAdapter(ctx,
					android.R.layout.simple_spinner_dropdown_item,
					tracksCursor, 
					new String[] {"name"},
					new int[] { android.R.id.text1 }
			);
		
		View v = inflater.inflate(R.layout.select_entry_plus, container, true);
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(ctx)
					.setSingleChoiceItems(adapter,
						Utils.moveCursor(tracksCursor, BaseColumns._ID, selectedTrackId),
						new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								tracksCursor.moveToPosition(which);	
								long selectedId = tracksCursor.getLong(tracksCursor.getColumnIndexOrThrow("_id")); 
								selectedTrackId = selectedId;
							}
						}
					)
					.setTitle(R.string.tracks)
					.show();
			}
		});
		
		c = new Controls(v);
		c.labelView.setText(R.string.track);
		c.locationAdd.setVisibility(View.GONE);
		return v;
	}
	
	public long getValue() {
		return selectedTrackId;
	}
	
	public void setValue(long trackId) {
		if (trackId <= 0) {
			selectedTrackId = trackId;
		} else {
			c.locationText.setText(Long.toString(trackId));
			selectedTrackId = trackId;
		}
	}
	
	private static class Controls {
		final TextView locationText;
		final TextView labelView;
		final ImageView locationAdd;
		
		public Controls(View v) {
			this.locationText	= (TextView)v.findViewById(R.id.data);
			this.labelView		= (TextView)v.findViewById(R.id.label);
			this.locationAdd	= (ImageView)v.findViewById(R.id.plus_minus);
		}
	}
}