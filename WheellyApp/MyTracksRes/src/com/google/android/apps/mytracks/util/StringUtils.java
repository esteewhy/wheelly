/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Various string manipulation methods.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils {

  private static final SimpleDateFormat ISO_8601_DATE_TIME_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
  private static final SimpleDateFormat ISO_8601_BASE = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
  private static final Pattern ISO_8601_EXTRAS = Pattern.compile(
      "^(\\.\\d+)?(?:Z|([+-])(\\d{2}):(\\d{2}))?$");
  static {
    ISO_8601_DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    ISO_8601_BASE.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private StringUtils() {}

  /**
   * Formats the date and time based on user's phone date/time preferences.
   * 
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatDateTime(Context context, long time) {
    return DateUtils.formatDateTime(
        context, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE) + " "
        + DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME).toString();
  }

  /**
   * Formats the time using the ISO 8601 date time format with fractional
   * seconds in UTC time zone.
   * 
   * @param time the time in milliseconds
   */
  public static String formatDateTimeIso8601(long time) {
    return ISO_8601_DATE_TIME_FORMAT.format(time);
  }

  /**
   * Formats the elapsed timed in the form "MM:SS" or "H:MM:SS".
   * 
   * @param time the time in milliseconds
   */
  public static String formatElapsedTime(long time) {
    /*
     * Temporary workaround for DateUtils.formatElapsedTime(time / 1000). In API
     * level 17, it returns strings like "1:0:00" instead of "1:00:00", which
     * breaks several unit tests.
     */
    if (time < 0) {
      return "-";
    }
    long hours = 0;
    long minutes = 0;
    long seconds = 0;
    long elapsedSeconds = time / 1000;

    if (elapsedSeconds >= 3600) {
      hours = elapsedSeconds / 3600;
      elapsedSeconds -= hours * 3600;
    }
    if (elapsedSeconds >= 60) {
      minutes = elapsedSeconds / 60;
      elapsedSeconds -= minutes * 60;
    }
    seconds = elapsedSeconds;

    if (hours > 0) {
      return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
  }

  /**
   * Formats the elapsed time in the form "H:MM:SS".
   * 
   * @param time the time in milliseconds
   */
  public static String formatElapsedTimeWithHour(long time) {
    String value = formatElapsedTime(time);
    return TextUtils.split(value, ":").length == 2 ? "0:" + value : value;
  }

  /**
   * Formats the distance.
   * 
   * @param context the context
   * @param distance the distance in meters
   * @param metricUnits true to use metric units. False to use imperial units
   */
  public static String formatDistance(Context context, double distance, boolean metricUnits) {
    if (Double.isNaN(distance) || Double.isInfinite(distance)) {
      return context.getString(R.string.value_unknown);
    }
    if (metricUnits) {
      if (distance > 500.0) {
        distance *= UnitConversions.M_TO_KM;
        return context.getString(R.string.value_float_kilometer, distance);
      } else {
        return context.getString(R.string.value_float_meter, distance);
      }
    } else {
      if (distance * UnitConversions.M_TO_MI > 0.5) {
        distance *= UnitConversions.M_TO_MI;
        return context.getString(R.string.value_float_mile, distance);
      } else {
        distance *= UnitConversions.M_TO_FT;
        return context.getString(R.string.value_float_feet, distance);
      }
    }
  }

  /**
   * Formats the given text as a XML CDATA element. This includes adding the
   * starting and ending CDATA tags. Please notice that this may result in
   * multiple consecutive CDATA tags.
   * 
   * @param text the given text
   */
  public static String formatCData(String text) {
    return "<![CDATA[" + text.replaceAll("]]>", "]]]]><![CDATA[>") + "]]>";
  }

  /**
   * Gets the html.
   * 
   * @param context the context
   * @param resId the string resource id
   * @param formatArgs the string resource ids of the format arguments
   */
  public static Spanned getHtml(Context context, int resId, Object... formatArgs) {
    Object[] args = new Object[formatArgs.length];
    for (int i = 0; i < formatArgs.length; i++) {
      String url = context.getString((Integer) formatArgs[i]);
      args[i] = " <a href='" + url + "'>" + url + "</a> ";
    }
    return Html.fromHtml(context.getString(resId, args));
  }
}