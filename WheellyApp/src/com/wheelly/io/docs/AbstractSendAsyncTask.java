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
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
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
		EntityType track = resolveEntity(trackId);
		
		if (track == null) {
			Log.d(TAG, "No track for " + trackId);
			return false;
		}
		
		String title = buildWorksheetTitle(track);
		
		publishProgress(PROGRESS_GET_SPREADSHEET_ID);
		
		try {
			if (isCancelled()) {
				return false;
			}
			
			String spreadsheetId = getSpreadSheetId(title);
			
			if (spreadsheetId == null) {
				Log.d(TAG, "Unable to get the spreadsheet ID for " + title);
				return false;
			}
			
			publishProgress(PROGRESS_GET_WORKSHEET_URL);
			SpreadsheetService spreadsheetService = getSpreadhseetService();
			
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
		} catch(UserRecoverableNotifiedException e) {
			return false;
		} catch (UserRecoverableAuthException e) {
			SendToGoogleUtils.sendNotification(
					context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEETS_NOTIFICATION_ID);
			return false;
		} catch (GoogleAuthException e) {
			Log.e(TAG, "GoogleaAuthException", e);
			return retryTask();
		} catch (UserRecoverableAuthIOException e) {
			SendToGoogleUtils.sendNotification(
					context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEETS_NOTIFICATION_ID);
			return false;
		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
			return retryTask();
		} catch (ServiceException e) {
			Log.e(TAG, "ServiceException", e);
			return retryTask();
		} finally {
			n.canceNotificationForMileage(1);
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
		FileList result = getDriveService(DriveScopes.DRIVE_METADATA_READONLY)
			.files()
			.list()
			.setQ(String.format(Locale.US, GET_SPREADSHEET_QUERY, fileName))
			.execute();
		
		for (File file : result.getItems()) {
			if (file.getSharedWithMeDate() == null) {
				return file.getId();
			}
		}
		return createSpreadsheet(fileName);
	}
	
	private Drive getDriveService(String scope)
			throws UserRecoverableNotifiedException, IOException, GoogleAuthException {
		GoogleAccountCredential credential = GoogleAccountCredential
			.usingOAuth2(context, scope)
			.setSelectedAccountName(account.name);
		
		GoogleAuthUtil.getTokenWithNotification(context, account.name, "oauth2:" + scope, null);
		
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
			.setApplicationName("Wheelly")
			.build();
	}
	
	private SpreadsheetService getSpreadhseetService() throws IOException, GoogleAuthException {
		return new SpreadsheetService("Wheelly") {{
			setOAuth2Credentials(
				new Credential(BearerToken.authorizationHeaderAccessMethod())
				.setAccessToken(
					GoogleAuthUtil.getTokenWithNotification(
						context,
						account.name,
						"oauth2:" + SendToGoogleUtils.SPREADSHEETS_SCOPE,
						null
					)
				)
			);
		}};
	}
	
	private String createSpreadsheet(String fileName) throws IOException, UserRecoverableNotifiedException, GoogleAuthException {
		Drive drive = getDriveService(DriveScopes.DRIVE_FILE);
		InputStream inputStream = null;
		
		try {
			inputStream = context.getResources().openRawResource(R.raw.wheelly_empty_spreadsheet);
			byte[] b = new byte[inputStream.available()];
			inputStream.read(b);
			ByteArrayContent fileContent = new ByteArrayContent(OPENDOCUMENT_SPREADSHEET_MIME_TYPE, b);
			
			File file = new File()
				.setTitle(fileName)
				.setMimeType(OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
			
			return drive.files()
				.insert(file, fileContent)
				.setConvert(true)
				.execute()
				.getId();
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