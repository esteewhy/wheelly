package com.wheelly.activity;

import com.google.android.maps.mytracks.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class NoAccountsDialog extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Context ctx = getActivity();
		
		return new AlertDialog.Builder(ctx)
			.setCancelable(true)
			.setMessage(R.string.send_google_no_account_message)
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dismiss();
				}
			})
			.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dismiss();
				}
			})
			.setTitle(R.string.send_google_no_account_title)
			.create();
	}
}