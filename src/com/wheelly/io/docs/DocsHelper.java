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

import com.google.android.apps.mytracks.util.StringUtils;
import android.database.Cursor;

/**
 * This class contains helper methods for interacting with Google Docs and
 * Google Spreadsheets.
 *
 * @author Sandor Dornbush
 * @author Matthew Simmons
 */
public class DocsHelper {
  /**
   * Gets the row content containing the track's info.
   */
  static String getRowContent(Cursor track, String entityId) {
    StringBuilder builder = new StringBuilder().append("<entry xmlns='http://www.w3.org/2005/Atom' "
        + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>");
    
    if(null != entityId) {
    	appendGenericTag(builder, "id", entityId);
    }
    
    final String type = iconFlagsToTypeString(track.getInt(track.getColumnIndexOrThrow("icons")));
    appendTag(builder, "type", type);
    appendTag(builder, "date", track.getString(track.getColumnIndexOrThrow("_created")));
    final String location = track.getString(track.getColumnIndexOrThrow("place"));
    if(null != location) {
    	appendTag(builder, "location", location);
    }
    appendTag(builder, "fuel", Long.toString(track.getLong(track.getColumnIndexOrThrow("fuel"))));
    appendTag(builder, "odometer", Float.toString(track.getFloat(track.getColumnIndexOrThrow("odometer"))));
    
    if("STOP".equals(type)) {
    	appendTag(builder, "distance", Float.toString(track.getFloat(track.getColumnIndex("distance"))));
    	appendTag(builder, "destination", track.getString(track.getColumnIndex("destination")));
    }
    
    if("REFUEL".equals(type)) {
    	appendTag(builder, "fuel", Float.toString(track.getFloat(track.getColumnIndex("fuel"))));
    	appendTag(builder, "amount", Float.toString(track.getFloat(track.getColumnIndex("amount"))));
    	appendTag(builder, "cost", Float.toString(track.getFloat(track.getColumnIndex("cost"))));
    	appendTag(builder, "transaction", Float.toString(track.getFloat(track.getColumnIndex("transaction_id"))));
    }
    
    builder.append("</entry>");
    return builder.toString();
  }

  /**
   * Appends a name-value pair as a gsx tag to a string builder.
   *
   * @param stringBuilder the string builder
   * @param name the name
   * @param value the value
   */
  static void appendTag(StringBuilder stringBuilder, String name, String value) {
      appendGenericTag(stringBuilder, "gsx:" + name, StringUtils.formatCData(value));
  }
  
  static void appendGenericTag(StringBuilder stringBuilder, String name, String value) {
	  stringBuilder
	  	.append("<")
	  	.append(name)
	  	.append(">")
	  	.append(value)
	  	.append("</")
	  	.append(name)
	  	.append(">");
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