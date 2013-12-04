package com.wheelly.sync.repositories.domain;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class LocationRecord extends Record {
  private static final String LOG_TAG = "LocationRecord";

  public static final String COLLECTION_NAME = "locations";

  public LocationRecord(String guid, String collection, long lastModified, boolean deleted) {
    super(guid, collection, lastModified, deleted);
    this.ttl = 0;
  }
  public LocationRecord(String guid, String collection, long lastModified) {
    this(guid, collection, lastModified, false);
  }
  public LocationRecord(String guid, String collection) {
    this(guid, collection, 0, false);
  }
  public LocationRecord(String guid) {
    this(guid, COLLECTION_NAME, 0, false);
  }
  public LocationRecord() {
    this(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }
  
  public String name;
  public String provider;
  public Number accuracy;
  public Number latitude;
  public Number longitude;
  public String address;
  public String color;
  
  @Override
  public Record copyWithIDs(String guid, long androidID) {
    LocationRecord out = new LocationRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;
    out.ttl       = this.ttl;
    
    out.name      = this.name;
    out.provider  = this.provider;
    out.accuracy  = this.accuracy;
    out.latitude  = this.latitude;
    out.longitude = this.longitude;
    out.address   = this.address;
    out.color     = this.color;
    return out;
  }

  @Override
  protected void populatePayload(ExtendedJSONObject payload) {
    putPayload(payload, "id",      this.guid);
    payload.put("name",      this.name);
    payload.put("provider",  this.provider);
    payload.put("accuracy",  this.accuracy);
    payload.put("latitude",  this.latitude);
    payload.put("longitude", this.longitude);
    payload.put("address",   this.address);
    payload.put("color",     this.color);
  }

  @Override
  protected void initFromPayload(ExtendedJSONObject payload) {
    this.name        = payload.getString("name");
    this.provider    = payload.getString("provider");
    this.accuracy    = (Number) payload.get("accuracy");
    this.latitude    = (Number) payload.get("latitude");
    this.longitude   = (Number) payload.get("longitude");
    this.address     = payload.getString("address");
    this.color       = payload.getString("color");
  }
  
  @Override
  public boolean congruentWith(Object o) {
    if (o == null || !(o instanceof LocationRecord)) {
      return false;
    }
    LocationRecord other = (LocationRecord) o;
    if (!super.congruentWith(other)) {
      return false;
    }
    return RepoUtils.stringsEqual(name, other.name)
      && latitude == other.latitude
      && longitude == other.longitude;
  }

  @Override
  public boolean equalPayloads(Object o) {
    if (o == null || !(o instanceof LocationRecord)) {
      Logger.debug(LOG_TAG, "Not a LocationRecord: " + o.getClass());
      return false;
    }
    LocationRecord other = (LocationRecord) o;
    
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
        congruentWith(o)
        && RepoUtils.stringsEqual(provider, other.provider)
        && accuracy == other.accuracy
        && RepoUtils.stringsEqual(address, other.address)
        && RepoUtils.stringsEqual(color, other.color);
  }
}