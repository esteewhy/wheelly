/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import java.util.ArrayList;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoGuidForIdException;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.ParentNotFoundException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class EventRepositorySession extends AndroidBrowserRepositorySession {
  public static final String LOG_TAG = "EventRepoSess";

  public static final String KEY_DATE = "date";
  public static final String KEY_TYPE = "type";

  /**
   * The number of records to queue for insertion before writing to databases.
   */
  public static int INSERT_RECORD_THRESHOLD = 50;

  public EventRepositorySession(Repository repository, Context context) {
    super(repository);
    dbHelper = new MileageDataAccessor(context);
  }

  @Override
  protected Record retrieveDuringStore(Cursor cur) {
    return RepoUtils.historyFromMirrorCursor(cur);
  }

  @Override
  protected Record retrieveDuringFetch(Cursor cur) {
    return RepoUtils.historyFromMirrorCursor(cur);
  }

  @Override
  protected String buildRecordString(Record record) {
    EventRecord hist = (EventRecord) record;
    return hist.guid;
  }

  @Override
  public boolean shouldIgnore(Record record) {
    if (super.shouldIgnore(record)) {
      return true;
    }
    if (!(record instanceof EventRecord)) {
      return true;
    }
    return false;
  }

  @Override
  protected Record prepareRecord(Record record) {
    return record;
  }

  @Override
  public void abort() {
    if (dbHelper != null) {
      ((MileageDataAccessor) dbHelper).closeExtender();
      dbHelper = null;
    }
    super.abort();
  }

  @Override
  public void finish(final RepositorySessionFinishDelegate delegate) throws InactiveSessionException {
    if (dbHelper != null) {
      ((MileageDataAccessor) dbHelper).closeExtender();
      dbHelper = null;
    }
    super.finish(delegate);
  }

  protected Object recordsBufferMonitor = new Object();
  protected ArrayList<EventRecord> recordsBuffer = new ArrayList<EventRecord>();

  /**
   * Queue record for insertion, possibly flushing the queue.
   * <p>
   * Must be called on <code>storeWorkQueue</code> thread! But this is only
   * called from <code>store</code>, which is called on the queue thread.
   *
   * @param record
   *          A <code>Record</code> with a GUID that is not present locally.
   */
  @Override
  protected void insert(Record record) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    enqueueNewRecord((EventRecord) prepareRecord(record));
  }

  /**
   * Batch incoming records until some reasonable threshold is hit or storeDone
   * is received.
   * <p>
   * Must be called on <code>storeWorkQueue</code> thread!
   *
   * @param record A <code>Record</code> with a GUID that is not present locally.
   * @throws NullCursorException
   */
  protected void enqueueNewRecord(EventRecord record) throws NullCursorException {
    synchronized (recordsBufferMonitor) {
      if (recordsBuffer.size() >= INSERT_RECORD_THRESHOLD) {
        flushNewRecords();
      }
      Logger.debug(LOG_TAG, "Enqueuing new record with GUID " + record.guid);
      recordsBuffer.add(record);
    }
  }

  /**
   * Flush queue of incoming records to database.
   * <p>
   * Must be called on <code>storeWorkQueue</code> thread!
   * <p>
   * Must be locked by recordsBufferMonitor!
   * @throws NullCursorException
   */
  protected void flushNewRecords() throws NullCursorException {
    if (recordsBuffer.size() < 1) {
      Logger.debug(LOG_TAG, "No records to flush, returning.");
      return;
    }

    final ArrayList<EventRecord> outgoing = recordsBuffer;
    recordsBuffer = new ArrayList<EventRecord>();
    Logger.debug(LOG_TAG, "Flushing " + outgoing.size() + " records to database.");
    // TODO: move bulkInsert to AndroidBrowserDataAccessor?
    int inserted = ((MileageDataAccessor) dbHelper).bulkInsert(outgoing);
    if (inserted != outgoing.size()) {
      // Something failed; most pessimistic action is to declare that all insertions failed.
      // TODO: perform the bulkInsert in a transaction and rollback unless all insertions succeed?
      for (EventRecord failed : outgoing) {
        delegate.onRecordStoreFailed(new RuntimeException("Failed to insert history item with guid " + failed.guid + "."), failed.guid);
      }
      return;
    }

    // All good, everybody succeeded.
    for (EventRecord succeeded : outgoing) {
      try {
        // Does not use androidID -- just GUID -> String map.
        updateBookkeeping(succeeded);
      } catch (NoGuidForIdException e) {
        // Should not happen.
        throw new NullCursorException(e);
      } catch (ParentNotFoundException e) {
        // Should not happen.
        throw new NullCursorException(e);
      } catch (NullCursorException e) {
        throw e;
      }
      trackRecord(succeeded);
      delegate.onRecordStoreSucceeded(succeeded.guid); // At this point, we are really inserted.
    }
  }

  @Override
  public void storeDone() {
    storeWorkQueue.execute(new Runnable() {
      @Override
      public void run() {
        synchronized (recordsBufferMonitor) {
          try {
            flushNewRecords();
          } catch (Exception e) {
            Logger.warn(LOG_TAG, "Error flushing records to database.", e);
          }
        }
        storeDone(System.currentTimeMillis());
      }
    });
  }
}
