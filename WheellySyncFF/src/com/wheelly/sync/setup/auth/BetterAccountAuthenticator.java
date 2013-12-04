/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync.setup.auth;

import java.util.LinkedList;
import java.util.Queue;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.setup.auth.AuthenticateAccountStage;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;
import org.mozilla.gecko.sync.setup.auth.AuthenticatorStage;
import org.mozilla.gecko.sync.setup.auth.EnsureUserExistenceStage;
import org.mozilla.gecko.sync.setup.auth.FetchUserNodeStage;

public class BetterAccountAuthenticator extends AccountAuthenticator {
  private final String LOG_TAG = "BetterAccountAuthenticator";

  protected AccountActivity activityCallback;
  protected Queue<AuthenticatorStage> stages;

  public BetterAccountAuthenticator(AccountActivity activity) {
    super(null);
    activityCallback = activity;
    prepareStages();
  }

  protected void prepareStages() {
    stages = new LinkedList<AuthenticatorStage>();
    stages.add(new EnsureUserExistenceStage());
    stages.add(new FetchUserNodeStage());
    stages.add(new AuthenticateAccountStage());
  }
  
  @Override
  public void authenticate(String server, String account, String password) {
    // Set authentication values.
    if (!server.endsWith("/")) {
      server += "/";
    }
    nodeServer = server;
    this.password = password;

    // Calculate and save username hash.
    try {
      username = Utils.usernameFromAccount(account);
    } catch (Exception e) {
      abort(AuthenticationResult.FAILURE_OTHER, e);
      return;
    }
    Logger.pii(LOG_TAG, "Username:" + username);
    Logger.debug(LOG_TAG, "Running first stage.");
    // Start first stage of authentication.
    runNextStage();
  }

  /**
   * Run next stage of authentication.
   */
  @Override
  public void runNextStage() {
    if (isCanceled) {
      return;
    }
    if (stages.size() == 0) {
      Logger.debug(LOG_TAG, "Authentication completed.");
      activityCallback.authCallback(isSuccess ? AuthenticationResult.SUCCESS : AuthenticationResult.FAILURE_PASSWORD);
      return;
    }
    AuthenticatorStage nextStage = stages.remove();
    try {
      nextStage.execute(this);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Unhandled exception in stage " + nextStage);
      abort(AuthenticationResult.FAILURE_OTHER, e);
    }
  }

  /**
   * Abort authentication.
   *
   * @param result
   *    returned to callback.
   * @param e
   *    Exception causing abort.
   */
  @Override
  public void abort(AuthenticationResult result, Exception e) {
    if (isCanceled) {
      return;
    }
    Logger.warn(LOG_TAG, "Authentication failed.", e);
    activityCallback.authCallback(result);
  }
}
