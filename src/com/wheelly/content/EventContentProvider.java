/*
 * Copyright (C) 2010 Karl Ostmo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wheelly.content;

import java.util.HashMap;
import java.util.Map;

import org.openintents.calendarpicker.contract.CalendarPickerConstants;
import org.openintents.calendarpicker.contract.CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns;

import com.google.common.collect.ObjectArrays;
import com.wheelly.db.DatabaseHelper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

public class EventContentProvider extends ContentProvider {
	
	static final String TAG = "EventContentProvider";
	
	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	public static final String AUTHORITY = "com.wheelly.provider.events";
	
	
	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path("events").build();


	public static Uri constructUri(long data_id) {
		return ContentUris.withAppendedId(BASE_URI, data_id);
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return CalendarPickerConstants.CalendarEventPicker.CONTENT_TYPE_CALENDAR_EVENT;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		long calendar_id = ContentUris.parseId(uri);
		
		DatabaseHelper dbHelper = new DatabaseHelper(getContext());
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(Tables);
		builder.appendWhere(ContentProviderColumns.CALENDAR_ID + " =" + calendar_id);
		builder.setProjectionMap(ListProjection);
		return builder.query(db,
				ObjectArrays.concat(ContentProviderColumns.CALENDAR_ID, projection),
				selection, selectionArgs, null, null, sortOrder);
	}
	
	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
	
	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
	
	private static final String IconColumnExpression =
		"((r._id IS NOT NULL) * 4"
		+ "	| (m1._id IS NOT NULL) * 2"
		+ "	| (m2._id IS NOT NULL))";
	
	private static final Map<String, String> ListProjection = new HashMap<String, String>();
	
	static {
		ListProjection.put(BaseColumns._ID, "h." + BaseColumns._ID);
		ListProjection.put(ContentProviderColumns.TITLE,
				IconColumnExpression + " + ' ' + h.odometer + ' ' + h.fuel + ' ' + l.name + ' ' + h._created "
					+ ContentProviderColumns.TITLE);
		ListProjection.put(ContentProviderColumns.TIMESTAMP, "h._created "
					+ ContentProviderColumns.TIMESTAMP);
		ListProjection.put(ContentProviderColumns.CALENDAR_ID, IconColumnExpression
					+ " " + ContentProviderColumns.CALENDAR_ID);
	};
	
	private static final String Tables = "heartbeats h"
		+ " LEFT JOIN locations l ON h.place_id = l." + BaseColumns._ID
		+ " LEFT JOIN mileages m1 ON m1.start_heartbeat_id = h." + BaseColumns._ID
		+ " LEFT JOIN mileages m2 ON m2.stop_heartbeat_id = h." + BaseColumns._ID
		+ " LEFT JOIN locations ls ON m2.location_id = ls." + BaseColumns._ID
		+ " LEFT JOIN refuels r ON r.heartbeat_id = h." + BaseColumns._ID;
}