/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.wheelly.sync;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;
import android.content.ContentResolver;
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
	
	@Override
	public void guidsSince(long timestamp,
			RepositorySessionGuidsSinceDelegate delegate) {
		ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				new String[] { "h.sync_etag" },
				"h._created >= ?",
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
	
	public static final String IconColumnExpression =
			"((r._id IS NOT NULL) * 4"
			+ "	| (m1._id IS NOT NULL) * 2"
			+ "	| (m2._id IS NOT NULL))";
	
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
		IconColumnExpression + " icons",
		"h.sync_id",
		"h.sync_etag",
		"h.sync_date",
		"h.sync_state"
	};
	
	@Override
	public void fetchSince(long timestamp,
			RepositorySessionFetchRecordsDelegate delegate) {
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				ListProjection,
				"h._created >= ?",
				new String[] { Long.toString(timestamp) },
				"odometer ASC, h._created ASC");
		
		if(c.moveToFirst()) {
			do {
				
				delegate.onFetchedRecord(recordFromCursor(c));
			} while(c.moveToNext());
		}
		
		delegate.onFetchCompleted(end);
	}
	
	private static EventRecord recordFromCursor(Cursor c) {
		EventRecord r = new EventRecord();
		r.amount		= c.getFloat(c.getColumnIndex("amount"));
		r.androidID		= c.getLong(c.getColumnIndex(BaseColumns._ID));
		r.cost			= c.getFloat(c.getColumnIndex("cost"));
		r.date			= c.getLong(c.getColumnIndex("_created"));
		r.destination	= c.getString(c.getColumnIndex("destination"));
		r.distance		= c.getFloat(c.getColumnIndex("distance"));
		r.fuel			= c.getFloat(c.getColumnIndex("fuel"));
		r.guid			= c.getString(c.getColumnIndex("sync_etag"));
		r.lastModified	= c.getLong(c.getColumnIndex("sync_state"));;
		r.location		= c.getString(c.getColumnIndex("place"));
		r.map			= c.getString(c.getColumnIndex("track_id"));
		r.odometer		= c.getLong(c.getColumnIndex("odometer"));
		r.type			= c.getString(c.getColumnIndex("icons"));
		return r;
	}
	
	@Override
	public void fetch(String[] guids,
			RepositorySessionFetchRecordsDelegate delegate)
			throws InactiveSessionException {
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				ListProjection,
				"sync_etag IN (" + new String(new char[guids.length]).replace("\0", "?,").substring(0, -1) +  ")",
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
		ContentResolver cr = context.getContentResolver();
		final long end = now();
		final Cursor c = cr.query(Uri.parse("content://com.wheelly/timeline"),
				ListProjection,
				null,
				null,
				"odometer ASC, h._created ASC");
		
		if(c.moveToFirst()) {
			do {
				delegate.onFetchedRecord(recordFromCursor(c));
			} while(c.moveToNext());
		}
		
		delegate.onFetchCompleted(end);
	}

	@Override
	public void store(Record record) throws NoStoreDelegateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void wipe(RepositorySessionWipeDelegate delegate) {
		// TODO Auto-generated method stub
	}
}