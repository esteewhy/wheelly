/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;
import org.mozilla.gecko.sync.setup.auth.AuthenticatorStage;
import org.mozilla.gecko.sync.setup.auth.EnsureUserExistenceStage;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class CreateUserStage implements AuthenticatorStage {
  private final String LOG_TAG = "CreateUser";
  
  @Override
  public void execute(final AccountAuthenticator aa) throws URISyntaxException,
      UnsupportedEncodingException {
    final EnsureUserExistenceStage.EnsureUserExistenceStageDelegate callbackDelegate = new EnsureUserExistenceStage.EnsureUserExistenceStageDelegate() {

      @Override
      public void handleSuccess() {
        // User exists; now determine auth node.
        Logger.debug(LOG_TAG, "handleSuccess()");
        aa.runNextStage();
      }

      @Override
      public void handleFailure(AuthenticationResult result) {
        aa.abort(result, new Exception("Failure in CreateUser"));
      }

      @Override
      public void handleError(Exception e) {
        Logger.info(LOG_TAG, "Error creating user.");
        aa.abort(AuthenticationResult.FAILURE_SERVER, e);
      }

    };

    String userRequestUrl = aa.nodeServer + "user/1.0" + aa.username;
    final BaseResource httpResource = new BaseResource(userRequestUrl);
    httpResource.delegate = new BaseResourceDelegate(httpResource) {
      @Override
      public String getUserAgent() {
        return SyncConstants.USER_AGENT;
      }

      @Override
      public void handleHttpResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        switch(statusCode) {
        case 200:
          try {
				InputStream content = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
				String username = reader.readLine();
				BaseResource.consumeReader(reader);
				reader.close();
				callbackDelegate.handleSuccess();
          } catch (Exception e) {
            Logger.error(LOG_TAG, "Failure in content parsing.", e);
            callbackDelegate.handleFailure(AuthenticationResult.FAILURE_OTHER);
          }
          break;
        default: // No other response is acceptable.
          callbackDelegate.handleFailure(AuthenticationResult.FAILURE_OTHER);
        }
        Logger.debug(LOG_TAG, "Consuming entity.");
        BaseResource.consumeEntity(response.getEntity());
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        callbackDelegate.handleError(e);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        callbackDelegate.handleError(e);
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        callbackDelegate.handleError(e);
      }

    };
    // Make request.
    AccountAuthenticator.runOnThread(new Runnable() {

      @Override
      public void run() {
    	final JSONObject jAccount = new JSONObject();
  		jAccount.put("email", "esteewhy@hotmail.com");
  		jAccount.put(Constants.JSON_KEY_PASSWORD, aa.password);
  		jAccount.put("captcha-challenge", "");
  		jAccount.put("captcha-response", "");
        try {
			httpResource.put(jAccount);
		} catch (UnsupportedEncodingException e) {
			callbackDelegate.handleFailure(AuthenticationResult.FAILURE_OTHER);
		}
      }
    });
  }
}