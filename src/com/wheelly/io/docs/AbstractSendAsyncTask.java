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

import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;
import com.wheelly.R;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * AsyncTask to send a track to Google Docs.
 *
 * @author Jimmy Shih
 */
public abstract class AbstractSendAsyncTask<EntityType> extends ReportingSendAsyncTask {
  private static final String GOOGLE_SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
  private static final String OPENDOCUMENT_SPREADSHEET_MIME_TYPE = "application/x-vnd.oasis.opendocument.spreadsheet";
  
  @VisibleForTesting
  public static final String GET_SPREADSHEET_QUERY =
    "'root' in parents and title = '%s' and mimeType = '"
    + GOOGLE_SPREADSHEET_MIME_TYPE
    + "' and trashed = false";
  
  @VisibleForTesting
  public static final String GET_WORKSHEETS_URI =
    "https://spreadsheets.google.com/feeds/worksheets/%s/private/full";
  
  private static final int PROGRESS_GET_SPREADSHEET_ID = 0;
  private static final int PROGRESS_GET_WORKSHEET_URL = 35;
  protected static final int PROGRESS_ADD_TRACK_INFO = 70;
  protected static final int PROGRESS_COMPLETE = 100;

  private static final String TAG = AbstractSendAsyncTask.class.getSimpleName();

  protected final long trackId;
  private final Account account;
  protected final Context context;

  public AbstractSendAsyncTask(Context context, long trackId, Account account) {
    super(context);
    this.trackId = trackId;
    this.account = account;
    this.context = context;
  }

  @Override
  protected void closeConnection() { }

  @Override
  protected void saveResult() { }

  @Override
  protected boolean performTask() {
    try {
      SpreadsheetService spreadsheetService = new SpreadsheetService("Wheelly");
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
      
      credential.setAccessToken(SendToGoogleUtils.getToken(
          context,
          account.name,
          SendToGoogleUtils.SPREADSHEET_SCOPE));
      
      spreadsheetService.setOAuth2Credentials(credential);
      
      EntityType track = resolveEntity(trackId);
      if (track == null) {
        Log.d(TAG, "No track for " + trackId);
        return false;
      }
      
      String title = buildWorksheetTitle(track);
      
      publishProgress(PROGRESS_GET_SPREADSHEET_ID);
      String spreadsheetId = getSpreadSheetId(title);
      
      if (spreadsheetId == null) {
        Log.d(TAG, "Unable to get the spreadsheet ID for " + title);
        return false;
      }
      
      publishProgress(PROGRESS_GET_WORKSHEET_URL);
      
      URL worksheetUrl = getWorksheetUrl(spreadsheetService, spreadsheetId);
      if (worksheetUrl == null) {
        Log.d(TAG, "Unable to get the worksheet url for " + spreadsheetId);
        return false;
      }
      
      publishProgress(PROGRESS_ADD_TRACK_INFO);
      if (!addTrackInfo(spreadsheetService, worksheetUrl, track)) {
        Log.d(TAG, "Unable to add track info");
        return false;
      }
      
      publishProgress(PROGRESS_COMPLETE);
      return true;
    } catch (UserRecoverableAuthException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEET_NOTIFICATION_ID);
      return false;
    } catch (GoogleAuthException e) {
      Log.e(TAG, "GoogleaAuthException", e);
      return retryTask();
    } catch (UserRecoverableAuthIOException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEET_NOTIFICATION_ID);
      return false;
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
      return retryTask();
    } catch (ServiceException e) {
      Log.e(TAG, "ServiceException", e);
      return retryTask();
    }
  }
  
  @Override
  protected void invalidateToken() { }

  /**
   * Gets the spreadsheet id.
   * 
   * @param fileName the file name
 * @throws GoogleAuthException 
   */
  private String getSpreadSheetId(String fileName)
      throws IOException, GoogleAuthException {
    if (isCancelled()) {
      return null;
    }
    
    GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
            context, account.name, SendToGoogleUtils.DRIVE_SCOPE);
    
    if (driveCredential == null) {
      return null;
    }
    
    Drive drive = SyncUtils.getDriveService(driveCredential);
    com.google.api.services.drive.Drive.Files.List list = drive.files()
            .list().setQ(String.format(Locale.US, GET_SPREADSHEET_QUERY, fileName));
    
    FileList result = list.execute();
    
    for (File file : result.getItems()) {
      if (file.getSharedWithMeDate() == null) {
        return file.getId();
      }
    }
    
    if (isCancelled()) {
      return null;
    }
    InputStream inputStream = null;
    try {
      inputStream = context.getResources().openRawResource(R.raw.wheelly_empty_spreadsheet);
      byte[] b = new byte[inputStream.available()];
      inputStream.read(b);
      ByteArrayContent fileContent = new ByteArrayContent(OPENDOCUMENT_SPREADSHEET_MIME_TYPE, b);

      File file = new File();
      file.setTitle(fileName);
      file.setMimeType(OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
      return drive.files().insert(file, fileContent).setConvert(true).execute().getId();
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  /**
   * Gets the worksheet url.
   * 
   * @param spreadsheetService the spreadsheet service
   * @param spreadsheetId the spreadsheet id
   */
  private URL getWorksheetUrl(
      SpreadsheetService spreadsheetService,
      String spreadsheetId)
      throws IOException, ServiceException {
    
    if (isCancelled()) {
      return null;
    }
    
    URL url = new URL(String.format(Locale.US, GET_WORKSHEETS_URI, spreadsheetId));
    WorksheetFeed feed = spreadsheetService.getFeed(url, WorksheetFeed.class);
    List<WorksheetEntry> worksheets = feed.getEntries();
    
    if (worksheets.size() > 0) {
      return worksheets.get(0).getListFeedUrl();
    }
    
    return null;
  }

  /**
   * Adds track info to a worksheet.
   * 
   * @param track the track
   * @return true if completes.
   */
  protected abstract boolean addTrackInfo(
    SpreadsheetService spreadsheetService,
    URL worksheetUrl,
    EntityType track);
  
  protected abstract EntityType resolveEntity(long trackId);
  
  protected abstract String buildWorksheetTitle(EntityType track);
}