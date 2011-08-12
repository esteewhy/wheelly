package com.wheelly.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;

public class FilterUtils {
	
	public static class FilterResult {
		public String Where;
		public String[] Values;
		public String Order;
		
		FilterResult() {}
	}
	
	public static class F {
		public final static String SORT_ORDER	= "sort_order";
		public final static String PERIOD		= "period";
		public final static String LOCATION		= "location_id";
		public final static String LOCATION_CONSTRAINT = "location_id_constraint";
	}
	
	/**
	 * Helps to construct SQL query from a given WHERE/ORDER values.
	 * 
	 * @param filter Map of filter values.
	 * @param filterExpr Map of SQL expressions, corresponding to values.
	 * @return Array of string values suitable for passing as a second parameter to SQLiteDatabase.rawQuery().
	 */
	public static FilterResult updateSqlFromFilter(
			ContentValues filter,
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
			conditions.add(filterExpr.get(F.PERIOD)
				.replace("@from",
					values.add(DateUtils.dbFormat.format(new Date(Long.valueOf(parts[1]))))
					? "?" + values.size() : "clever?")
				.replace("@to",
					values.add(DateUtils.dbFormat.format(new Date(Long.valueOf(parts[2]))))
					? "?" + values.size() : "hell yeah!")
			);
		}
		
		final FilterResult r = new FilterResult();
		r.Where = conditions.size() > 0
			? "("
				.concat(TextUtils.join(") AND (", conditions.toArray()))
				.concat(") ")
			: null;
		r.Order =
			filterExpr.get(F.SORT_ORDER)
			+ (filter.containsKey(F.SORT_ORDER) && filter.getAsInteger(F.SORT_ORDER) != 0
				? " ASC" : " DESC");
		r.Values = values.toArray(new String[0]);
		return r;
	}
	
	public static void intentToFilter(Intent intent, ContentValues filter) {
		if(intent.hasExtra(F.PERIOD)) {
			filter.put(F.PERIOD, intent.getStringExtra(F.PERIOD));
		} else {
			filter.remove(F.PERIOD);
		}
		
		if(intent.hasExtra(F.LOCATION)) {
			filter.put(F.LOCATION, intent.getLongExtra(F.LOCATION, -1));
		} else {
			filter.remove(F.LOCATION);
		}
		
		if(intent.hasExtra(F.SORT_ORDER)) {
			filter.put(F.SORT_ORDER, intent.getIntExtra(F.SORT_ORDER, 0));
		} else {
			filter.remove(F.SORT_ORDER);
		}
		
		if(intent.hasExtra(F.LOCATION_CONSTRAINT)) {
			filter.put(F.LOCATION_CONSTRAINT, intent.getStringExtra(F.LOCATION_CONSTRAINT));
		} else {
			filter.remove(F.LOCATION_CONSTRAINT);
		}
	}
	
	public static void filterToIntent(ContentValues filter, Intent intent) {
		if(filter.containsKey(F.PERIOD)) {
			intent.putExtra(F.PERIOD, filter.getAsString(F.PERIOD));
		} else {
			intent.removeExtra(F.PERIOD);
		}
		
		if(filter.containsKey(F.LOCATION)) {
			intent.putExtra(F.LOCATION, filter.getAsLong(F.LOCATION));
		} else {
			intent.removeExtra(F.LOCATION);
		}
		
		if(filter.containsKey(F.SORT_ORDER) && filter.getAsInteger(F.SORT_ORDER) > 0) {
			intent.putExtra(F.SORT_ORDER, 1);
		} else {
			intent.removeExtra(F.SORT_ORDER);
		}
		
		if(filter.containsKey(F.LOCATION_CONSTRAINT)) {
			intent.putExtra(F.LOCATION_CONSTRAINT, filter.getAsString(F.LOCATION_CONSTRAINT));
		} else {
			intent.removeExtra(F.LOCATION_CONSTRAINT);
		}
	}
}