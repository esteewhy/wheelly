package com.wheelly.sync.repositories.domain;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class EventRecord extends Record {
  private static final String LOG_TAG = "EventRecord";

  public static final String COLLECTION_NAME = "events";

  public EventRecord(String guid, String collection, long lastModified, boolean deleted) {
    super(guid, collection, lastModified, deleted);
    this.ttl = 0;
  }
  public EventRecord(String guid, String collection, long lastModified) {
    this(guid, collection, lastModified, false);
  }
  public EventRecord(String guid, String collection) {
    this(guid, collection, 0, false);
  }
  public EventRecord(String guid) {
    this(guid, COLLECTION_NAME, 0, false);
  }
  public EventRecord() {
    this(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }
  
  public long odometer;
  public String type;
  public long date;
  public String location;
  public Number fuel;
  public Number distance;
  public String destination;
  public Number amount;
  public Number cost;
  public String transaction;
  public String map;
  
  @Override
  public Record copyWithIDs(String guid, long androidID) {
    EventRecord out = new EventRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;
    out.ttl       = this.ttl;
    
    out.odometer    = this.odometer;
    out.type        = this.type;
    out.date        = this.date;
    out.location    = this.location;
    out.fuel        = this.fuel;
    out.distance    = this.distance;
    out.destination = this.destination;
    out.amount      = this.amount;
    out.cost        = this.cost;
    out.transaction = this.transaction;
    out.map         = this.map;
    
    return out;
  }

  @Override
  protected void populatePayload(ExtendedJSONObject payload) {
    putPayload(payload, "id",      this.guid);
    payload.put("odometer",    this.odometer);
    payload.put("type",        this.type);
    payload.put("date",        this.date);
    payload.put("location",    this.location);
    payload.put("fuel",        this.fuel);
    payload.put("distance",    this.distance);
    payload.put("destination", this.destination);
    payload.put("amount",      this.amount);
    payload.put("cost",        this.cost);
    payload.put("transaction", this.transaction);
    payload.put("map",         this.map);
  }

  @Override
  protected void initFromPayload(ExtendedJSONObject payload) {
    this.odometer    = payload.getLong("odometer");
    this.type        = payload.getString("type");
    this.date        = payload.getTimestamp("date");
    this.location    = payload.getString("location");
    this.fuel        = (Number) payload.get("fuel");
    this.distance    = (Number) payload.get("distance");
    this.destination = payload.getString("destination");
    this.amount      = (Number) payload.get("amount");
    this.cost        = (Number) payload.get("cost");
    this.transaction = payload.getString("transaction");
    this.map         = payload.getString("map");
  }
  
  @Override
  public boolean congruentWith(Object o) {
    if (o == null || !(o instanceof EventRecord)) {
      return false;
    }
    EventRecord other = (EventRecord) o;
    if (!super.congruentWith(other)) {
      return false;
    }
    return this.odometer == other.odometer
      && RepoUtils.stringsEqual(this.location, other.location);
  }

  @Override
  public boolean equalPayloads(Object o) {
    if (o == null || !(o instanceof EventRecord)) {
      Logger.debug(LOG_TAG, "Not an EventRecord: " + o.getClass());
      return false;
    }
    EventRecord other = (EventRecord) o;
    
    if (this.deleted) {
      if (other.deleted) {
        // Deleted records are equal if their guids match.
        return RepoUtils.stringsEqual(this.guid, other.guid);
      }
      // One record is deleted, the other is not. Not equal.
      return false;
    }
    
    if (!super.equalPayloads(other)) {
      Logger.debug(LOG_TAG, "super.equalPayloads returned false.");
      return false;
    }
    
    return
        this.odometer == other.odometer
        && RepoUtils.stringsEqual(this.type, other.type)
        && this.date == other.date
        && RepoUtils.stringsEqual(this.location, other.location)
        && this.fuel == other.fuel
        && this.distance == other.distance
        && RepoUtils.stringsEqual(this.destination, other.destination)
        && this.amount == other.amount
        && this.cost == other.cost
        && RepoUtils.stringsEqual(this.transaction, other.transaction)
        && RepoUtils.stringsEqual(this.map, other.map);
  }
}