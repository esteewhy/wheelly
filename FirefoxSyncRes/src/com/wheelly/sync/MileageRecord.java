package com.wheelly.sync;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class MileageRecord extends Record {
  private static final String LOG_TAG = "MileageRecord";

  public static final String COLLECTION_NAME = "mileages";
  public static final long HISTORY_TTL = 60 * 24 * 60 * 60; // 60 days in seconds.

  public MileageRecord(String guid, String collection, long lastModified, boolean deleted) {
    super(guid, collection, lastModified, deleted);
    this.ttl = HISTORY_TTL;
  }
  public MileageRecord(String guid, String collection, long lastModified) {
    this(guid, collection, lastModified, false);
  }
  public MileageRecord(String guid, String collection) {
    this(guid, collection, 0, false);
  }
  public MileageRecord(String guid) {
    this(guid, COLLECTION_NAME, 0, false);
  }
  public MileageRecord() {
    this(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }
  
  public long trackId;
  public long startHeartbeatId;
  public long stopHeartbeatId;
  public long locationId;
  public float mileage;
  public float amount;
  
  @Override
  public Record copyWithIDs(String guid, long androidID) {
    MileageRecord out = new MileageRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;
    out.ttl       = this.ttl;
    
    out.trackId           = this.trackId;
    out.startHeartbeatId  = this.startHeartbeatId;
    out.stopHeartbeatId   = this.stopHeartbeatId;
    out.locationId        = this.locationId;
    out.mileage           = this.mileage;
    out.amount            = this.amount;
    
    return out;
  }

  @Override
  protected void populatePayload(ExtendedJSONObject payload) {
    putPayload(payload, "id",      this.guid);
    payload.put("trackId", this.trackId);
    payload.put("startHeartbeatId", this.startHeartbeatId);
    payload.put("stopHeartbeatId", this.stopHeartbeatId);
    payload.put("locationId", this.locationId);
    payload.put("mileage", this.mileage);
    payload.put("amount", this.amount);
  }

  @Override
  protected void initFromPayload(ExtendedJSONObject payload) {
    this.trackId          = (Long) payload.get("trackId");
    this.startHeartbeatId = (Long) payload.get("startHeartbeatId");
    this.stopHeartbeatId  = (Long) payload.get("stopHeartbeatId");
    this.locationId       = (Long) payload.get("locationId");
    this.mileage          = (Float) payload.get("mileage");
    this.amount           = (Float) payload.get("amount");
  }

  /**
   * We consider two history records to be congruent if they represent the
   * same history record regardless of visits. Titles are allowed to differ,
   * but the URI must be the same.
   */
  @Override
  public boolean congruentWith(Object o) {
    if (o == null || !(o instanceof MileageRecord)) {
      return false;
    }
    MileageRecord other = (MileageRecord) o;
    if (!super.congruentWith(other)) {
      return false;
    }
    return Float.compare(this.mileage, other.mileage) == 0;
  }

  @Override
  public boolean equalPayloads(Object o) {
    if (o == null || !(o instanceof MileageRecord)) {
      Logger.debug(LOG_TAG, "Not a HistoryRecord: " + o.getClass());
      return false;
    }
    MileageRecord other = (MileageRecord) o;
    if (!super.equalPayloads(other)) {
      Logger.debug(LOG_TAG, "super.equalPayloads returned false.");
      return false;
    }
    return this.startHeartbeatId == other.startHeartbeatId &&
           this.stopHeartbeatId == other.stopHeartbeatId &&
           this.locationId == other.locationId;
  }

  @Override
  public boolean equalAndroidIDs(Record other) {
    return super.equalAndroidIDs(other) &&
    		this.startHeartbeatId == ((MileageRecord)other).startHeartbeatId &&
            this.stopHeartbeatId == ((MileageRecord)other).stopHeartbeatId &&
            this.locationId == ((MileageRecord)other).locationId;
  }
}