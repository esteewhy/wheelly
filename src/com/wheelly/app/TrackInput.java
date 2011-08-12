package com.wheelly.app;

import ru.orangesoftware.financisto.utils.Utils;

import com.wheelly.R;
import com.wheelly.content.TrackRepository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Recorded track selection (using MyTracks app as a source).
 */
public final class TrackInput extends Fragment {
	
	private long selectedTrackId = 1;
	private Controls c;
	private Cursor tracksCursor;
	
	public static interface OnTrackChangedListener {
		void onTrackChanged(long trackId); 
	}
	
	private OnTrackChangedListener onTrackChangedListener;
	
	public void setOnTrackChangedListener(OnTrackChangedListener listener) {
		onTrackChangedListener = listener;
	}
	
	protected void onTrackChanged(long trackId) {
		if(trackId != selectedTrackId) {
			if(null != onTrackChangedListener) {
				onTrackChangedListener.onTrackChanged(trackId);
			}
			setValue(trackId);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Activity ctx = getActivity();
		tracksCursor = new TrackRepository(ctx).list();
		
		final View v = inflater.inflate(R.layout.select_entry_plus, container, true);
		
		// prepend MyTracks icon
		((ViewGroup)v).addView(new ImageView(ctx) {{
			setImageResource(R.drawable.arrow_icon);
		}}, 0, new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		
		if(null != tracksCursor) {
		
			ctx.startManagingCursor(tracksCursor);
			final ListAdapter adapter =
				new SimpleCursorAdapter(ctx,
						android.R.layout.simple_spinner_dropdown_item,
						tracksCursor, 
						new String[] {"name"},
						new int[] { android.R.id.text1 }
				);
		
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
									onTrackChanged(selectedId);
								}
							}
						)
						.setTitle(R.string.tracks)
						.show();
				}
			});
		} else {
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
				        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.mytracks")));
				    } catch (ActivityNotFoundException e) {
				        Toast.makeText(getActivity(), "Market not installed. Please install MyTracks app manually.", Toast.LENGTH_SHORT).show();
				    }
				}
			});
		}
		
		c = new Controls(v);
		c.labelView.setText(R.string.track);
		c.locationAdd.setVisibility(View.GONE);
		setValue(0);//force default label
		return v;
	}
	
	public long getValue() {
		return selectedTrackId;
	}
	
	public void setValue(long trackId) {
		if(selectedTrackId != trackId) {
			if(trackId != 0 && Utils.moveCursor(tracksCursor, "_id", trackId) != -1) {
				c.locationText.setText(tracksCursor.getString(tracksCursor.getColumnIndexOrThrow("name")));
			} else {
				c.locationText.setText(R.string.no_track);
			}
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