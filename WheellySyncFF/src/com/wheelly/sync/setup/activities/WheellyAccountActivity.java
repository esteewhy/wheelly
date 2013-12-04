/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync.setup.activities;

import java.util.Locale;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.InvalidSyncKeyException;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;
import org.mozilla.gecko.sync.setup.activities.ActivityUtils;
import org.mozilla.gecko.sync.setup.activities.SetupFailureActivity;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;

import com.wheelly.sync.WheellyAccountAuthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class WheellyAccountActivity extends AccountActivity {
  private final static String LOG_TAG = "AccountActivity";

  private AccountManager      mAccountManager;
  private Context             mContext;
  private String              username;
  private String              password;
  private String              key;
  private String              server = "http://sync.wheelly.com";

  // UI elements.
  private EditText            serverInput;
  private EditText            usernameInput;
  private EditText            passwordInput;
  private EditText            synckeyInput;
  private CheckBox            serverCheckbox;
  private Button              connectButton;
  private Button              cancelButton;
  private ProgressDialog      progressDialog;

  private AccountAuthenticator accountAuthenticator;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mContext = getApplicationContext();
    mAccountManager = AccountManager.get(mContext);

    // Find UI elements.
    usernameInput = (EditText) findViewById(R.id.usernameInput);
    passwordInput = (EditText) findViewById(R.id.passwordInput);
    synckeyInput = (EditText) findViewById(R.id.keyInput);
    serverInput = (EditText) findViewById(R.id.serverInput);
    connectButton = (Button) findViewById(R.id.accountConnectButton);
    cancelButton = (Button) findViewById(R.id.accountCancelButton);
    serverCheckbox = (CheckBox) findViewById(R.id.checkbox_server);
  }
  
  @Override
  public void cancelConnectHandler(View target) {
    if (accountAuthenticator != null) {
      accountAuthenticator.isCanceled = true;
      accountAuthenticator = null;
    }
    displayVerifying(false);
    activateView(connectButton, true);
    clearCredentials();
    usernameInput.requestFocus();
  }

  private void clearCredentials() {
    // Only clear password. Re-typing the sync key or email is annoying.
    passwordInput.setText("");
  }
  /*
   * Get credentials on "Connect" and write to AccountManager, where it can be
   * accessed by Fennec and Sync Service.
   */
  @Override
  public void connectClickHandler(View target) {
    Logger.debug(LOG_TAG, "connectClickHandler for view " + target);
    // Validate sync key format.
    try {
      key = ActivityUtils.validateSyncKey(synckeyInput.getText().toString());
    } catch (InvalidSyncKeyException e) {
      // Toast: invalid sync key format.
      Toast toast = Toast.makeText(mContext, R.string.sync_new_recoverykey_status_incorrect, Toast.LENGTH_LONG);
      toast.show();
      return;
    }
    username = usernameInput.getText().toString().toLowerCase(Locale.US);
    password = passwordInput.getText().toString();
    key      = synckeyInput.getText().toString();
    server   = "http://sync.wheelly.com";

    if (serverCheckbox.isChecked()) {
      String userServer = serverInput.getText().toString();
      if (userServer != null) {
        userServer = userServer.trim();
        if (userServer.length() != 0) {
          if (!userServer.startsWith("https://") &&
              !userServer.startsWith("http://")) {
            // Assume HTTPS if not specified.
            userServer = "https://" + userServer;
            serverInput.setText(userServer);
          }
          server = userServer;
        }
      }
    }

    clearErrors();
    displayVerifying(true);
    cancelButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        cancelConnectHandler(v);
        // Set cancel click handler to leave account setup.
        cancelButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            cancelClickHandler(v);
          }
        });
      }
    });

    accountAuthenticator = new WheellyAccountAuthenticator(this);
    accountAuthenticator.authenticate(server, username, password);
  }

  /*
   * Callback that handles auth based on success/failure
   */
  @Override
  public void authCallback(final AuthenticationResult result) {
    displayVerifying(false);
    if (result != AuthenticationResult.SUCCESS) {
      Logger.debug(LOG_TAG, "displayFailure()");
      displayFailure(result);
      return;
    }
    // Successful authentication. Create and add account to AccountManager.
    SyncAccountParameters syncAccount = new SyncAccountParameters(
        mContext, mAccountManager, username, key, password, server);
    createAccountOnThread(syncAccount);
  }

  private void createAccountOnThread(final SyncAccountParameters syncAccount) {
    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        Account account = SyncAccounts.createSyncAccount(syncAccount);
        boolean isSuccess = (account != null);
        if (!isSuccess) {
          setResult(RESULT_CANCELED);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              displayFailure(AuthenticationResult.FAILURE_ACCOUNT);
            }
          });
          return;
        }

        // Account created successfully.
        clearErrors();

        Bundle resultBundle = new Bundle();
        resultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, syncAccount.username);
        resultBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, SyncConstants.ACCOUNTTYPE_SYNC);
        resultBundle.putString(AccountManager.KEY_AUTHTOKEN, SyncConstants.ACCOUNTTYPE_SYNC);
        setAccountAuthenticatorResult(resultBundle);

        setResult(RESULT_OK);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            authSuccess();
          }
        });
      }
    });
  }

  private void displayVerifying(final boolean isVerifying) {
    if (isVerifying) {
      progressDialog = ProgressDialog.show(WheellyAccountActivity.this, "", getString(R.string.sync_verifying_label), true);
    } else {
      progressDialog.dismiss();
    }
  }

  private void displayFailure(final AuthenticationResult result) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Intent intent;
        switch (result) {
        case FAILURE_USERNAME:
          // No such username. Don't leak whether the username exists.
        case FAILURE_PASSWORD:
          findViewById(R.id.cred_error).setVisibility(View.VISIBLE);
          usernameInput.requestFocus();
          break;
        case FAILURE_SERVER:
          findViewById(R.id.server_error).setVisibility(View.VISIBLE);
          serverInput.requestFocus();
          break;
        case FAILURE_ACCOUNT:
          intent = new Intent(mContext, SetupFailureActivity.class);
          intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
          intent.putExtra(Constants.INTENT_EXTRA_IS_ACCOUNTERROR, true);
          startActivity(intent);
          break;
        case FAILURE_OTHER:
        default:
          // Display default error screen.
          Logger.debug(LOG_TAG, "displaying default failure.");
          intent = new Intent(mContext, SetupFailureActivity.class);
          intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
          startActivity(intent);
        }
      }
    });
  }

  private void activateView(View view, boolean toActivate) {
    view.setEnabled(toActivate);
    view.setClickable(toActivate);
  }

  private void clearErrors() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        findViewById(R.id.cred_error).setVisibility(View.GONE);
        findViewById(R.id.server_error).setVisibility(View.GONE);
      }
    });
  }
}
