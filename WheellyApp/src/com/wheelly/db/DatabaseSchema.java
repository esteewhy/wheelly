package com.wheelly.db;

import android.content.ContentResolver;
import android.net.Uri;
import static android.provider.BaseColumns._ID;

public final class DatabaseSchema {
	public static final String CONTENT_AUTHORITY = "com.wheelly";
	private static final Uri BASE_CONTENT_URI =
		Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_MILEAGES = "mileages";
	private static final String PATH_REFUELS = "refuels";
	private static final String PATH_HEARTBEATS = "heartbeats";
	private static final String PATH_TIMELINE = "timeline";
	
	public static final class Mileages {
		public static final String LastPendingIdSql =
			"SELECT _id FROM heartbeats WHERE type = 34 LIMIT 1";
		
		public static final String Tables = "mileages m"
			+ " LEFT OUTER JOIN heartbeats start"
			+ " 	ON m.start_heartbeat_id = start." + _ID
			+ " LEFT OUTER JOIN locations start_place"
			+ "		ON start.place_id = start_place." + _ID
			+ " LEFT OUTER JOIN heartbeats stop"
			+ " 	ON m.stop_heartbeat_id = stop." + _ID
			+ " LEFT OUTER JOIN locations stop_place"
			+ "		ON stop.place_id = stop_place." + _ID
			+ " LEFT OUTER JOIN locations dest"
			+ "		ON m.location_id = dest." + _ID
			+ " LEFT OUTER JOIN next_mileages n"
			+ "		ON n.mileage_id = m." + _ID
			+ " LEFT OUTER JOIN prev_mileages p"
			+ "		ON p.mileage_id = m." + _ID;
		
		public static final String[] DefaultProjection = {
			"-1 " + _ID,
			"0 mileage",
			"CURRENT_TIMESTAMP _created",
			"MAX(amount) amount",
			"0 place_id",
			"NULL track_id",
			"CURRENT_TIMESTAMP	name"
		};
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_MILEAGES).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.mileage";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.mileages";
		
		public static final int STATE_NONE = 0;
		public static final int STATE_ACTIVE = 1;
		public static final int STATE_TRACKING = 2;
	}
	
	public static final class Refuels {
		public static final String[] SingleProjection = {
			_ID,
			"name",
			"cost",
			"amount",
			"unit_price",
			"_created",
			"transaction_id",
			"place_id",
			"type",
			"odometer",
			"fuel",
		};
		
		private static final String EstimateAmount =
			"SELECT ?1 - fuel FROM heartbeats ORDER BY odometer DESC LIMIT 1";
		
		private static final String EstimateCost =
			"SELECT f.cost * (?1 - fuel) / f.amount FROM heartbeats f"
			+ " WHERE f.type = 4"
			+ " ORDER BY f.odometer DESC LIMIT 1";
		
		private static final String LastPrice =
			"SELECT f.unit_price FROM heartbeats f WHERE f.type = 4 ORDER BY odometer DESC LIMIT 1";
		
		public static final String[] DefaultProjection = {
			"0 _id",
			"'' name",
			"(" + EstimateAmount + ") amount",
			"(" + LastPrice + ") unit_price",
			"CURRENT_TIMESTAMP _created",
			"NULL transaction_id",
			"(" + EstimateCost +") cost",
			"odometer",
			"fuel",
			"0 place_id",
			"4 type"
		};
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_REFUELS).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.refuel";
	}
	
	public static final class Heartbeats {
		public static final String[] ListProjection = {
			"h._id",
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"h.sync_state",
			"h.type",
			"l.color color",
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l ON h.place_id = l." + _ID
			+ " LEFT JOIN mileages m1 ON m1.start_heartbeat_id = h." + _ID
			+ " LEFT JOIN mileages m2 ON m2.stop_heartbeat_id = h." + _ID
			+ " LEFT JOIN refuels r ON r.heartbeat_id = h." + _ID;
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_HEARTBEATS).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.heartbeat";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.heartbeats";
	}

	public static final class Timeline {
		
		public static final long SYNC_STATE_NONE = 0;
		public static final long SYNC_STATE_READY = 1;
		public static final long SYNC_STATE_CHANGED = 2;
		public static final long SYNC_STATE_CONFLICT = 3;
		
		private static final String EnRouteRefuelAmount =
				"SELECT Sum(r.amount)"
				+ " FROM heartbeats r"
				+ " WHERE r.type = 4 AND r._created BETWEEN start._created AND h._created";
		
		private static final String FuelField =
				"COALESCE(NULLIF(h.amount, 0), h.fuel - start.fuel - COALESCE((" + EnRouteRefuelAmount + "), 0), h.fuel)";
		
		private static final String LegTypeField =
				"(CASE WHEN h.type != 4 THEN"
				+ "	CASE WHEN h.type = 1 THEN"
				+ "			CASE WHEN p._id IS NOT NULL THEN 5"//RESTART
				+ "			ELSE 1 END"//START
				+ "		WHEN h.type = 2 THEN"
				+ "			CASE WHEN n._id IS NOT NULL THEN 3"//PAUSE
				+ "			ELSE 2 END END"//STOP
				+ "	ELSE 4 END)";//REFUEL
		
		public static final String[] ListProjection = {
			"h." + _ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"l.color",
			"h.distance",
			"h.cost",
			FuelField + " amount",
			"h.transaction_id",
			"h.type",
			"h.sync_id",
			"h.sync_etag",
			"h.sync_date",
			"h.sync_state",
			"h.modified",
			LegTypeField + " leg"
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l			ON h.place_id = l._id"
			+ " LEFT JOIN stop_events stop		ON stop.heartbeat_id = h._id"
			+ " LEFT JOIN start_events start	ON start.heartbeat_id = h._id"
			+ " LEFT JOIN locations ls			ON start.place_id = ls._id"
			+ " LEFT JOIN refuels r				ON r.heartbeat_id = h._id"
			+ " LEFT JOIN prev_event p			ON p.heartbeat_id = h._id"
			+ " LEFT JOIN next_event n			ON n.heartbeat_id = h._id";
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_TIMELINE).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.timeline_entry";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.timeline";
	}
}