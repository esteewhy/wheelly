package com.wheelly.db;

import java.util.HashMap;
import java.util.Map;

import com.wheelly.activity.Filter.F;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class DatabaseSchema {
	public static final String CONTENT_AUTHORITY = "com.wheelly";
	private static final Uri BASE_CONTENT_URI =
		Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_MILEAGES = "mileages";
	private static final String PATH_REFUELS = "refuels";
	private static final String PATH_HEARTBEATS = "refuels";
	
	public static final String DateFormat = "yyyy-MM-dd";
	public static final String TimeFormat = "HH:mm:ss";
	public static final String DateTimeFormat = DateFormat + " " + TimeFormat;
	
	public static final class Mileages {
		public static final String Create = 
			"CREATE TABLE mileages ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "_modified	TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "name			TEXT,"
			+ "track_id		LONG,"
			
			+ "start_heartbeat_id	LONG,"
			+ "stop_heartbeat_id	LONG,"
			
			+ "location_id	LONG,"
			
			+ "mileage		NUMERIC,"
			+ "amount		NUMERIC,"
			//TODO Calculated fields.
			+ "calc_cost	NUMERIC,"
			+ "calc_amount	NUMERIC"
			/// Constraints arent's supported prior 2.2
			//+ ",FOREIGN KEY start_heartbeat_id REFERENCES heartbeats(_id)"
			//+ ",FOREIGN KEY stop_heartbeat_id REFERENCES heartbeats(_id)"
			+ ");";
		
		//Calculates amount of refuels occurred in progress of a given trip.
		private static final String EnRouteRefuelAmount =
			"SELECT Sum(r.amount)"
			+ " FROM refuels r"
			+ " INNER JOIN heartbeats rh"
			+ "  ON r.heartbeat_id = rh." + BaseColumns._ID
			+ " WHERE rh._created BETWEEN start._created AND stop._created";
		
		public static final String[] Columns = {
			"m." + BaseColumns._ID,
			"COALESCE(stop._created, start._created, m._created) stop_time",
			"mileage",
			"calc_cost cost",
			//TODO Calculate in a scheduled async. job
			"COALESCE(stop.fuel - start.fuel - COALESCE((" + EnRouteRefuelAmount + "), 0), calc_amount) fuel",
			"start_place.name start_place",
			"stop_place.name stop_place",
			"dest.name destination"
		};
		
		public static final String Tables = "mileages m"
			+ " LEFT OUTER JOIN heartbeats start"
			+ " 	ON m.start_heartbeat_id = start." + BaseColumns._ID
			+ " LEFT OUTER JOIN locations start_place"
			+ "		ON start.place_id = start_place." + BaseColumns._ID
			+ " LEFT OUTER JOIN heartbeats stop"
			+ " 	ON m.stop_heartbeat_id = stop." + BaseColumns._ID
			+ " LEFT OUTER JOIN locations stop_place"
			+ "		ON stop.place_id = stop_place." + BaseColumns._ID
			+ " LEFT OUTER JOIN locations dest"
			+ "		ON m.location_id = dest." + BaseColumns._ID;
		
		public static final String Single =
			"SELECT "
			+ "m." + BaseColumns._ID
			+ ", m._created"
			+ ", name"
			+ ", track_id"
			+ ", start_heartbeat_id"
			+ ", stop_heartbeat_id"
			+ ", mileage"
			+ ", amount"
			+ ", location_id"
			+ ", calc_cost"
			+ ", calc_amount"
			+ " FROM mileages m"
			+ " WHERE m." + BaseColumns._ID + " = ?";
		
		public static final String Defaults =
			"SELECT -1 " + BaseColumns._ID
			+ ", 0					mileage"
			+ ", CURRENT_TIMESTAMP	_created"
			+ ", MAX(m.amount)		amount"
			+ ", 0					location_id"
			+ ", NULL				track_id"
			+ ", NULL				start_heartbeat_id"
			+ ", NULL				stop_heartbeat_id"
			+ ", 0					calc_cost"
			+ ", 0					calc_amount"
			+ ", CURRENT_TIMESTAMP	name"
			+ " FROM mileages m;";
		
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
	}
	
	public static final class Refuels {
		public static final String Create =
			"CREATE TABLE refuels ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
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
			
			+ "calc_mileage	NUMERIC"
			
			//+ ",FOREIGN KEY(heartbeat_id) REFERENCES heartbeats(_id)"
			+ ")";
		
		private static final String SelectPreviousHeartbeat =
			"SELECT h.odometer - hh.odometer"
			+ " FROM heartbeats hh"
			+ " INNER JOIN refuels ff ON ff.heartbeat_id = hh." + BaseColumns._ID
			+ " WHERE hh._created < h._created"
			+ " ORDER BY hh._created DESC"
			+ " LIMIT 1";
		
		public static final String[] Columns = {
			"f." + BaseColumns._ID,
			"f.name",
			//"f.calc_mileage mileage",
			"(" + SelectPreviousHeartbeat + ") mileage",
			"f.cost",
			"f.amount",
			"IFNULL(h._created, f._created) _created",
			"l.name place",
		};
		
		public static final String Tables = "refuels f"
			+ " LEFT OUTER JOIN heartbeats h"
			+ "		ON f.heartbeat_id = h." + BaseColumns._ID
			+ " LEFT OUTER JOIN locations l"
			+ "		ON h.place_id = l." + BaseColumns._ID;
		
		public static final String Single =
			"SELECT " + BaseColumns._ID + ", name, calc_mileage, cost, amount"
			+", unit_price"
			+", _created"
			+", transaction_id"
			+", heartbeat_id"
			+" FROM refuels"
			+" WHERE " + BaseColumns._ID + " = ?"
			+" LIMIT 1";
		
		public static final String Defaults =
			"SELECT 0				_id"
			+", 1					is_full"
			+", ''					name"
			+", NULL				calc_mileage"
			+", (?1- h.fuel)		amount"
			+", NULL				unit_price"
			+", CURRENT_TIMESTAMP	_created"
			+", NULL				transaction_id"
			+", NULL				heartbeat_id"
			+", (SELECT f.cost * (?1 - h.fuel) / f.amount FROM refuels f"
			+" 		LEFT OUTER JOIN heartbeats hh"
			+"		ON hh._id = f.heartbeat_id"
			+"		ORDER BY IFNULL(hh._created, f._created) DESC LIMIT 1"
			+") cost"
			+" FROM heartbeats h"
			+" ORDER BY h._created DESC LIMIT 1";
		
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
		public static final String Create =
			"CREATE TABLE heartbeats ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "odometer		NUMERIC NOT NULL,"
			+ "fuel			NUMERIC NOT NULL,"
			+ "place_id		LONG);";
		
		// reverse links detection
		private static final String IconColumn =
			"EXISTS(SELECT 1 FROM refuels WHERE heartbeat_id = h._id) * 4"
			+ "	| EXISTS(SELECT 1 FROM mileages WHERE start_heartbeat_id = h._id) * 2"
			+ "	| EXISTS(SELECT 1 FROM mileages WHERE stop_heartbeat_id = h._id) icons";
		
		public static final String[] Columns = {
			"h." + BaseColumns._ID,
			"h._created",
			"h.odometer",
			"h.fuel",
			"l.name place",
			IconColumn
		};
		
		public static final String Tables = "heartbeats h"
			+ " LEFT JOIN locations l ON h.place_id = l." + BaseColumns._ID;
		
		public static final String Defaults =
			"SELECT * FROM heartbeats ORDER BY _created DESC LIMIT 1";
		
		public static final String Single =
			"SELECT * FROM heartbeats"
			+ " WHERE " + BaseColumns._ID + " = ?";
		
		public static final String Exists =
			"SELECT "
			+ BaseColumns._ID
			+ " FROM heartbeats"
			+ " WHERE odometer = ? AND fuel = ?"
			+ " LIMIT 1;";
		
		public static final String ReferenceCount =
			"SELECT SUM(cnt) FROM ("
			+ "SELECT COUNT(1) cnt FROM mileages WHERE start_heartbeat_id = ?1"
			+ " UNION "
			+ "SELECT COUNT(1) cnt FROM mileages WHERE stop_heartbeat_id = ?1"
			+ " UNION "
			+ "SELECT COUNT(1) cnt FROM refuels WHERE heartbeat_id = ?1"
			+ ");";
		
		public static final Map<String, String> FilterExpr = new HashMap<String, String>();
		static {
			FilterExpr.put(F.LOCATION, "h.place_id = @location_id");
			FilterExpr.put(F.PERIOD, "h._created BETWEEN @from AND @to");
			FilterExpr.put(F.SORT_ORDER, "h._created");
		}
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(PATH_HEARTBEATS).build();
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/com.wheelly.heartbeat";
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.wheelly.heartbeats";
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
			+"		resolved_address text"
			+"	);";
		
		public static String Select =
			"SELECT * FROM locations;";
		
		public static String Single =
			"SELECT * FROM locations WHERE _id = ? LIMIT 1;";
		
		public static String SelectByMileages =
			"SELECT l.* FROM locations l"
			+ " INNER JOIN ("
			+ "  SELECT location_id, COUNT(" + BaseColumns._ID + ") cnt FROM ("
			+ "   SELECT " + BaseColumns._ID + ", location_id FROM mileages"
			+ "   UNION"
			+ "   SELECT m." + BaseColumns._ID + ", h.place_id FROM mileages m INNER JOIN heartbeats h ON m.start_heartbeat_id = h." + BaseColumns._ID
			+ "   UNION"
			+ "   SELECT m." + BaseColumns._ID + ", h.place_id FROM mileages m INNER JOIN heartbeats h ON m.stop_heartbeat_id = h." + BaseColumns._ID
			+ "  ) GROUP BY location_id"
			+ " ) ml ON ml.location_id = l." + BaseColumns._ID
			+ " ORDER BY ml.cnt DESC";
	}
}