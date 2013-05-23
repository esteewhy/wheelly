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

package com.google.android.apps.mytracks;

import android.view.Menu;
import android.view.View;

/**
 * Callback when an item in the contextual action mode is selected.
 * 
 * @author Jimmy Shih
 */
public interface ContextualActionModeCallback {

  /**
   * Invoked when an item is selected.
   * 
   * @param itemId the context menu item id
   * @param position the position of the selected row
   * @param id the id of the selected row, if available
   */
  public boolean onClick(int itemId, int position, long id);

  /**
   * Invoked to prepare the menu for the selected item.
   * 
   * @param menu the menu
   * @param position the position of the selected item
   * @param id the id of the selected item
   */
  public void onPrepare(Menu menu, int position, long id);
  
  public void onCreate(Menu menu);
  
  public CharSequence getCaption(View view);
}