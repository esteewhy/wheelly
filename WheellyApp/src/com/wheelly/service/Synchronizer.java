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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.wheelly.activity.NoAccountsDialog;
import com.wheelly.activity.SelectDialog;

public class Synchronizer {
	private final Context context;
	private final FragmentManager fm;
	
	public Synchronizer(Context context, FragmentManager fm) {
		this.context = context;
		this.fm = fm;
	}
	
	private void execute(final long id, Account account) {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName("com.wheelly.sync.drive", "com.wheelly.sync.drive.SyncService"));
		intent.putExtra("id", id);
		intent.putExtra("accountName", account.name);
		context.startService(intent);
		//new SendSpreadsheetsAsyncTask(context, id, account.name).execute();
	}
	
	public void execute(final long id) {
		final Account[] accounts = AccountManager.get(context).getAccountsByType(Constants.ACCOUNT_TYPE);
		
		if(0 == accounts.length) {
			new NoAccountsDialog().show(fm, "no_accounts");
			return;
		}
		
		if(1 == accounts.length) {
			setPreferredAccountName(accounts[0].name);
			execute(id, accounts[0]);
		} else {
			final int selectedAccountIndex = getPreferredAccountIndex(accounts);
			
			if(selectedAccountIndex >= 0) {
				execute(id, accounts[selectedAccountIndex]);
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
							execute(id, account);
						}
					}).show(fm, "select");
			}
		}
	}
	
	private int getPreferredAccountIndex(Account[] accounts) {
		String preferredAccount = PreferencesUtils.getString(context,
			R.string.google_account_key,
			PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);

		for (int i = 0; i < accounts.length; i++) {
			if (accounts[i].name.equals(preferredAccount)) {
				return i;
			}
		}
		
		return -1;
	}
	
	private void setPreferredAccountName(String name) {
		PreferencesUtils.setString(context, R.string.google_account_key, name);
	}
}