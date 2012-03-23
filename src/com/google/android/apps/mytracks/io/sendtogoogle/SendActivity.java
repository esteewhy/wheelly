/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.io.sendtogoogle;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.AccountChooser;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManager.AuthCallback;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.util.UriUtils;
import com.wheelly.R;
import com.wheelly.db.DatabaseSchema.Timeline;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

/**
 * Helper activity for managing the sending of tracks to Google services.
 *
 * @author Rodrigo Damazio
 */
public class SendActivity extends Activity implements ProgressIndicator {
  // Keys for saved state variables.
  private static final String STATE_DOCS_SUCCESS = "docsSuccess";
  private static final String STATE_STATE = "state";
  private static final String STATE_ACCOUNT_TYPE = "accountType";
  private static final String STATE_ACCOUNT_NAME = "accountName";

  /** States for the state machine that defines the upload process. */
  private enum SendState {
    START,
    AUTHENTICATE_MAPS,
    PICK_MAP,
    AUTHENTICATE_DOCS,
    AUTHENTICATE_TRIX,
    SEND_TO_DOCS,
    SEND_TO_DOCS_DONE,
    SHOW_RESULTS,
    FINISH,
    DONE,
    NOT_READY
  }

  private static final int PROGRESS_DIALOG = 2;
  /* @VisibleForTesting */
  static final int DONE_DIALOG = 3;

  // UI
  private ProgressDialog progressDialog;

  // Authentication
  private AuthManager lastAuth;
  private final HashMap<String, AuthManager> authMap = new HashMap<String, AuthManager>();
  private AccountChooser accountChooser;
  private String lastAccountName;
  private String lastAccountType;

  // Send request information.
  private long sendTrackId;

  // Send result information, used by results dialog.
  private boolean sendToDocsSuccess = false;

  // Current sending state.
  private SendState currentState;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "SendActivity.onCreate");
    super.onCreate(savedInstanceState);

    resetState();

    if (savedInstanceState != null) {
      restoreInstanceState(savedInstanceState);
    }

    // If we had the instance restored after it was done, reset it.
    if (currentState == SendState.DONE) {
      resetState();
    }

    // Only consider the intent if we're not restoring from a previous state.
    if (currentState == SendState.START) {
      if (!handleIntent()) {
        finish();
        return;
      }
    }

    // Execute the state machine, at the start or restored state.
    Log.w(TAG, "Starting at state " + currentState);
    executeStateMachine(currentState);
  }

  private boolean handleIntent() {
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    Uri data = intent.getData();
    if (!Intent.ACTION_SEND.equals(action) ||
        !Timeline.CONTENT_ITEM_TYPE.equals(type) ||
        !UriUtils.matchesContentUri(data, Timeline.CONTENT_URI)) {
      Log.e(TAG, "Got bad send intent: " + intent);
      return false;
    }

    sendTrackId = ContentUris.parseId(data);
    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case PROGRESS_DIALOG:
        return createProgressDialog();
    }

    return null;
  }

  private void restoreInstanceState(Bundle savedInstanceState) {
    currentState = SendState.values()[savedInstanceState.getInt(STATE_STATE)];

    sendToDocsSuccess = savedInstanceState.getBoolean(STATE_DOCS_SUCCESS);

    lastAccountName = savedInstanceState.getString(STATE_ACCOUNT_NAME);
    lastAccountType = savedInstanceState.getString(STATE_ACCOUNT_TYPE);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(STATE_STATE, currentState.ordinal());

    outState.putBoolean(STATE_DOCS_SUCCESS, sendToDocsSuccess);

    outState.putString(STATE_ACCOUNT_NAME, lastAccountName);
    outState.putString(STATE_ACCOUNT_TYPE, lastAccountType);

    // TODO: Ideally we should serialize/restore the authenticator map and lastAuth somehow,
    //       but it's highly unlikely we'll get killed while an auth dialog is displayed.
  }

  private void executeStateMachine(SendState startState) {
    currentState = startState;

    // If a state handler returns NOT_READY, it means it's waiting for some
    // event, and will call this method again when it happens.
    while (currentState != SendState.DONE &&
           currentState != SendState.NOT_READY) {
      Log.d(TAG, "Executing state " + currentState);
      currentState = executeState(currentState);
      Log.d(TAG, "New state is " + currentState);
    }
  }

  private SendState executeState(SendState state) {
    switch (state) {
      case START:
        return startSend();
      case AUTHENTICATE_DOCS:
        return authenticateToGoogleDocs();
      case AUTHENTICATE_TRIX:
        return authenticateToGoogleTrix();
      case SEND_TO_DOCS:
        return sendToGoogleDocs();
      case SEND_TO_DOCS_DONE:
        return onSendToGoogleDocsDone();
      case SHOW_RESULTS:
        return onSendToGoogleDone();
      case FINISH:
        return onAllDone();
      default:
        Log.e(TAG, "Reached a non-executable state");
        return null;
    }
  }

  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   */
  private SendState startSend() {
    showDialog(PROGRESS_DIALOG);
    return SendState.AUTHENTICATE_DOCS;
  }

  private Dialog createProgressDialog() {
    progressDialog = new ProgressDialog(this);
    progressDialog.setCancelable(false);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setTitle(R.string.generic_progress_title);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setMax(100);
    progressDialog.setProgress(0);

    return progressDialog;
  }

  private SendState authenticateToGoogleDocs() {
    setProgressValue(0);
    setProgressMessage(getAuthenticatingProgressMessage("Google Docs"));
    authenticate(Constants.AUTHENTICATE_TO_DOCLIST, SendToDocs.GDATA_SERVICE_NAME_DOCLIST);
    // AUTHENTICATE_TO_DOCLIST callback calls authenticateToGoogleTrix
    return SendState.NOT_READY;
  }

  private SendState authenticateToGoogleTrix() {
    setProgressValue(30);
    setProgressMessage(getAuthenticatingProgressMessage("Google Trix"));
    authenticate(Constants.AUTHENTICATE_TO_TRIX, SendToDocs.GDATA_SERVICE_NAME_TRIX);
    // AUTHENTICATE_TO_TRIX callback calls sendToGoogleDocs
    return SendState.NOT_READY;
  }

  private SendState sendToGoogleDocs() {
    setProgressValue(50);
    String format = getString(R.string.send_google_progress_sending);
    String serviceName = "Google Docs";
    setProgressMessage(String.format(format, serviceName));
    final SendToDocs sender = new SendToDocs(this,
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_TRIX),
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_DOCLIST),
        this);
    Runnable onCompletion = new Runnable() {
      public void run() {
        setProgressValue(100);
        sendToDocsSuccess = sender.wasSuccess();
        executeStateMachine(SendState.SEND_TO_DOCS_DONE);
      }
    };
    sender.setOnCompletion(onCompletion);
    sender.sendToDocs(sendTrackId);

    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleDocsDone() {
    return SendState.SHOW_RESULTS;
  }

  private SendState onSendToGoogleDone() {

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Sending to Google done.");
        dismissDialog(PROGRESS_DIALOG);
        Toast.makeText(SendActivity.this, "Sent to Google Docs", 5);
        executeStateMachine(SendState.FINISH);
      }
    });

    return SendState.NOT_READY;
  }

  private SendState onAllDone() {
    Log.d(TAG, "All sending done.");
    removeDialog(PROGRESS_DIALOG);
    progressDialog = null;
    finish();
    return SendState.DONE;
  }

  /**
   * Initializes the authentication manager which obtains an authentication
   * token, prompting the user for a login and password if needed.
   */
  private void authenticate(final int requestCode, final String service) {
    lastAuth = authMap.get(service);
    if (lastAuth == null) {
      Log.i(TAG, "Creating a new authentication for service: " + service);
      lastAuth = AuthManagerFactory.getAuthManager(this,
          Constants.GET_LOGIN,
          null,
          true,
          service);
      authMap.put(service, lastAuth);
    }

    Log.d(TAG, "Logging in to " + service + "...");
    if (AuthManagerFactory.useModernAuthManager()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          chooseAccount(requestCode, service);
        }
      });
    } else {
      doLogin(requestCode, service, null);
    }
  }

  private void chooseAccount(final int requestCode, final String service) {
    if (accountChooser == null) {
      accountChooser = new AccountChooser();

      // Restore state if necessary.
      if (lastAccountName != null && lastAccountType != null) {
        accountChooser.setChosenAccount(lastAccountName, lastAccountType);
      }
    }

    accountChooser.chooseAccount(SendActivity.this,
        new AccountChooser.AccountHandler() {
          @Override
          public void onAccountSelected(Account account) {
            if (account == null) {
              dismissDialog(PROGRESS_DIALOG);
              finish();
              return;
            }

            lastAccountName = account.name;
            lastAccountType = account.type;
            doLogin(requestCode, service, account);
          }
        });
  }

  private void doLogin(final int requestCode, final String service, final Object account) {
    lastAuth.doLogin(new AuthCallback() {
      @Override
      public void onAuthResult(boolean success) {
        Log.i(TAG, "Login success for " + service + ": " + success);
        if (!success) {
          executeStateMachine(SendState.SHOW_RESULTS);
          return;
        }

        onLoginSuccess(requestCode);
      }
    }, account);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    SendState nextState = null;
    switch (requestCode) {
      case Constants.GET_LOGIN: {
        if (resultCode == RESULT_CANCELED || lastAuth == null) {
          nextState = SendState.FINISH;
          break;
        }

        // This will invoke onAuthResult appropriately.
        lastAuth.authResult(resultCode, results);
        break;
      }
      default: {
        Log.e(TAG, "Unrequested result: " + requestCode);
        return;
      }
    }

    if (nextState != null) {
      executeStateMachine(nextState);
    }
  }

  private void onLoginSuccess(int requestCode) {
    SendState nextState;
    switch (requestCode) {
      case Constants.AUTHENTICATE_TO_DOCLIST:
        // Authenticated with Google Docs
        nextState = SendState.AUTHENTICATE_TRIX;
        break;
      case Constants.AUTHENTICATE_TO_TRIX:
        // Authenticated with Trix
        nextState = SendState.SEND_TO_DOCS;
        break;

      default: {
        Log.e(TAG, "Unrequested login code: " + requestCode);
        return;
      }
    }

    executeStateMachine(nextState);
  }

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  private void resetState() {
    currentState = SendState.START;
    sendToDocsSuccess = true;
  }

  /**
   * Gets a progress message indicating My Tracks is authenticating to a
   * service.
   *
   * @param type the type of service
   */
  private String getAuthenticatingProgressMessage(String serviceName) {
    String format = getString(R.string.send_google_progress_authenticating);
    return String.format(format, serviceName);
  }

  @Override
  public void setProgressMessage(final String message) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (progressDialog != null) {
          progressDialog.setMessage(message);
        }
      }
    });
  }

  @Override
  public void setProgressValue(final int percent) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (progressDialog != null) {
          progressDialog.setProgress(percent);
        }
      }
    });
  }

  @Override
  public void clearProgressMessage() {
    progressDialog.setMessage("");
  }

  public static void sendToGoogle(Context ctx, long trackId) {
    Uri uri = ContentUris.withAppendedId(Timeline.CONTENT_URI, trackId);

    Intent intent = new Intent(ctx, SendActivity.class);
    intent.setAction(Intent.ACTION_SEND);
    intent.setDataAndType(uri, Timeline.CONTENT_ITEM_TYPE);
    ctx.startActivity(intent);
  }
}