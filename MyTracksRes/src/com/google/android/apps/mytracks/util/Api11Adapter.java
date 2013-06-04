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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.MapContextActionCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Vibrator;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import java.util.List;

/**
 * API level 11 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(11)
public class Api11Adapter extends Api10Adapter {

  @Override
  public void hideTitle(Activity activity) {
    // Do nothing
  }

  @Override
  public void configureActionBarHomeAsUp(Activity activity) {
    activity.getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public void configureListViewContextualMenu(final ListFragment activity,
      final ContextualActionModeCallback contextualActionModeCallback) {
    activity.getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
      ActionMode actionMode;

        @Override
      public boolean onItemLongClick(
          AdapterView<?> parent, View view, final int position, final long id) {
        if (actionMode != null) {
        //  return false;
        }
        actionMode = activity.getActivity().startActionMode(new ActionMode.Callback() {
            @Override
          public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //mode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
            contextualActionModeCallback.onCreate(menu);
            return true;
          }

            @Override
          public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            contextualActionModeCallback.onPrepare(menu, position, id);
            // Return true to indicate change
            return true;
          }

            @Override
          public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
          }

            @Override
          public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            return contextualActionModeCallback.onClick(item.getItemId(), position, id);
          }
        });
        CharSequence text = contextualActionModeCallback.getCaption(view);
        if (text != null) {
          actionMode.setTitle(text);
        }
        view.setSelected(true);
        return true;
      }
    });
  };
  
  @Override
  public void configureMapViewContextualMenu(final SupportMapFragment fragment,
      final MapContextActionCallback callback) {
	  
	fragment.getMap().setOnMarkerDragListener(new OnMarkerDragListener() {
		private LatLng position;
		private Marker currentMarker;
		private ActionMode actionMode;
		
		@Override
		public void onMarkerDragStart(Marker marker) {
			if(null == currentMarker || !marker.getId().equals(currentMarker.getId())) {
				if(null != actionMode) {
					actionMode.finish();
				}
				
				position = marker.getPosition();
				currentMarker = marker;
				marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
			}
			
			((Vibrator)fragment.getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
		}
		
		@Override
		public void onMarkerDragEnd(final Marker marker) {
			if (actionMode != null) {
				if(null == currentMarker || !marker.getId().equals(currentMarker.getId())) {
					actionMode.finish();
				} else {
					((Vibrator)fragment.getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
					return;
				}
			}
			((Vibrator)fragment.getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(new long[] { 0, 50, 100, 50, 100 }, -1);
			actionMode = fragment.getActivity().startActionMode(new ActionMode.Callback() {
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					callback.onCreate(menu);
					return true;
				}
				
				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					callback.onPrepare(menu, marker);
					// Return true to indicate change
					return true;
				}
				
				@Override
				public void onDestroyActionMode(ActionMode mode) {
					actionMode = null;
					if(null != position) {
						marker.setPosition(position);
						position = null;
						currentMarker = null;
					}
				}
				
				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					mode.finish();
					
					if(callback.onClick(item, marker)) {
						position = null;
						return true;
					}
					
					return false;
				}
			});
			
		}
		
		@Override
		public void onMarkerDrag(Marker paramMarker) { }
	});
	
    fragment.getMap().setOnMapLongClickListener(new OnMapLongClickListener() {
      private ActionMode actionMode;
      private Marker marker;
      
      @Override
      public void onMapLongClick(final LatLng point) {
        if (actionMode != null) {
          actionMode.finish();
        }
        
        marker = fragment.getMap().addMarker(new MarkerOptions()
			.position(point)
			.title("New location")
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        
        actionMode = fragment.getActivity().startActionMode(new ActionMode.Callback() {
          @Override
          public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            callback.onCreate(menu);
            return true;
          }

          @Override
          public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            callback.onPrepare(menu, marker);
            // Return true to indicate change
            return true;
          }

          @Override
          public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
			if(null != marker) {
				marker.remove();
			}
          }

          @Override
          public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            return callback.onClick(item, marker);
          }
        });
      }
    });
  };
  
  @Override
  public void configureSearchWidget(Activity activity, final MenuItem menuItem) {
    SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
    searchView.setQueryRefinementEnabled(true);
  }

  @Override
  public boolean handleSearchMenuSelection(Activity activity) {
    // Returns false to allow the platform to expand the search widget.
    return false;
  }

  @Override
  public <T> void addAllToArrayAdapter(ArrayAdapter<T> arrayAdapter, List<T> items) {
    arrayAdapter.addAll(items);
  }

  @Override
  public void invalidMenu(Activity activity) {
    activity.invalidateOptionsMenu();
  }
}
