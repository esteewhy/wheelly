package com.wheelly.db;

import android.provider.BaseColumns;

public final class DatabaseSchema {
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
			
			+ "start_time	TIMESTAMP,"
			+ "stop_time	TIMESTAMP,"
			
			+ "mileage		NUMERIC,"
			+ "amount		NUMERIC,"
			
			+ "calc_cost	NUMERIC,"
			+ "calc_amount	NUMERIC);";
		
		public static final String Select =
			"SELECT m." + BaseColumns._ID + ", name, h._created start_time, mileage, calc_cost, calc_amount"
			+ " FROM mileages m"
			+ " LEFT OUTER JOIN heartbeats h"
			+ " ON m.start_heartbeat_id = h." + BaseColumns._ID
			+ " ORDER BY m._created";
		
		public static final String Single =
			"SELECT "
			+ "m." + BaseColumns._ID + ","
			+ "m._created, name,"
			+ "track_id,"
			+ "start_time,"
			+ "start_heartbeat_id,"
			+ "stop_time,"
			+ "stop_heartbeat_id,"
			+ "mileage,"
			+ "amount,"
			+ "calc_cost,"
			+ "calc_amount"
			+ " FROM mileages m"
			+ " WHERE m." + BaseColumns._ID + " = ?";
		
		public static final String Defaults =
			"SELECT -1 " + BaseColumns._ID
			+ ", (select max(h.odometer) from heartbeats h) mileage"
			+ ", CURRENT_TIMESTAMP start_time"
			+ ", NULL stop_time"
			+ ", CURRENT_TIMESTAMP _created"
			+ ", MAX(m.amount) amount, NULL track_id"
			+ ", NULL start_heartbeat_id, NULL stop_heartbeat_id"
			+ ", 0 calc_cost, 0 calc_amount"
			+ ", CURRENT_TIMESTAMP name FROM mileages m;";
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
			+ "place_id		INTEGER,"
			+ "transaction_id INTEGER,"
			+ "is_full		INTEGER NOT NULL DEFAULT 1,"
			+ "heartbeat_id	LONG,"
			
			+ "calc_mileage	NUMERIC)";
		
		public static final String Select =
			"SELECT f._id, f.name, f.calc_mileage, f.cost, f.amount"
			+", IFNULL(f._created, h._created) _created"
			+" FROM refuels f"
			+" LEFT OUTER JOIN heartbeats h ON f.heartbeat_id = h._id;";
		
		public static final String Defaults =
			"SELECT 1 is_full, '' name, NULL calc_mileage"
			+", (60 - h.fuel) amount"
			+", CURRENT_TIMESTAMP _created"
			+", NULL transaction_id"
			+", NULL heartbeat_id"
			+", (SELECT f.cost * (60 - h.fuel) / f.amount FROM refuels f"
			+" 		LEFT OUTER JOIN heartbeats hh"
			+"		ON hh._id = f.heartbeat_id"
			+"		ORDER BY IFNULL(hh._created, f._created) DESC LIMIT 1"
			+") cost"
			+" FROM heartbeats h"
			+" ORDER BY h._created DESC LIMIT 1";
	}
	
	public static final class Heartbeats {
		public static final String Create =
			"CREATE TABLE heartbeats ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "odometer		NUMERIC NOT NULL,"
			+ "fuel			NUMERIC NOT NULL,"
			+ "place_id		LONG,"
			+ "latitude		DOUBLE,"
			+ "longitude	DOUBLE);";
		
		public static final String Select =
			"SELECT * FROM heartbeats ORDER BY _created DESC";
		
		public static final String Defaults =
			"SELECT * FROM heartbeats ORDER BY _created DESC LIMIT 1";
		
		public static final String Single =
			"SELECT * FROM heartbeats"
			+ " WHERE " + BaseColumns._ID + " = ?";
	}
}