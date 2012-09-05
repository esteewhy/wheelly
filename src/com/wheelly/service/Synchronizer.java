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

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.wheelly.activity.NoAccountsDialog;
import com.wheelly.activity.SelectDialog;
import com.wheelly.io.docs.ReportingSendAsyncTask;
import com.wheelly.io.docs.SyncDocsAsyncTask;

/**
 * A chooser to select an account.
 *
 * @author Jimmy Shih
 */
public class Synchronizer {
	private static final String TAG = Synchronizer.class.getSimpleName();
	private Account[] accounts;
	private final FragmentActivity activity;
	
	public Synchronizer(FragmentActivity activity) {
		this.activity = activity; 
	}
	
	public void execute(final long id) {
		accounts = AccountManager.get(activity).getAccountsByType(Constants.ACCOUNT_TYPE);
		
		if(0 == accounts.length) {
			new NoAccountsDialog().show(activity.getSupportFragmentManager(), "no_accounts");
			return;
		}
		
		final ConditionalCallback<Account> syncTask = new ConditionalCallback<Account>() {
			@Override
			public void onSuccess(Account item) {
				ReportingSendAsyncTask asyncTask = new SyncDocsAsyncTask(activity, id, item);
				asyncTask.execute();

			}
		};
		
		if(1 == accounts.length) {
			getPermission(SpreadsheetsClient.SERVICE, true, accounts[0], syncTask);
		} else {
			final int selectedAccountIndex = getPreferredAccountIndex();
			
			if(selectedAccountIndex >= 0) {
				getPermission(SpreadsheetsClient.SERVICE, true, accounts[selectedAccountIndex], syncTask);
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
							getPermission(SpreadsheetsClient.SERVICE, true, account, syncTask);
						}
					}).show(activity.getSupportFragmentManager(), "select");
			}
		}
	}
	
	private int getPreferredAccountIndex() {
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
	
  /**
   * Gets the user permission to access a service.
   * 
   * @param authTokenType the auth token type of the service
   * @param needPermission true if need the permission
   * @param callback callback after getting the permission
   */
  private void getPermission(
      String authTokenType, boolean needPermission, final Account account, final ConditionalCallback<Account> onSuccess) {
    if (needPermission) {
      AccountManager.get(activity).getAuthToken(
    		  account,
    		  authTokenType,
    		  null,
    		  activity,
          new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
              try {
                if (future.getResult().getString(AccountManager.KEY_AUTHTOKEN) != null) {
                	onSuccess.onSuccess(account);
                } else {
                  Log.d(TAG, "auth token is null");

                }
              } catch (OperationCanceledException e) {
                Log.d(TAG, "Unable to get auth token", e);
              } catch (AuthenticatorException e) {
                Log.d(TAG, "Unable to get auth token", e);
              } catch (IOException e) {
                Log.d(TAG, "Unable to get auth token", e);
              }
            }
          }, null);
    } else {
      onSuccess.onSuccess(account);
    }
  }
  
	private static interface ConditionalCallback<T> {
		public void onSuccess(T item);
	}
}