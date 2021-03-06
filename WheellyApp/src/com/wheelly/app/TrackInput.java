package com.wheelly.app;

import ru.orangesoftware.financisto.utils.Utils;

import com.wheelly.R;
import com.wheelly.content.TrackRepository;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.issue40537.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

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
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Activity ctx = getActivity();
		
		final View v = inflater.inflate(R.layout.select_entry_plus, container, true);
		
		// prepend MyTracks icon
		((ViewGroup)v).addView(new ImageView(ctx) {{
			setImageResource(R.drawable.arrow_icon);
		}}, 0, new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		
		
		try {
			tracksCursor = new TrackRepository(ctx).list();
		}
		catch(SecurityException e) {
		}
		
		//@todo implement notification about disabled sharing in MyTracks
		if(null != tracksCursor) {
			populateTracks(ctx, v, tracksCursor);
		}
		
		c = new Controls(v);
		c.labelView.setText(R.string.track);
		c.locationAdd.setVisibility(View.GONE);
		setValue(0);//force default label
		return v;
	}

	private void populateTracks(final Activity ctx, final View v, final Cursor tracksCursor) {
			ctx.startManagingCursor(tracksCursor);
			@SuppressWarnings("deprecation")
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
		
	}
	
	public long getValue() {
		return selectedTrackId;
	}
	
	public void setValue(final long trackId) {
		if(selectedTrackId != trackId && null != tracksCursor) {
			if(trackId != 0 && Utils.moveCursor(tracksCursor, "_id", trackId) != -1) {
				c.locationText.setText(tracksCursor.getString(tracksCursor.getColumnIndexOrThrow("name")));
				((View)c.labelView.getParent()).setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						Intent intent = new Intent();
						intent.setData(
							Uri.withAppendedPath(Uri.parse("content://nl.sogeti.android.gpstracker/tracks/"), Long.toString(trackId))
						);
						intent.setClassName("nl.sogeti.android.gpstracker", "nl.sogeti.android.gpstracker.viewer.LoggerMap");
						try {
							startActivity(intent);
						} catch(ActivityNotFoundException e) {
							
						}
						
						return false;
					}
				});
			} else {
				c.locationText.setText(R.string.no_track);
				((View)c.labelView.getParent()).setOnLongClickListener(null);
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