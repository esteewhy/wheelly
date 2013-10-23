/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

public class EventSyncStage extends ServerSyncStage {
  protected static final String LOG_TAG = "EventSyncStage";

  @Override
  protected String getCollection() {
    return "events";
  }

  @Override
  protected String getEngineName() {
    return "events";
  }

  @Override
  public Integer getStorageVersion() {
    return 1;
  }

  @Override
  protected Repository getLocalRepository() {
    return new MileageRepository();
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new RecordFactory() {
		@Override
		public Record createRecord(Record record) {
		    EventRecord r = new EventRecord();
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