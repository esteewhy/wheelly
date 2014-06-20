package com.wheelly.sync.syncadapter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.Sync11Configuration;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.config.AccountPickler;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import com.wheelly.sync.WheellyGlobalSession;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

public class WheellySyncAdapter extends SyncAdapter {
  private static final String  LOG_TAG = "SyncAdapter";
  private final Context        mContext;

  public WheellySyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContext = context;
  }

  /**
   * Now that we have a sync key and password, go ahead and do the work.
   * @throws NoSuchAlgorithmException
   * @throws IllegalArgumentException
   * @throws SyncConfigurationException
   * @throws AlreadySyncingException
   * @throws NonObjectJSONException
   * @throws ParseException
   * @throws IOException
   * @throws CryptoException
   */
  protected void performSync(final Account account,
                             final Bundle extras,
                             final String authority,
                             final ContentProviderClient provider,
                             final SyncResult syncResult,
                             final String username,
                             final String password,
                             final String prefsPath,
                             final String serverURL,
                             final String syncKey)
                                 throws NoSuchAlgorithmException,
                                        SyncConfigurationException,
                                        IllegalArgumentException,
                                        AlreadySyncingException,
                                        IOException, ParseException,
                                        NonObjectJSONException, CryptoException {
    Logger.trace(LOG_TAG, "Performing sync.");

    /**
     * Bug 769745: pickle Sync account parameters to JSON file. Un-pickle in
     * <code>SyncAccounts.syncAccountsExist</code>.
     */
    try {
      // Constructor can throw on nulls, which should not happen -- but let's be safe.
      final SyncAccountParameters params = new SyncAccountParameters(mContext, null,
        account.name, // Un-encoded, like "test@mozilla.com".
        syncKey,
        password,
        serverURL,
        null, // We'll re-fetch cluster URL; not great, but not harmful.
        clientsDataDelegate.getClientName(),
        clientsDataDelegate.getAccountGUID());

      // Bug 772971: pickle Sync account parameters on background thread to
      // avoid strict mode warnings.
      ThreadPool.run(new Runnable() {
        @Override
        public void run() {
          final boolean syncAutomatically = ContentResolver.getSyncAutomatically(account, authority);
          try {
            AccountPickler.pickle(mContext, Constants.ACCOUNT_PICKLE_FILENAME, params, syncAutomatically);
          } catch (Exception e) {
            // Should never happen, but we really don't want to die in a background thread.
            Logger.warn(LOG_TAG, "Got exception pickling current account details; ignoring.", e);
          }
        }
      });
    } catch (IllegalArgumentException e) {
      // Do nothing.
    }
    
    if (username == null) {
      throw new IllegalArgumentException("username must not be null.");
    }

    if (syncKey == null) {
      throw new SyncConfigurationException();
    }
    
    final KeyBundle keyBundle = new KeyBundle(username, syncKey);
    
    if (keyBundle == null ||
        keyBundle.getEncryptionKey() == null ||
        keyBundle.getHMACKey() == null) {
      throw new SyncConfigurationException();
    }
    
    final AuthHeaderProvider authHeaderProvider = new BasicAuthHeaderProvider(username, password);
    final SharedPreferences prefs = getContext().getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
    final SyncConfiguration config = new Sync11Configuration(username, authHeaderProvider, prefs, keyBundle);

    Collection<String> knownStageNames = SyncConfiguration.validEngineNames();
    config.stagesToSync = Utils.getStagesToSyncFromBundle(knownStageNames, extras);
    
    extras.remove(Constants.EXTRAS_KEY_STAGES_TO_SYNC);
    extras.putString(Constants.EXTRAS_KEY_STAGES_TO_SYNC, "{ \"history\" : true }");
    
    GlobalSession globalSession = new WheellyGlobalSession(config, this, this.mContext, clientsDataDelegate, nodeAssignmentDelegate);
    globalSession.start();
  }
}