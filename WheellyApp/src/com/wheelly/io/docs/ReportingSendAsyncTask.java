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

import com.wheelly.service.ProgressNotifier;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Derived from com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendAsyncTask,
 * but abstracted from AbstractSendActivity to use status bar notifications instead.
 */
public abstract class ReportingSendAsyncTask extends AsyncTask<Long, Integer, Boolean> {
	protected final ProgressNotifier n; 
	
/**
   * True if the AsyncTask result is success.
   */
  private boolean success;

  /**
   * True if can retry the AsyncTask.
   */
  private boolean canRetry;

  /**
   * Creates an AsyncTask.
   *
   * @param activity the activity currently associated with this AsyncTask
   */
	public ReportingSendAsyncTask(Context context) {
		n = new ProgressNotifier(context);
		success = false;
		canRetry = true;
	}
	
	@Override
	protected void onPreExecute() {
		n.startProgress();
	}

	@Override
	protected Boolean doInBackground(Long... params) {
		try {
			return performTask();
		} finally {
			closeConnection();
			if (success) {
				saveResult();
			}
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		n.notifyProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		n.reportResult(result);
	}

  /**
   * Retries the task. First, invalidates the auth token. If can retry, invokes
   * {@link #performTask()}. Returns false if cannot retry.
   *
   * @return the result of the retry.
   */
  protected boolean retryTask() {
    if (isCancelled()) {
      return false;
    }

    invalidateToken();
    if (canRetry) {
      canRetry = false;
      return performTask();
    }
    return false;
  }

  /**
   * Closes any AsyncTask connection.
   */
  protected abstract void closeConnection();

  /**
   * Saves any AsyncTask result.
   */
  protected abstract void saveResult();

  /**
   * Performs the AsyncTask.
   *
   * @return true if success
   */
  protected abstract boolean performTask();

  /**
   * Invalidates the auth token.
   */
  protected abstract void invalidateToken();
}