/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wheelly.io.docs;

import com.google.gdata.data.spreadsheet.ListEntry;

import android.database.Cursor;

/**
 * This class contains helper methods for interacting with Google Docs and
 * Google Spreadsheets.
 *
 * @author Sandor Dornbush
 * @author Matthew Simmons
 */
public class DocsHelper {
  
  static void assignEntityFromCursor(Cursor track, ListEntry row, String entityId)
  {
    row.setId(entityId);
    assignEntityFromCursor(track, row);
  }
  
  static void assignEntityFromCursor(Cursor track, ListEntry row)
  {
    final String type = iconFlagsToTypeString(track.getInt(track.getColumnIndexOrThrow("icons")));
    row.getCustomElements().setValueLocal("type", type);
    row.getCustomElements().setValueLocal("date", track.getString(track.getColumnIndexOrThrow("_created")));
    final String location = track.getString(track.getColumnIndexOrThrow("place"));
    if(null != location) {
      row.getCustomElements().setValueLocal("location", location);
    }
    row.getCustomElements().setValueLocal("fuel", Long.toString(track.getLong(track.getColumnIndexOrThrow("fuel"))));
    row.getCustomElements().setValueLocal("odometer", Float.toString(track.getFloat(track.getColumnIndexOrThrow("odometer"))));
    
    if("STOP".equals(type)) {
      row.getCustomElements().setValueLocal("distance", Float.toString(track.getFloat(track.getColumnIndex("distance"))));
      row.getCustomElements().setValueLocal("destination", track.getString(track.getColumnIndex("destination")));
    }
    
    if("REFUEL".equals(type)) {
      row.getCustomElements().setValueLocal("fuel", Float.toString(track.getFloat(track.getColumnIndex("fuel"))));
      row.getCustomElements().setValueLocal("amount", Float.toString(track.getFloat(track.getColumnIndex("amount"))));
      row.getCustomElements().setValueLocal("cost", Float.toString(track.getFloat(track.getColumnIndex("cost"))));
      row.getCustomElements().setValueLocal("transaction", Float.toString(track.getFloat(track.getColumnIndex("transaction_id"))));
    }
  }
	
  public static String iconFlagsToTypeString(int flags) {
    return
      (flags & 1) != 0
        ? "STOP"
        : (flags & 2) != 0
          ? "START"
          : (flags & 4) != 0
            ? "REFUEL"
            : "";
  }
}