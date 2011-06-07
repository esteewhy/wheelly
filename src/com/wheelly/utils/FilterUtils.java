package com.wheelly.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.content.ContentValues;
import android.text.TextUtils;

import com.wheelly.activity.Filter.F;
import com.wheelly.db.DatabaseSchema;

public class FilterUtils {
	/**
	 * Helps to construct SQL query from a given WHERE/ORDER values.
	 * 
	 * @param filter Map of filter values.
	 * @param sql Accumulator of SQL string.
	 * @param filterExpr Map of SQL expressions, corresponding to values.
	 * @return Array of string values suitable for passing as a second parameter to SQLiteDatabase.rawQuery().
	 */
	public static String[] updateSqlFromFilter(
			ContentValues filter,
			StringBuilder sql,
			Map<String, String> filterExpr) {
		ArrayList<String> conditions = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		
		if(filter.containsKey(F.LOCATION)) {
			values.add(Long.toString(filter.getAsLong(F.LOCATION)));
			conditions.add(filterExpr.get(F.LOCATION)
				.replace("@location_id", "?" + values.size()));
		}
		
		if(filter.containsKey(F.PERIOD)) {
			String[] parts = filter.getAsString(F.PERIOD).split(",");
			SimpleDateFormat date = new SimpleDateFormat(DatabaseSchema.DateTimeFormat); 
			conditions.add(filterExpr.get(F.PERIOD)
				.replace("@from",
					values.add(date.format(new Date(Long.valueOf(parts[1]))))
					? "?" + values.size() : "clever?")
				.replace("@to",
					values.add(date.format(new Date(Long.valueOf(parts[2]))))
					? "?" + values.size() : "hell yeah!")
			);
		}
		
		if(conditions.size() > 0) {
			sql.append(" WHERE ("
				.concat(TextUtils.join(") AND (", conditions.toArray()))
				.concat(") "));
		}
		
		sql.append(filterExpr.get(F.SORT_ORDER)
			+ (filter.containsKey(F.SORT_ORDER) && filter.getAsInteger(F.SORT_ORDER) != 0
			? " ASC" : " DESC"));
		
		return values.toArray(new String[0]);
	}
}