package com.wheelly.db;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Unified contract for persisting entities of a given type.
 */
public interface IRepository {
	Cursor list();
	
	long insert(ContentValues values);
	
	void update(ContentValues values);
	
	ContentValues load(long id);
	
	void delete(long id);
	
	/**
	 * Calculate default entity values.
	 * @return
	 */
	ContentValues getDefaults();
	
	/**
	 * Attempts to match an existing record against provided values.
	 * 
	 * This is primarily used to reduce duplications and is specific to entity.
	 * @param values
	 * @return Matched record ID or 0.
	 */
	long exists(ContentValues values);
}