package com.wheelly.app;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

import java.util.List;

import com.wheelly.R;

import ru.orangesoftware.financisto.utils.ThumbnailUtil;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;
import ru.orangesoftware.financisto.view.NodeInflater.Builder;
import ru.orangesoftware.financisto.view.NodeInflater.PictureBuilder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MileageInfoDialogFragment extends DialogFragment {
	
	private ContentValues mileage;
	
	public void showDialog(ContentValues mileage) {
		this.mileage = mileage;
		this.show(getFragmentManager(), "mileage");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.mileage_info, container);
		LinearLayout layout = (LinearLayout)v.findViewById(R.id.list);
		
		View titleView = inflater.inflate(R.layout.mileage_info_title, null);
		TextView titleLabel = (TextView)titleView.findViewById(R.id.label);
		TextView titleData = (TextView)titleView.findViewById(R.id.data);
		ImageView titleIcon = (ImageView)titleView.findViewById(R.id.icon);
		
		titleIcon.setImageResource(R.drawable.mileage);
		/*
		add(layout, R.string.date, DateUtils.formatDateTime(
				getActivity(),
				ti.dateTime,
				DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_YEAR),
				ti.attachedPicture);
		
        Project project = ti.project;
        if (project != null && project.id > 0) {
            add(layout, R.string.project, project.title);
        }

        if (!Utils.isEmpty(ti.note)) {
            add(layout, R.string.note, ti.note);
        }

        MyLocation location = ti.location;
        String locationName;
        if (location != null && location.id > 0) {
            locationName = location.name+(location.resolvedAddress != null ? " ("+location.resolvedAddress+")" : "");
            add(layout, R.string.location, locationName);
        }

		final Dialog d = new AlertDialog.Builder(parentActivity)
			.setCustomTitle(titleView)
			.setView(v)
			.create();
		d.setCanceledOnTouchOutside(true);

		Button bEdit = (Button)v.findViewById(R.id.bEdit);
		bEdit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				d.dismiss();
				//parentActivity.editItem(position, id);
			}
		});
		
		Button bClose = (Button)v.findViewById(R.id.bClose);
		bClose.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				d.dismiss();
			}
		});
*/
		//d.show();
		return v;
	}
/*
	private void add(LinearLayout layout, int labelId, String data, AccountType accountType) {
		inflater.new Builder(layout, R.layout.select_entry_simple_icon)
			.withIcon(accountType.iconId).withLabel(labelId).withData(data).create();
	}

	private void add(LinearLayout layout, int labelId, String data) {
		inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(labelId)
			.withData(data).create();
	}

	private void add(LinearLayout layout, String label, String data) {
		inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(label)
			.withData(data).create();
	}*/
}