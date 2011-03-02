package com.wheelly.db;

import android.provider.BaseColumns;

public final class DatabaseSchema {
	public static final class Mileages {
		public static final String Create = 
			"CREATE TABLE mileages ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "_modified	TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "name			TEXT,"
			+ "track_id		LONG,"
			
			+ "start_heartbeat_id	LONG NOT NULL,"
			+ "stop_heartbeat_id	LONG"
			
			+ "mileage		NUMERIC,"
			+ "amount		NUMERIC,"
			
			+ "calc_cost	NUMERIC,"
			+ "calc_amount	NUMERIC);";
		
		public static final String Select =
			"SELECT name, h._created start_time, mileage, calc_cost, calc_amount"
			+ " FROM mileages m"
			+ " INNER JOIN heartbeats h"
			+ " ON m.start_heartbeat_id = h." + BaseColumns._ID;
		
		public static final String Single =
			"SELECT "
			+ "name,"
			+ "track_id,"
			+ "h1._created start_time,"
			+ "h1.place_id start_place_id,"
			+ "h2._created stop_time,"
			+ "h2.place_id stop_place_id,"
			+ "mileage,"
			+ "amount,"
			+ "calc_cost,"
			+ "calc_amount"
			+ " FROM mileages m"
			+ " INNER JOIN heartbeats h1 ON m.start_heartbeat_id = h1." + BaseColumns._ID
			+ " OUTER JOIN heartbeats h2 ON m.stop_heartbeat_id = h2." + BaseColumns._ID
			+ " WHERE " + BaseColumns._ID + " = ?";
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
			+ "heartbeat_id	LONG"
			
			+ "calc_mileage	NUMERIC)";
	}
	
	public static final class Heartbeats {
		public static final String Create =
			"CREATE TABLE heartbeats ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "_created		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
			+ "odometer		NUMERIC NOT NULL,"
			+ "fuel			NUMERIC NOT NULL,"
			+ "place_id		LONG"
			+ "latitude		DOUBLE,"
			+ "longitude	DOUBLE);";
	}
}