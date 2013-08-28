/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wheelly.activity;

import com.google.android.maps.mytracks.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A chooser to select an account.
 *
 * @author Jimmy Shih
 */
public class SelectDialog<T> extends DialogFragment {
	private final OnSelectItemListener<T> listener;
	private int selectedAccountIndex;
	private final T[] accounts;
	private final ItemDescriptor<T> descriptor;
	
	public SelectDialog(T[] accounts, int selected, ItemDescriptor<T> descriptor, OnSelectItemListener<T> listener) {
		this.listener = listener;
		this.accounts = accounts;
		this.selectedAccountIndex = selected;
		this.descriptor = descriptor;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Context ctx = getActivity();
		
		String[] choices = new String[accounts.length];
		
		for (int i = 0; i < accounts.length; i++) {
			choices[i] = descriptor.toString(accounts[i]);
		}
		
		return new AlertDialog.Builder(ctx)
			.setCancelable(true)
			.setNegativeButton(R.string.generic_cancel, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dismiss();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dismiss();
				}
			})
			.setPositiveButton(R.string.generic_ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					T account = accounts[selectedAccountIndex];
					if(null != listener) {
						listener.onSelect(getDialog(), selectedAccountIndex, account);
					}
				}
			})
			.setSingleChoiceItems(
				choices, selectedAccountIndex, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectedAccountIndex = which;
					}
				})
			.setTitle(R.string.send_google_choose_account_title)
			.create();
	}
	
	public static interface OnSelectItemListener<T> {
		public void onSelect(DialogInterface dialog, int which, T account);
	}
	
	public static interface ItemDescriptor<T> {
		public String toString(T item);
	}
}