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
		
		public static final String PrevMileageView =
				"CREATE VIEW prev_mileages AS"
				+ " SELECT m." + _ID + " mileage_id, prev.*"
				+ " FROM mileages m"
				+ " INNER JOIN heartbeats start"
				+ " 	ON m.start_heartbeat_id = start." + _ID
				+ " INNER JOIN heartbeats prev_stop"
				+ " 	ON start.odometer = prev_stop.odometer"
				+ "			AND start.place_id = prev_stop.place_id"
				+ " 		AND start." + _ID + " != prev_stop." + _ID
				+ " INNER JOIN mileages prev"
				+ " 	ON prev.stop_heartbeat_id = prev_stop." + _ID;
		
		public static final String NextMileageView =
			"CREATE VIEW next_mileages AS"
			+ " SELECT m." + _ID + " mileage_id, next.*"
			+ " FROM mileages m"
			+ " INNER JOIN heartbeats stop"
			+ " 	ON m.stop_heartbeat_id = stop." + _ID
			+ " INNER JOIN heartbeats next_start"
			+ " 	ON stop.odometer = next_start.odometer"
			+ "			AND stop.place_id = next_start.place_id"
			+ " 		AND stop." + _ID + " != next_start." + _ID
			+ " INNER JOIN mileages next"
			+ " 	ON next.start_heartbeat_id = next_start." + _ID;
		
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
			"start_heartbeat_id",
			"stop_heartbeat_id",
			"mileage",
			"amount",
			"location_id",
			"calc_cost",
			"calc_amount"
		};
		
		public static final String[] DefaultProjection = {
			"-1 " + _ID,
			"0 mileage",
			"CURRENT_TIMESTAMP _created",
			"MAX(m.amount) amount",
			"0 location_id",
			"NULL track_id",
			"NULL start_heartbeat_id",
			"NULL stop_heartbeat_id",
			"0 calc_cost",
			"0 calc_amount",
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
		// reverse links detection
		public static final String IconColumnExpression =
				"((r._id IS NOT NULL) * 4"
				+ "	| (m1._id IS NOT NULL) * 2"
				+ "	| (m2._id IS NOT NULL))";
		
		public static final String[] ListProjection = {
			"h." + _ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"h.sync_state",
			IconColumnExpression + " icons",
			"l.color color",
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l ON h.place_id = l." + _ID
			+ " LEFT JOIN mileages m1 ON m1.start_heartbeat_id = h." + _ID
			+ " LEFT JOIN mileages m2 ON m2.stop_heartbeat_id = h." + _ID
			+ " LEFT JOIN refuels r ON r.heartbeat_id = h." + _ID;
		
		public static final String ReferenceCount =
			"SELECT SUM(cnt) FROM ("
			+ "SELECT COUNT(1) cnt FROM mileages WHERE start_heartbeat_id = ?1"
			+ " UNION "
			+ "SELECT COUNT(1) cnt FROM mileages WHERE stop_heartbeat_id = ?1"
			+ " UNION "
			+ "SELECT COUNT(1) cnt FROM refuels WHERE heartbeat_id = ?1"
			+ ")";
		
		public static final String[] RelatedItemProjection = {
			Heartbeats.IconColumnExpression + " icons",
			"COALESCE(r." + _ID + " , m1." + _ID + ", m2." + _ID + ", h." + _ID + ") edit_id"
		};
		
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
				+ " FROM refuels r"
				+ " INNER JOIN heartbeats rh"
				+ "  ON r.heartbeat_id = rh." + _ID
				+ " WHERE rh._created BETWEEN start._created AND h._created";
		
		private static final String FuelField =
				"COALESCE(r.amount, h.fuel - start.fuel - COALESCE((" + EnRouteRefuelAmount + "), 0), m2.calc_amount, h.fuel)";
		
		private static final String LegTypeField =
				"(CASE WHEN r." + _ID + " IS NULL THEN"
				+ "	CASE WHEN m1." + _ID + " IS NOT NULL THEN"
				+ "			CASE WHEN p." + _ID + " IS NOT NULL THEN 5"//RESTART
				+ "			WHEN m1.stop_heartbeat_id > 0 THEN 1"//START
				+ "			ELSE 6 END"//RUNNING
				+ "		WHEN m2." + _ID + " IS NOT NULL THEN"
				+ "			CASE WHEN n." + _ID + " IS NOT NULL THEN 3"//PAUSE
				+ "			ELSE 2 END END"//STOP
				+ "	ELSE 4 END)";//REFUEL
		
		public static final String[] ListProjection = {
			"h." + _ID + " " + _ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			"l.color",
			"m2.mileage distance",
			"ls.name destination",
			"r.cost",
			FuelField + " amount",
			"r.transaction_id",
			Heartbeats.IconColumnExpression + " icons",
			"h.sync_id",
			"h.sync_etag",
			"h.sync_date",
			"h.sync_state",
			"h.modified",
			LegTypeField + " leg"
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l	ON h.place_id = l." + _ID
			+ " LEFT JOIN mileages m1	ON m1.start_heartbeat_id = h." + _ID
			+ " LEFT JOIN mileages m2	ON m2.stop_heartbeat_id = h." + _ID
			+ " LEFT JOIN locations ls	ON m2.location_id = ls." + _ID
			+ " LEFT JOIN refuels r		ON r.heartbeat_id = h." + _ID
			+ " LEFT JOIN prev_event p	ON p.heartbeat_id = h." + _ID
			+ " LEFT JOIN next_event n	ON n.heartbeat_id = h." + _ID
			+ " LEFT JOIN heartbeats start ON start." + _ID + " = m2.start_heartbeat_id";
		
		public static final String PrevEventView =
			"CREATE VIEW prev_event AS"
			+ " SELECT h." + _ID + " heartbeat_id, prev.*"
			+ " FROM heartbeats h"
			+ " INNER JOIN heartbeats prev"
			+ " 	ON h.odometer = prev.odometer"
			+ "			AND h.place_id = prev.place_id"
			+ "			AND h._created > prev._created"
			+ "			AND SUBSTR(h._created, 1, 10) = SUBSTR(prev._created, 1, 10)"
			+ "			AND h." + _ID + " != prev." + _ID;
		
		public static final String NextEventView =
			"CREATE VIEW next_event AS"
			+ " SELECT h." + _ID + " heartbeat_id, next.*"
			+ " FROM heartbeats h"
			+ " INNER JOIN heartbeats next"
			+ " 	ON h.odometer = next.odometer"
			+ "			AND h.place_id = next.place_id"
			+ "			AND h._created < next._created"
			+ "			AND SUBSTR(h._created, 1, 10) = SUBSTR(next._created, 1, 10)"
			+ " 		AND h." + _ID + " != next." + _ID;
		
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
		public static String Create =
			"create table if not exists locations ("
			+"		_id integer primary key autoincrement," 
			+"		name text not null,	"
			+"		datetime long not null,"
			+"		provider text,"
			+"		accuracy float,"
			+"		latitude double,"
			+"		longitude double,"
			+"		is_payee integer not null default 0,"
			+"		resolved_address text,"
			+"		color TEXT,"
			+"		sync_etag TEXT,"
			+"		modified TIMESTAMP DEFAULT (strftime('%s', 'now'))"
			+"	)";
		
		public static final String Select =
			"SELECT * FROM locations";
		
		public static final String Single =
			"SELECT * FROM locations WHERE _id = ? LIMIT 1";
		
		public static final String SelectByMileages =
			"SELECT l.* FROM locations l"
			+ " INNER JOIN ("
			+ "  SELECT location_id, COUNT(" + _ID + ") cnt FROM ("
			+ "   SELECT " + _ID + ", location_id FROM mileages"
			+ "   UNION"
			+ "   SELECT m." + _ID + ", h.place_id FROM mileages m INNER JOIN heartbeats h ON m.start_heartbeat_id = h." + _ID
			+ "   UNION"
			+ "   SELECT m." + _ID + ", h.place_id FROM mileages m INNER JOIN heartbeats h ON m.stop_heartbeat_id = h." + _ID
			+ "  ) GROUP BY location_id"
			+ " ) ml ON ml.location_id = l." + _ID
			+ " ORDER BY ml.cnt DESC";
		
		public static final String SelectByRefuels =
			"SELECT l.* FROM locations l"
			+ " INNER JOIN ("
			+ "  SELECT COUNT(1) cnt, h.place_id place_id"
			+ "  FROM refuels r"
			+ "  INNER JOIN heartbeats h ON r.heartbeat_id = h." + _ID
			+ "  GROUP BY h.place_id"
			+ " ) rl ON rl.place_id = l." + _ID
			+ " ORDER BY rl.cnt DESC";
		
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.location";
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.locations";
	}
}