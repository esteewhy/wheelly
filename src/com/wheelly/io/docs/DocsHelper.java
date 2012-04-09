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
import android.content.Context;
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
   *
   * @param track the track
   * @param metricUnits true to use metric
   * @param context the context
   */
  static String getRowContent(Cursor track, boolean metricUnits, Context context) {
    StringBuilder builder = new StringBuilder().append("<entry xmlns='http://www.w3.org/2005/Atom' "
        + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>");
    
    appendTag(builder, "type", iconFlagsToTypeString(track.getInt(track.getColumnIndexOrThrow("icons"))));
    appendTag(builder, "date", track.getString(track.getColumnIndexOrThrow("_created")));
    appendTag(builder, "location", track.getString(track.getColumnIndexOrThrow("place")));
    appendTag(builder, "fuel", Long.toString(track.getLong(track.getColumnIndexOrThrow("fuel"))));
    appendTag(builder, "odometer", Float.toString(track.getFloat(track.getColumnIndexOrThrow("odometer"))));
    
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
    stringBuilder
        .append("<gsx:")
        .append(name)
        .append(">")
        .append(StringUtils.formatCData(value))
        .append("</gsx:")
        .append(name)
        .append(">");
  }
  
  private static String iconFlagsToTypeString(int flags) {
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