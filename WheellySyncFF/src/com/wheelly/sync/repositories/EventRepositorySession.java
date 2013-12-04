/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync.repositories;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import com.wheelly.sync.DateUtils;
import com.wheelly.sync.repositories.domain.EventRecord;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class EventRepositorySession extends RepositorySession {
	
	private final Context context;
	
	public EventRepositorySession(Repository repository, Context context) {
		super(repository);
		this.context = context;
	}
	
	void ensureGuids() {
		ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				new String[] { "h." + BaseColumns._ID },
				"h.sync_etag IS NULL", null, null);
		
		Uri uri = Uri.parse("content://com.wheelly/heartbeats/");
		
		if(c.moveToFirst()) {
			do {
				long id = c.getLong(0);
				ContentValues values = new ContentValues();
				values.put("sync_etag", Utils.generateGuid());
				cr.update(ContentUris.withAppendedId(uri, id), values, null, null);
			} while(c.moveToNext());
		}
	}
	
	@Override
	public void guidsSince(long timestamp,
			RepositorySessionGuidsSinceDelegate delegate) {
		ensureGuids();
		ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				new String[] { "h.sync_etag guid" },
				"h.modified >= ?",
				new String[] { Long.toString(timestamp) },
				"odometer ASC, h._created ASC");
		List<String> guids = new ArrayList<String>();
		
		if(c.moveToFirst()) {
			do {
				guids.add(c.getString(0));
			} while(c.moveToNext());
		}
		
		delegate.onGuidsSinceSucceeded(guids.toArray(new String[0]));
	}
	
	public static final String TypeColumnExpression =
			"CASE WHEN r._id IS NOT NULL THEN 'REFUEL'"
			+ "	WHEN m1._id IS NOT NULL THEN 'START'"
			+ "	WHEN m2._id IS NOT NULL THEN 'STOP'"
			+ " END";
	
	public static final String[] ListProjection = {
		"h." + BaseColumns._ID + " " + BaseColumns._ID,
		"h._created",
		"h.odometer",
		"h.fuel",
		"l.name place",
		"m2.mileage distance",
		"m2.track_id track_id",
		"ls.name destination",
		"r.cost",
		"r.amount",
		"r.transaction_id",
		TypeColumnExpression + " type",
		"h.sync_etag guid",
		"h.modified"
	};
	
	@Override
	public void fetchSince(long timestamp,
			RepositorySessionFetchRecordsDelegate delegate) {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		Cursor c = null;
		try {
			c= cr.query(Uri.parse("content://com.wheelly/timeline"),
					ListProjection,
					"h.modified >= ?",
					new String[] { Long.toString(timestamp) },
					"odometer ASC, h._created ASC");
			
			if(c.moveToFirst()) {
				do {
					
					delegate.onFetchedRecord(recordFromCursor(c));
				} while(c.moveToNext());
			}
		} finally {
			if(null != c) {
				c.close();
			}
		}
		
		delegate.onFetchCompleted(end);
	}

	private static EventRecord recordFromCursor(Cursor c) {
		EventRecord r = new EventRecord();
		r.amount		= c.getFloat(c.getColumnIndex("amount"));
		r.androidID		= c.getLong(c.getColumnIndex(BaseColumns._ID));
		r.cost			= c.getFloat(c.getColumnIndex("cost"));
		r.date			= DateUtils.parseDate(c.getString(c.getColumnIndex("_created")));
		r.destination	= c.getString(c.getColumnIndex("destination"));
		r.distance		= c.getFloat(c.getColumnIndex("distance"));
		r.fuel			= c.getFloat(c.getColumnIndex("fuel"));
		r.guid			= c.getString(c.getColumnIndex("guid"));
		r.lastModified  = c.getLong(c.getColumnIndex("modified"));
		r.location		= c.getString(c.getColumnIndex("place"));
		r.map			= c.getString(c.getColumnIndex("track_id"));
		r.odometer		= c.getLong(c.getColumnIndex("odometer"));
		r.type			= c.getString(c.getColumnIndex("type"));
		return r;
	}
	
	@Override
	public void fetch(String[] guids,
			RepositorySessionFetchRecordsDelegate delegate)
			throws InactiveSessionException {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				ListProjection,
				"guid IN (" + new String(new char[guids.length]).replace("\0", "?,").substring(0, -1) +  ")",
				guids,
				"odometer ASC, h._created ASC");
		
		if(c.moveToFirst()) {
			do {
				delegate.onFetchedRecord(recordFromCursor(c));
			} while(c.moveToNext());
		}
		
		delegate.onFetchCompleted(end);
	}

	@Override
	public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		Cursor c = null;
		
		try {
			c = cr.query(Uri.parse("content://com.wheelly/timeline"),
					ListProjection,
					null,
					null,
					"odometer ASC, h._created ASC");
			
			if(c.moveToFirst()) {
				do {
					delegate.onFetchedRecord(recordFromCursor(c));
				} while(c.moveToNext());
			}
		} finally {
			if(c != null) {
				c.close();
			}
		}
		
		delegate.onFetchCompleted(end);
	}
	
	private long resolveLocation(String name, EventRecord er) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.parse("content://com.wheelly/locations");
		Cursor c = null;
		try {
			c = cr.query(uri, new String[] { BaseColumns._ID }, "name = ?", new String[] { name }, null);
			if(c.moveToFirst()) {
				long id = c.getLong(0);
				if(!c.moveToNext()) {
					return id;
				}
			}
		} finally {
			if(null != c) {
				c.close();
			}
		}
		ContentValues values = new ContentValues();
		values.put("name",		name);
		values.put("datetime",	er.date);
		return ContentUris.parseId(cr.insert(uri, values));
	}
	
	
	long lastVacantHeartbeatId;
	
	private long storeHeartbeat(EventRecord er) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.parse("content://com.wheelly/heartbeats");
		
		ContentValues values = new ContentValues();
		values.put("_created",	DateUtils.toDate(er.date));
		values.put("odometer",	er.odometer);
		values.put("fuel",		er.fuel.floatValue());
		values.put("sync_etag",	er.guid);
		values.put("modified",	er.lastModified);
		if(null != er.location) {
			values.put("place_id",	resolveLocation(er.location, er));
		}
		
		if(er.androidID >= 0) {
			cr.update(ContentUris.withAppendedId(uri, er.androidID), values, null, null);
		} else {
			return ContentUris.parseId(cr.insert(uri, values));
		}
		
		return er.androidID;
	}
	
	private long storeRefuel(EventRecord er) {
		ContentValues values = new ContentValues();
		values.put("heartbeat_id",	er.androidID);
		values.put("_created",		DateUtils.toDate(er.date));
		values.put("_modified",		er.lastModified);
		values.put("amount",		er.amount.floatValue());
		values.put("cost",			er.cost.floatValue());
		values.put("transaction_id",	er.transaction);
		
		return ContentUris.parseId(context.getContentResolver().insert(Uri.parse("content://com.wheelly/refuels"), values));
	}
	
	private long storeMileage(EventRecord er) {
		ContentValues values = new ContentValues();
		values.put("start_heartbeat_id",	lastVacantHeartbeatId);
		values.put("stop_heartbeat_id",		er.androidID);
		values.put("_created",		DateUtils.toDate(er.date));
		values.put("_modified",		er.lastModified);
		values.put("amount",		er.amount.floatValue());
		values.put("mileage",		er.distance.floatValue());
		values.put("track_id",		er.map);
		if(null != er.destination) {
			values.put("location_id",	resolveLocation(er.destination, er));
		}
		
		return ContentUris.parseId(context.getContentResolver().insert(Uri.parse("content://com.wheelly/mileages"), values));
	}
	
	@Override
	public void store(Record record) throws NoStoreDelegateException {
		EventRecord er = (EventRecord)record;
		er.androidID = storeHeartbeat(er);
		
		if("STOP".equals(er.type)) {
			storeMileage(er);
			lastVacantHeartbeatId = 0;
		} else if("REFUEL".equals(er.type)) {
			storeRefuel(er);
		} else if("START".equals(er.type)) {
			lastVacantHeartbeatId = er.androidID;
		}
	}

	@Override
	public void wipe(RepositorySessionWipeDelegate delegate) {
		// TODO Auto-generated method stub
	}
}