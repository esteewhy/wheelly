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
package com.google.android.apps.mytracks.util;

import com.androidmapsextensions.SupportMapFragment;
import com.google.android.apps.mytracks.MapContextActionCallback;

import android.content.SharedPreferences;

/**
 * A set of methods that may be implemented differently depending on the Android
 * API level.
 *
 * @author Bartlomiej Niechwiej
 */
public interface ApiAdapter {

  /**
   * Applies all the changes done to a given preferences editor. Changes may or
   * may not be applied immediately.
   * <p>
   * Due to changes in API level 9.
   * 
   * @param editor the editor
   */
  public void applyPreferenceChanges(SharedPreferences.Editor editor);

  public void configureMapViewContextualMenu(SupportMapFragment fragment,
	      MapContextActionCallback callback);
}