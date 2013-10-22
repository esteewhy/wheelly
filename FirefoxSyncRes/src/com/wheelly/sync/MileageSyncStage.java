/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.repositories.ConstrainedServer11Repository;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.domain.VersionConstants;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

public class MileageSyncStage extends ServerSyncStage {
  protected static final String LOG_TAG = "HeartbeatsStage";

  // Eventually this kind of sync stage will be data-driven,
  // and all this hard-coding can go away.
  private static final String MILEAGE_SORT          = "index";
  private static final long   MILEAGE_REQUEST_LIMIT = 250;

  @Override
  protected String getCollection() {
    return "mileages";
  }

  @Override
  protected String getEngineName() {
    return "mileages";
  }

  @Override
  public Integer getStorageVersion() {
    return VersionConstants.HISTORY_ENGINE_VERSION;
  }

  @Override
  protected Repository getLocalRepository() {
    return new MileageRepository();
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new ConstrainedServer11Repository(session.config.getClusterURLString(),
                                             session.config.username,
                                             getCollection(),
                                             session,
                                             MILEAGE_REQUEST_LIMIT,
                                             MILEAGE_SORT);
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new RecordFactory() {
		@Override
		public Record createRecord(Record record) {
		    MileageRecord r = new MileageRecord();
		    r.initFromEnvelope((CryptoRecord) record);
		    return r;
		}
    };
  }

  @Override
  protected boolean isEnabled() throws MetaGlobalException {
    if (session == null || session.getContext() == null) {
      return false;
    }
    return super.isEnabled();
  }
}