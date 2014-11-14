package com.wheelly.db;

import java.util.HashMap;
import java.util.Map;

import com.wheelly.util.FilterUtils.F;

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
	private static final String PATH_LOCATIONS = "locations";
	
	public static final class Mileages {
		public static final String Create = 
			"CREATE TABLE mileages ("
			+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "_modified	TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "name			TEXT,"
			+ "track_id		LONG,"
			
			+ "start_heartbeat_id	LONG,"
			+ "stop_heartbeat_id	LONG,"
			
			+ "location_id	LONG,"
			
			+ "mileage		NUMERIC,"
			+ "amount		NUMERIC,"
			+ "vehicle_id	LONG,"
			//TODO Calculated fields.
			+ "calc_cost	NUMERIC,"
			+ "calc_amount	NUMERIC"
			/// Constraints arent's supported prior 2.2
			//+ ",FOREIGN KEY start_heartbeat_id REFERENCES heartbeats(_id)"
			//+ ",FOREIGN KEY stop_heartbeat_id REFERENCES heartbeats(_id)"
			+ ")";
		
		public static final String LastPendingIdSql =
			"SELECT m." + _ID + ""
				+ " FROM mileages m"
				+ " LEFT OUTER JOIN heartbeats start"
				+ "		ON m.start_heartbeat_id = start." + _ID
				+ " LEFT OUTER JOIN heartbeats stop"
				+ "		ON m.stop_heartbeat_id = stop." + _ID
				+ " WHERE start." + _ID + " IS NULL"
				+ "		OR stop." + _ID + " IS NULL"
				+ " LIMIT 1";
		
		//Calculates amount of refuels occurred in progress of a given trip.
		private static final String EnRouteRefuelAmount =
			"SELECT Sum(r.amount)"
			+ " FROM refuels r"
			+ " INNER JOIN heartbeats rh"
			+ "  ON r.heartbeat_id = rh." + _ID
			+ " WHERE rh._created BETWEEN start._created AND stop._created"
			//not strictly necessary, but to reassure order
			;//+ "  AND rh.odometer BETWEEN start.odometer AND stop.odometer";
		
		private static final String StateColumnExpression =
			"(CASE WHEN m.track_id < 0 THEN 2"
			+" WHEN stop." + _ID + " IS NULL THEN 1"
			+" ELSE 0"
			+" END)";
		
		private static final String FuelField =
			"COALESCE(stop.fuel - start.fuel - COALESCE((" + EnRouteRefuelAmount + "), 0), m.calc_amount)";
		
		private static final String LegTypeField =
			"(CASE"
			+ " WHEN n." + _ID + " IS NULL AND p." + _ID + " IS NOT NULL THEN 1"
			+ " WHEN n." + _ID + " IS NOT NULL AND p." + _ID + " IS NULL THEN 2"
			+ " WHEN n." + _ID + " IS NOT NULL AND p." + _ID + " IS NOT NULL THEN 3"
			+ " ELSE 0 END)";
		
		public static final String[] ListProjection = {
			"m." + _ID,
			"COALESCE(stop._created, start._created, m._created) _created",
			"m.mileage",
			"m.calc_cost cost",
			//TODO Calculate in a scheduled async. job
			FuelField + " fuel",
			"start_place.name start_place",
			"stop_place.name stop_place",
			"start_place.color start_color",
			"stop_place.color stop_color",
			"dest.name destination",
			StateColumnExpression + " state",
			LegTypeField + " leg",
		};
		
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
		
		public static final String[] SINGLE_VIEW_PROJECTION = {
			"m." + _ID,
			"COALESCE(stop._created, start._created, m._created) _created",
			"m.mileage",
			"m.calc_cost cost",
			//TODO Calculate in a scheduled async. job
			FuelField + " fuel",
			"start_place.name start_place",
			"stop_place.name stop_place",
			"start._created start_time",
			"dest.name destination",
			"CASE WHEN m.mileage > 0 THEN -100.0 * " + FuelField + " / m.mileage ELSE 0 END consumption"
		};
		
		public static final String[] SingleEditProjection = {
			_ID,
			"_created",
			"name",
			"track_id",
			"_id start_heartbeat_id",
			"(SELECT start._id FROM start_events start WHERE start.heartbeat_id = m._id) stop_heartbeat_id",
			"m._id as stop_heartbeat_id",
			"distance",
			"amount",
			"location_id",
		};
		
		public static final String[] DefaultProjection = {
			"-1 " + _ID,
			"0 mileage",
			"CURRENT_TIMESTAMP _created",
			"MAX(m.amount) amount",
			"0 location_id",
			"NULL track_id",
			"CURRENT_TIMESTAMP	name"
		};
		
		public static final Map<String, String> FilterExpr = new HashMap<String, String>();
		static {
			FilterExpr.put(F.LOCATION, "stop.place_id = ?1 OR start.place_id = @location_id OR m.location_id = @location_id");
			FilterExpr.put(F.PERIOD, "stop._created BETWEEN @from AND @to AND start._created BETWEEN @from AND @to");
			FilterExpr.put(F.SORT_ORDER, "COALESCE(stop._created, start._created, m._created)");
		}
		
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
		public static final String Create =
			"CREATE TABLE refuels ("
			+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "_modified	TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "name			TEXT,"
			+ "amount		NUMERIC,"
			+ "unit_price	NUMERIC,"
			+ "cost			NUMERIC,"
			+ "fuel_type	TEXT,"
			+ "transaction_id INTEGER,"
			+ "is_full		INTEGER NOT NULL DEFAULT 1,"
			+ "heartbeat_id	LONG,"
			
			+ "calc_mileage	NUMERIC,"
			+ "vehicle_id	LONG"
			//+ ",FOREIGN KEY(heartbeat_id) REFERENCES heartbeats(_id)"
			+ ")";
		
		private static final String SelectPreviousHeartbeat =
			"SELECT h.odometer - hh.odometer"
			+ " FROM heartbeats hh"
			+ " INNER JOIN refuels ff ON ff.heartbeat_id = hh." + _ID
			+ " WHERE hh._created < h._created"
			+ " ORDER BY hh._created DESC"
			+ " LIMIT 1";
		
		public static final String[] ListProjection = {
			"f." + _ID,
			"f.name",
			//"f.calc_mileage mileage",
			"(" + SelectPreviousHeartbeat + ") mileage",
			"f.cost",
			"f.amount",
			"IFNULL(h._created, f._created) _created",
			"l.name place",
			"l.color color",
		};
		
		public static final String Tables = "refuels f"
			+ " LEFT OUTER JOIN heartbeats h"
			+ "		ON f.heartbeat_id = h." + _ID
			+ " LEFT OUTER JOIN locations l"
			+ "		ON h.place_id = l." + _ID;
		
		public static final String[] SingleProjection = {
			_ID,
			"name",
			"calc_mileage",
			"cost",
			"amount",
			"unit_price",
			"_created",
			"transaction_id",
			"heartbeat_id"
		};
		
		private static final String EstimateAmount =
			"SELECT ?1 - fuel FROM heartbeats ORDER BY _created DESC LIMIT 1";
		
		private static final String EstimateCost =
			"SELECT f.cost * (?1 - fuel) / f.amount FROM refuels f"
			+ " 		LEFT OUTER JOIN heartbeats hh"
			+ "		ON hh._id = f.heartbeat_id"
			+ "		ORDER BY IFNULL(hh._created, f._created) DESC LIMIT 1";
		
		public static final String[] DefaultProjection = {
			"0 _id",
			"1 is_full",
			"'' name",
			" NULL calc_mileage",
			"(" + EstimateAmount + ") amount",
			" NULL unit_price",
			"CURRENT_TIMESTAMP _created",
			"NULL transaction_id",
			"NULL heartbeat_id",
			"(" + EstimateCost +") cost"
		};
		
		public static final Map<String, String> FilterExpr = new HashMap<String, String>();
		static {
			FilterExpr.put(F.LOCATION, "h.place_id = @location_id");
			FilterExpr.put(F.PERIOD, "h._created BETWEEN @from AND @to");
			FilterExpr.put(F.SORT_ORDER, "h._created");
		}
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_REFUELS).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.refuel";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.refuels";
	}
	
	public static final class Heartbeats {
		public static final String[] ListProjection = {
			"h." + _ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"h.sync_state",
			"h.type icons",
			"l.color color",
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l ON h.place_id = l." + _ID
			+ " LEFT JOIN mileages m1 ON m1.start_heartbeat_id = h." + _ID
			+ " LEFT JOIN mileages m2 ON m2.stop_heartbeat_id = h." + _ID
			+ " LEFT JOIN refuels r ON r.heartbeat_id = h." + _ID;
		
		public static final Map<String, String> FilterExpr = new HashMap<String, String>();
		static {
			FilterExpr.put(F.LOCATION, "h.place_id = @location_id");
			FilterExpr.put(F.PERIOD, "h._created BETWEEN @from AND @to");
			FilterExpr.put(F.SORT_ORDER, "h.odometer DESC, h._created");
		}
		
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
				"COALESCE(h.amount, h.fuel - start.fuel - COALESCE((" + EnRouteRefuelAmount + "), 0), h.fuel)";
		
		private static final String LegTypeField =
				"(CASE WHEN type != 4 THEN"
				+ "	CASE WHEN type = 1 THEN"
				+ "			CASE WHEN p._id IS NOT NULL THEN 5"//RESTART
				+ "			WHEN stop._id IS NOT NULL 0 THEN 1"//START
				+ "			ELSE 6 END"//RUNNING
				+ "		WHEN type = 2 THEN"
				+ "			CASE WHEN n._id IS NOT NULL THEN 3"//PAUSE
				+ "			ELSE 2 END END"//STOP
				+ "	ELSE 4 END)";//REFUEL
		
		public static final String[] ListProjection = {
			"h." + _ID + " " + _ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"l.color",
			"h.distance",
			"h.cost",
			FuelField + " amount",
			"h.transaction_id",
			"h.type icons",
			"h.sync_id",
			"h.sync_etag",
			"h.sync_date",
			"h.sync_state",
			"h.modified",
			LegTypeField + " leg"
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l			ON h.place_id = l._id"
			+ " LEFT JOIN stop_eventss stop		ON stop.heartbeat_id = h._id"
			+ " LEFT JOIN start_events start	ON start.heartbeat_id = h._id"
			+ " LEFT JOIN locations ls			ON start.location_id = ls._id"
			+ " LEFT JOIN refuels r				ON r.heartbeat_id = h._id"
			+ " LEFT JOIN prev_event p			ON p.heartbeat_id = h._id"
			+ " LEFT JOIN next_event n			ON n.heartbeat_id = h._id";
		
		public static final Map<String, String> FilterExpr = new HashMap<String, String>();
		static {
			FilterExpr.put(F.LOCATION, "h.place_id = @location_id");
			FilterExpr.put(F.PERIOD, "h._created BETWEEN @from AND @to");
			FilterExpr.put(F.SORT_ORDER, "h.odometer DESC, h._created");
		}
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_TIMELINE).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.timeline_entry";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.timeline";
	}
	
	public static final class Locations {
		public static final String Select =
			"SELECT * FROM locations";
		
		public static final String Single =
			"SELECT * FROM locations WHERE _id = ? LIMIT 1";
		
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.location";
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.locations";
	}
}