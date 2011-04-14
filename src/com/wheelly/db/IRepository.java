package com.wheelly.db;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Unified contract for persisting entities of a given type.
 */
public interface IRepository {
	public Cursor list();
	
	public long insert(ContentValues values);
	
	public void update(ContentValues values);
	
	public ContentValues load(long id);
	
	public ContentValues getDefaults();
}