/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.wheelly.activity;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.widget.RemoteViews;
import com.wheelly.R;
import java.util.ArrayList;
import java.util.List;

import com.wheelly.db.HeartbeatBroker;

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

public class Widget extends AppWidgetProvider {

    private static final String WIDGET_UPDATE_ACTION = "com.wheelly.UPDATE_WIDGET";
    public static final String WIDGET_ID = "widgetId";

    public static void updateWidgets(Context context) {
        List<Integer> allWidgetIds = new ArrayList<Integer>();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, Widget.class);
        int[] widgetIds = manager.getAppWidgetIds(thisWidget);
        for (int widgetId : widgetIds) {
            allWidgetIds.add(widgetId);
        }
        int[] ids = new int[allWidgetIds.size()];
        for (int i=0; i<ids.length; i++) {
            ids[i] = allWidgetIds.get(i);
        }
        updateWidgets(context, manager, ids, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WIDGET_UPDATE_ACTION.equals(action)) {
            int widgetId = intent.getIntExtra(WIDGET_ID, INVALID_APPWIDGET_ID);
            if (widgetId != INVALID_APPWIDGET_ID) {
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                updateWidgets(context, manager, new int[]{widgetId}, true);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, false);
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds, boolean nextAccount) {
        for (int id : appWidgetIds) {
            int layoutId = manager.getAppWidgetInfo(id).initialLayout;
			RemoteViews remoteViews = buildUpdateForNextAccount(context, id, layoutId);
			manager.updateAppWidget(id, remoteViews);
        }
    }

    private static void addTapOnClick(Context context, RemoteViews updateViews) {
        Intent intent = new Intent(context, Main.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
    }

    private static RemoteViews buildUpdateForNextAccount(Context context, int widgetId, int layoutId) {
        final ContentValues recentHeartbeat = new HeartbeatBroker(context).loadOrCreate(-1);
    	
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), layoutId);
        updateViews.setProgressBar(R.id.fuel_gauge, 60, recentHeartbeat.getAsInteger("fuel"), false);
        updateViews.setTextViewText(R.id.odometer, String.valueOf(recentHeartbeat.getAsLong("odometer")));
        addTapOnClick(context, updateViews);
        return updateViews;
    }
}