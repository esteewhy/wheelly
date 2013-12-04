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

import com.wheelly.sync.repositories.domain.LocationRecord;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class LocationRepositorySession extends RepositorySession {
	
	private final Context context;
	
	public LocationRepositorySession(Repository repository, Context context) {
		super(repository);
		this.context = context;
	}
	
	void ensureGuids() {
		ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/locations"),
				new String[] { BaseColumns._ID },
				"sync_etag IS NULL", null, null);
		
		Uri uri = Uri.parse("content://com.wheelly/locations/");
		
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
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/locations"),
				new String[] { "sync_etag guid" },
				"modified >= ?",
				new String[] { Long.toString(timestamp) },
				"name ASC");
		List<String> guids = new ArrayList<String>();
		
		if(c.moveToFirst()) {
			do {
				guids.add(c.getString(0));
			} while(c.moveToNext());
		}
		
		delegate.onGuidsSinceSucceeded(guids.toArray(new String[0]));
	}
	
	public static final String[] ListProjection = {
		BaseColumns._ID,
		"name",
		"provider",
		"accuracy",
		"latitude",
		"longitude",
		"resolved_address address",
		"color",
		"sync_etag guid",
		"modified"
	};
	
	@Override
	public void fetchSince(long timestamp,
			RepositorySessionFetchRecordsDelegate delegate) {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		Cursor c = null;
		try {
			c= cr.query(Uri.parse("content://com.wheelly/locations"),
					ListProjection,
					"modified >= ?",
					new String[] { Long.toString(timestamp) },
					"name ASC");
			
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

	private static LocationRecord recordFromCursor(Cursor c) {
		LocationRecord r = new LocationRecord();
		r.name		= c.getString(c.getColumnIndex("name"));
		r.androidID	= c.getLong(c.getColumnIndex(BaseColumns._ID));
		r.provider	= c.getString(c.getColumnIndex("provider"));
		r.accuracy	= c.getFloat(c.getColumnIndex("accuracy"));
		r.latitude	= c.getFloat(c.getColumnIndex("latitude"));
		r.longitude	= c.getFloat(c.getColumnIndex("longitude"));
		r.guid		= c.getString(c.getColumnIndex("guid"));
		r.lastModified  = c.getLong(c.getColumnIndex("modified"));
		r.color		= c.getString(c.getColumnIndex("color"));
		return r;
	}
	
	@Override
	public void fetch(String[] guids,
			RepositorySessionFetchRecordsDelegate delegate)
			throws InactiveSessionException {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		Cursor c = null;
		try {
			c = cr.query(Uri.parse("content://com.wheelly/locations"),
				ListProjection,
				"guid IN (" + new String(new char[guids.length]).replace("\0", "?,").substring(0, -1) +  ")",
				guids,
				"name ASC");
		
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

	@Override
	public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
		ensureGuids();
		
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		Cursor c = null;
		
		try {
			c = cr.query(Uri.parse("content://com.wheelly/locations"),
					ListProjection,
					null,
					null,
					"name ASC");
			
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
	
	private long storeLocation(LocationRecord er) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.parse("content://com.wheelly/locations");
		
		ContentValues values = new ContentValues();
		values.put("sync_etag",	er.guid);
		values.put("modified",	er.lastModified);
		values.put("name",		er.name);
		values.put("provider",	er.provider);
		values.put("accuracy",	er.accuracy.floatValue());
		values.put("latitude",	er.latitude.floatValue());
		values.put("longitude",	er.longitude.floatValue());
		values.put("color",		er.color);
		if(er.androidID >= 0) {
			cr.update(ContentUris.withAppendedId(uri, er.androidID), values, null, null);
		} else {
			return ContentUris.parseId(cr.insert(uri, values));
		}
		return er.androidID;
	}
	
	@Override
	public void store(Record record) throws NoStoreDelegateException {
		storeLocation((LocationRecord)record);
	}

	@Override
	public void wipe(RepositorySessionWipeDelegate delegate) {
		// TODO Auto-generated method stub
	}
}