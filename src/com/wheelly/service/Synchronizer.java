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
package com.wheelly.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.sendtogoogle.PermissionCallback;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.wheelly.activity.NoAccountsDialog;
import com.wheelly.activity.SelectDialog;
import com.wheelly.io.docs.ReportingSendAsyncTask;
import com.wheelly.io.docs.SyncDocsAsyncTask;

public class Synchronizer {
	private final FragmentActivity activity;
	
	public Synchronizer(FragmentActivity activity) {
		this.activity = activity; 
	}
	
	private void checkPermission(final Account account, final long id)
	{
		final PermissionCallback spreadsheetsCallback = new PermissionCallback() {
			@Override
			public void onSuccess() {
				ReportingSendAsyncTask asyncTask = new SyncDocsAsyncTask(activity, id, account);
				asyncTask.execute();
			}
			
			@Override
			public void onFailure() {
				handleNoAccountPermission();
			}
		};
		
		SendToGoogleUtils.checkPermissionByActivity(activity, account.name,
		          SendToGoogleUtils.SPREADSHEET_SCOPE,
		          SendToGoogleUtils.SPREADSHEET_PERMISSION_REQUEST_CODE, spreadsheetsCallback);
	}
	
	public void execute(final long id) {
		final Account[] accounts = AccountManager.get(activity).getAccountsByType(Constants.ACCOUNT_TYPE);
		
		if(0 == accounts.length) {
			new NoAccountsDialog().show(activity.getSupportFragmentManager(), "no_accounts");
			return;
		}
		
		if(1 == accounts.length) {
			setPreferredAccountName(accounts[0].name);
			checkPermission(accounts[0], id);
		} else {
			final int selectedAccountIndex = getPreferredAccountIndex(accounts);
			
			if(selectedAccountIndex >= 0) {
				checkPermission(accounts[selectedAccountIndex], id);
			} else {
				new SelectDialog<Account>(accounts,
						selectedAccountIndex,
						new SelectDialog.ItemDescriptor<Account>() {
							@Override
							public String toString(Account item) {
								return item.name;
							}
						},
						new SelectDialog.OnSelectItemListener<Account>() {
						@Override
						public void onSelect(DialogInterface dialog, int which, Account account) {
							setPreferredAccountName(account.name);
							checkPermission(account, id);
						}
					}).show(activity.getSupportFragmentManager(), "select");
			}
		}
	}
	
	private int getPreferredAccountIndex(Account[] accounts) {
		String preferredAccount = PreferencesUtils.getString(activity,
			R.string.preferred_account_key,
			PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);

		for (int i = 0; i < accounts.length; i++) {
			if (accounts[i].name.equals(preferredAccount)) {
				return i;
			}
		}
		
		return -1;
	}
	
	private void setPreferredAccountName(String name) {
		PreferencesUtils.setString(activity,
			R.string.preferred_account_key,
			name);
	}
	
  private void handleNoAccountPermission() {
    Toast.makeText(activity, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
  }
}