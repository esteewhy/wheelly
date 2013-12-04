package com.wheelly.sync.stage;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

import android.content.Context;

import com.wheelly.sync.repositories.EventRepositorySession;
import com.wheelly.sync.repositories.domain.EventRecord;

public class EventSyncStage extends ServerSyncStage {
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
    return new Repository() {
      @Override
      public void createSession(RepositorySessionCreationDelegate delegate, Context context) {
        try {
          delegate.onSessionCreated(new EventRepositorySession(this, context));
        } catch(Exception ex) {
          delegate.onSessionCreateFailed(ex);
        }
      }
    };
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new RecordFactory() {
        @Override
        public Record createRecord(Record record) {
            Record r = new EventRecord();
            r.initFromEnvelope((CryptoRecord) record);
            return r;
        }
    };
  }

  @Override
  protected boolean isEnabled() throws MetaGlobalException {
    return session != null && session.getContext() != null;// && super.isEnabled();
  }
}