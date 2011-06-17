package com.wheelly.db;

import com.wheelly.db.DatabaseSchema.Refuels;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

/**
 * Higher level abstraction over refuel persistence to accommodate
 * database connection management.
 */
public class RefuelBroker {
	
	private final Context context;
	
	public RefuelBroker(Context context)
	{
		this.context = context;
	}
	
	public ContentValues loadOrCreate(long id) {
		final ContentResolver cr = context.getContentResolver();
		final Uri uri = ContentUris.appendId(Refuels.CONTENT_URI.buildUpon(), id).build();
		final Cursor cursor =
			id > 0
				? cr.query(uri, Refuels.SingleProjection,
					BaseColumns._ID + " = ?",
					new String[] { Long.toString(id) },
					null)
				: cr.query(uri, Refuels.DefaultProjection,
					null,
					// pass parameter to some projection fields
					new String[] {
						Integer.toString(PreferenceManager.getDefaultSharedPreferences(context).getInt("fuel_capacity", 60))
					},
					"_created DESC LIMIT 1");
		
		try {
			if(cursor.moveToFirst()) {
				return deserialize(cursor);
			} else {
				final ContentValues r = new ContentValues();
				r.put(BaseColumns._ID,	0);
				r.put("name",			"First refuel!");
				//r.put("_created",		"");
				r.put("transaction_id",	0);
				r.put("heartbeat_id",	-1);
				r.put("calc_mileage",	0);
				r.put("amount",			0);
				r.put("cost",			0);
				r.put("unit_price",		0);
				return r;
			}
		} finally {
			cursor.close();
		}
	}
	
	public long updateOrInsert(ContentValues refuel, ContentValues heartbeat) {
		long id = refuel.getAsLong(BaseColumns._ID);
		refuel.put("heartbeat_id", new HeartbeatBroker(context).updateOrInsert(heartbeat));
		ContentResolver cr = context.getContentResolver();
			
		if(id > 0) {
			cr.update(
				ContentUris.withAppendedId(Refuels.CONTENT_URI, id),
				refuel, null, null);
			return id;
		} else {
			refuel.remove(BaseColumns._ID);
			return ContentUris.parseId(cr.insert(Refuels.CONTENT_URI, refuel));
		}
	}
	
	public static ContentValues deserialize(Cursor cursor) {
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));
		values.put("name",			cursor.getString(cursor.getColumnIndexOrThrow("name")));
		values.put("_created",		cursor.getString(cursor.getColumnIndexOrThrow("_created")));
		values.put("transaction_id",cursor.getLong(cursor.getColumnIndexOrThrow("transaction_id")));
		values.put("heartbeat_id",	cursor.getLong(cursor.getColumnIndexOrThrow("heartbeat_id")));
		values.put("calc_mileage",	cursor.getFloat(cursor.getColumnIndexOrThrow("calc_mileage")));
		values.put("amount",		cursor.getFloat(cursor.getColumnIndexOrThrow("amount")));
		values.put("cost",			cursor.getFloat(cursor.getColumnIndexOrThrow("cost")));
		values.put("unit_price",	cursor.getFloat(cursor.getColumnIndexOrThrow("unit_price")));
		return values;
	}
}