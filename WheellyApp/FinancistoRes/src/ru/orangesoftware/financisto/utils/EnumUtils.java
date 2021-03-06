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
import android.widget.ArrayAdapter;

public abstract class EnumUtils {

	public static String[] getLocalizedValues(Context context, LocalizableEnum...values) {
		int count = values.length;
		String[] items = new String[count];
		for (int i = 0; i<count; i++) {
			LocalizableEnum r = values[i];
			items[i] = context.getString(r.getTitleId());
		}
		return items;
	}
	
	public static ArrayAdapter<String> createDropDownAdapter(Context context, LocalizableEnum...values) {
		String[] items = getLocalizedValues(context, values);
		return new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items);		
	}

	public static ArrayAdapter<String> createSpinnerAdapter(Context context, LocalizableEnum...values) {
		String[] items = getLocalizedValues(context, values);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}

    public static String[] asStringArray(Enum... values) {
        int count = values.length;
        String[] a = new String[count];
        for (int i=0; i<count; i++) {
            a[i] = values[i].name();
        }
        return a;
    }

}
