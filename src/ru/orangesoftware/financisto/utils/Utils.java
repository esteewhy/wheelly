/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.utils;

import android.content.Context;
import android.database.Cursor;
import java.math.BigDecimal;

public class Utils {
	
	public static final BigDecimal HUNDRED = new BigDecimal(100);
	
	public Utils(Context context) {
	}
		
	public static int moveCursor(Cursor cursor, String idColumnName, long id) {
		if (id != -1) {			
			int pos = cursor.getColumnIndexOrThrow(idColumnName);
			if (cursor.moveToFirst()) {
				do {
					if (cursor.getLong(pos) == id) {
						return cursor.getPosition();
					}
				} while(cursor.moveToNext());
			}
		}
		return -1;
	}
	
	public static boolean isNotEmpty(String s) {
		return s != null && s.length() > 0;
	}
}
