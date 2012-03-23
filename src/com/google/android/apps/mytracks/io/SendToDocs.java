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
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.io.docs.DocsHelper;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.wheelly.R;
import com.wheelly.db.DatabaseSchema.Timeline;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.docs.DocumentsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.docs.XmlDocsGDataParserFactory;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import java.io.IOException;

/**
 * A helper class used to transmit tracks statistics to Google Docs/Trix.
 *
 * @author Sandor Dornbush
 */
public class SendToDocs {
  /** The GData service name for Google Spreadsheets (aka Trix) */
  public static final String GDATA_SERVICE_NAME_TRIX = "wise";

  /** The GData service name for the Google Docs Document List */
  public static final String GDATA_SERVICE_NAME_DOCLIST = "writely";

  private final Activity activity;
  private final AuthManager trixAuth;
  private final AuthManager docListAuth;
  private final ProgressIndicator progressIndicator;
  private final boolean metricUnits;

  private boolean success = true;
  private Runnable onCompletion = null;


  public SendToDocs(Activity activity, AuthManager trixAuth,
      AuthManager docListAuth, ProgressIndicator progressIndicator) {
    this.activity = activity;
    this.trixAuth = trixAuth;
    this.docListAuth = docListAuth;
    this.progressIndicator = progressIndicator;

    SharedPreferences preferences = activity.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (preferences != null) {
      metricUnits =
          preferences.getBoolean(activity.getString(R.string.metric_units_key),
              true);
    } else {
      metricUnits = true;
    }
  }

  public void sendToDocs(final long trackId) {
    Log.d(Constants.TAG,
        "Sending to Google Docs: trackId = " + trackId);
    new Thread("SendToGoogleDocs") {
      @Override
      public void run() {
        doUpload(trackId);
      }
    }.start();
  }

  private void doUpload(long trackId) {
    success = false;
    try {
      final ContentResolver cr = activity.getContentResolver();
      // Get the track from the provider:
      final Cursor track =
        trackId > 0
          ? cr.query(Timeline.CONTENT_URI, Timeline.ListProjection, "h._id = ?",
              new String[] { Long.toString(trackId) }, null)
          : cr.query(Timeline.CONTENT_URI, Timeline.ListProjection, null, null,
              "odometer ASC");
      
      try {
        if(track.moveToFirst()) {
          // Transmit track stats via GData feed:
          // -------------------------------
          
          Log.d(Constants.TAG, "SendToDocs: Uploading to spreadsheet");
          success = uploadToDocs(track);
          Log.d(Constants.TAG, "SendToDocs: Done.");
        }
        return;
      } finally {
    	  track.close();
      }
    } finally {
      if (onCompletion != null) {
        activity.runOnUiThread(onCompletion);
      }
    }
  }

  public boolean wasSuccess() {
    return success;
  }

  public void setOnCompletion(Runnable onCompletion) {
    this.onCompletion = onCompletion;
  }

  /**
   * Uploads the statistics about a track to Google Docs using the docs GData
   * feed.
   *
   * @param track the track
   */
  private boolean uploadToDocs(Cursor track) {
    DocsHelper docsHelper = new DocsHelper();

    GDataClient androidClient = null;
    try {
      androidClient = GDataClientFactory.getGDataClient(activity);
      
      Log.d(Constants.TAG,
          "GData connection prepared: " + this.docListAuth);
      String sheetTitle = "Wheelly";
      
      // First try to find the spreadsheet:
      String spreadsheetId = getSpreadsheetId(docsHelper, androidClient, sheetTitle);
      
      if(null == spreadsheetId) {
        return false;
      }
      
      String worksheetId = getWorksheetId(docsHelper, androidClient, spreadsheetId);
      
      if(null == worksheetId) {
        return false;
      }
      
      progressIndicator.setProgressValue(90);
      
      do {
        docsHelper.addTrackRow(activity, trixAuth, spreadsheetId, worksheetId,
            track, metricUnits);
      } while(track.moveToNext());
      Log.i(Constants.TAG, "Done uploading to docs.");
    } catch (IOException e) {
      Log.e(Constants.TAG, "Unable to upload docs.", e);
      return false;
    } finally {
      if (androidClient != null) {
        androidClient.close();
      }
    }
    return true;
  }
  
  private String getWorksheetId(
      DocsHelper docsHelper,
      GDataClient androidClient,
      String spreadsheetId) {
    SpreadsheetsClient gdataClient = new SpreadsheetsClient(androidClient,
              new XmlDocsGDataParserFactory(new AndroidXmlParserFactory()));
    
    GDataWrapper<GDataServiceClient> trixWrapper = new GDataWrapper<GDataServiceClient>();
    trixWrapper.setAuthManager(trixAuth);
    trixWrapper.setRetryOnAuthFailure(true);
    trixWrapper.setClient(gdataClient);
    
    String worksheetId = null;
    try {
      worksheetId = docsHelper.getWorksheetId(trixWrapper, spreadsheetId);
      if (worksheetId == null) {
        throw new IOException("Worksheet ID lookup returned empty");
      }
    } catch (IOException e) {
      Log.i(Constants.TAG, "Looking up worksheet id failed.", e);
      return null;
    }
    return worksheetId;
  }
  
  private String getSpreadsheetId(
      DocsHelper docsHelper,
      GDataClient androidClient,
      String sheetTitle) {
    DocumentsClient docsGdataClient = new DocumentsClient(androidClient,
            new XmlDocsGDataParserFactory(new AndroidXmlParserFactory()));
    
    GDataWrapper<GDataServiceClient> docListWrapper = new GDataWrapper<GDataServiceClient>();
    docListWrapper.setAuthManager(docListAuth);
    docListWrapper.setRetryOnAuthFailure(true);
    docListWrapper.setClient(docsGdataClient);

    // First try to find the spreadsheet:
    String spreadsheetId = null;
    try {
      spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper,
          sheetTitle);
    } catch (IOException e) {
      Log.i(Constants.TAG, "Spreadsheet lookup failed.", e);
      return null;
    }

    if (spreadsheetId == null) {
      progressIndicator.setProgressValue(65);
      // Waiting a few seconds and trying again. Maybe the server just had a
      // hickup (unfortunately that happens quite a lot...).
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        Log.e(Constants.TAG, "Sleep interrupted", e);
      }
      
      try {
        spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper,
            sheetTitle);
      } catch (IOException e) {
        Log.i(Constants.TAG, "2nd spreadsheet lookup failed.", e);
        return null;
      }
    }
    
    // We were unable to find an existing spreadsheet, so create a new one.
    progressIndicator.setProgressValue(70);
    if (spreadsheetId == null) {
      Log.i(Constants.TAG, "Creating new spreadsheet: " + sheetTitle);
      
      try {
        spreadsheetId = docsHelper.createSpreadsheet(activity, docListWrapper,
            sheetTitle);
      } catch (IOException e) {
        Log.i(Constants.TAG, "Failed to create new spreadsheet "
            + sheetTitle, e);
        return null;
      }
      progressIndicator.setProgressValue(80);
      
      if (spreadsheetId == null) {
        progressIndicator.setProgressValue(85);
        // The previous creation might have succeeded even though GData
        // reported an error. Seems to be a know bug,
        // see http://code.google.com/p/gdata-issues/issues/detail?id=929
        // Try to find the created spreadsheet:
        Log.w(Constants.TAG,
            "Create might have failed. Trying to find created document.");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          Log.e(Constants.TAG, "Sleep interrupted", e);
        }
        
        try {
          spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper,
              sheetTitle);
        } catch (IOException e) {
          Log.i(Constants.TAG, "Failed create-failed lookup", e);
          return null;
        }
        
        if (spreadsheetId == null) {
          progressIndicator.setProgressValue(87);
          // Re-try
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            Log.e(Constants.TAG, "Sleep interrupted", e);
          }
          
          try {
            spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper,
                sheetTitle);
          } catch (IOException e) {
            Log.i(Constants.TAG, "Failed create-failed relookup", e);
            return null;
          }
        }
        if (spreadsheetId == null) {
          Log.i(Constants.TAG,
              "Creating new spreadsheet really failed.");
          return null;
        }
      }
    }
    return spreadsheetId;
  }
}