/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import org.mozilla.gecko.sync.repositories.HistoryRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public class MileageRepository extends AndroidBrowserRepository implements HistoryRepository {

  @Override
  protected void sessionCreator(RepositorySessionCreationDelegate delegate, Context context) {
    delegate.onSessionCreated(new AndroidBrowserHistoryRepositorySession(MileageRepository.this, context));
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor(Context context) {
    return new MileageDataAccessor(context);
  }
}