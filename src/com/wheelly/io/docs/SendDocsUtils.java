/*
 * Copyright 2012 Google Inc.
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Locale;

import android.content.Context;
import android.util.Log;
import com.google.android.apps.mytracks.util.ResourceUtils;

/**
 * Utilities for sending a track to Google Docs.
 *
 * @author Sandor Dornbush
 * @author Matthew Simmons
 */
public class SendDocsUtils {
  private static final String CREATE_SPREADSHEET_URI =
    "https://docs.google.com/feeds/documents/private/full";
  public static final String GET_WORKSHEET_URI =
    "https://spreadsheets.google.com/feeds/list/%s/%s/private/full";

  private static final String SPREADSHEET_ID_PREFIX =
      "https://docs.google.com/feeds/documents/private/full/spreadsheet%3A";

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String OPENDOCUMENT_SPREADSHEET_MIME_TYPE =
      "application/x-vnd.oasis.opendocument.spreadsheet";

  private static final String AUTHORIZATION = "Authorization";
  private static final String AUTHORIZATION_PREFIX = "GoogleLogin auth=";

  private static final String SLUG = "Slug";

  // Google Docs can only parse numbers in the English locale.
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);
  static {
    NUMBER_FORMAT.setMaximumFractionDigits(2);
    NUMBER_FORMAT.setMinimumFractionDigits(2);
  }

  private static final String TAG = SendDocsUtils.class.getSimpleName();

  private SendDocsUtils() {}

  /**
   * Creates a new spreadsheet with the given title. Returns the spreadsheet ID
   * if successful. Returns null otherwise. Note that it is possible that a new
   * spreadsheet is created, but the returned ID is null.
   *
   * @param title the title
   * @param authToken the auth token
   * @param context the context
   */
  public static String createSpreadsheet(String title, String authToken, Context context)
      throws IOException {
	  return createSpreadsheet(title, authToken, context, com.wheelly.R.raw.wheelly_empty_spreadsheet);
  }
  
  public static String createSpreadsheet(String title, String authToken, Context context, int spreadsheetResourceId)
      throws IOException {
  
  URL url = new URL(CREATE_SPREADSHEET_URI);
    URLConnection conn = url.openConnection();
    conn.addRequestProperty(CONTENT_TYPE, OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
    conn.addRequestProperty(SLUG, title);
    conn.addRequestProperty(AUTHORIZATION, AUTHORIZATION_PREFIX + authToken);
    conn.setDoOutput(true);
    OutputStream outputStream = conn.getOutputStream();
    ResourceUtils.readBinaryFileToOutputStream(
        context, spreadsheetResourceId, outputStream);

    // Get the response
    BufferedReader bufferedReader = null;
    StringBuilder resultBuilder = new StringBuilder();
    try {
      bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        resultBuilder.append(line);
      }
    } catch (FileNotFoundException e) {
      // The GData API sometimes throws an error, even though creation of
      // the document succeeded. In that case let's just return. The caller
      // then needs to check if the doc actually exists.
      Log.d(TAG, "Unable to read result after creating a spreadsheet", e);
      return null;
    } finally {
      outputStream.close();
      if (bufferedReader != null) {
        bufferedReader.close();
      }
    }
    return getNewSpreadsheetId(resultBuilder.toString());
  }

  /**
   * Gets the spreadsheet id from a create spreadsheet result.
   *
   * @param result the create spreadsheet result
   */
  static String getNewSpreadsheetId(String result) {
    int idTagIndex = result.indexOf("<id>");
    if (idTagIndex == -1) {
      return null;
    }

    int idTagCloseIndex = result.indexOf("</id>", idTagIndex);
    if (idTagCloseIndex == -1) {
      return null;
    }

    int idStringStart = result.indexOf(SPREADSHEET_ID_PREFIX, idTagIndex);
    if (idStringStart == -1) {
      return null;
    }

    return result.substring(idStringStart + SPREADSHEET_ID_PREFIX.length(), idTagCloseIndex);
  }
}