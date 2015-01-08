/*
 * Copyright (C) 2013 Fairphone Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fairphone.updater.widgets.gapps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.R;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;

public class GoogleAppsInstallerWidget extends AppWidgetProvider
{

    private static final String TAG = GoogleAppsInstallerWidget.class.getSimpleName();

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context)
    {
        // Called once the last instance of your widget is removed from the
        // homescreen
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        // Widget instance is removed from the homescreen
        // Log.d(TAG, "onDeleted - " + appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
    {
        updateUI(context, appWidgetManager, appWidgetId);
        // Obtain appropriate widget and update it.
        // appWidgetManager.updateAppWidget(appWidgetId, new
        // RemoteViews(context.getPackageName(), R.layout.widget));
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    private void setupButtonClickIntents(Context context, RemoteViews widget)
    {
        Intent updater = new Intent(context, FairphoneUpdater.class);
        updater.setAction(GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL);
//        updater.putExtra(, true);
        PendingIntent launchUpdaterIntent = PendingIntent.getActivity(context, 0, updater, PendingIntent.FLAG_UPDATE_CURRENT);

        widget.setOnClickPendingIntent(R.id.installButton, launchUpdaterIntent);

        widget.setOnClickPendingIntent(R.id.reinstallButton, launchUpdaterIntent);

    }

    private void updateUI(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        // get the widgets
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget_google_apps_installer);

        setupButtonClickIntents(context, widget);

        SharedPreferences sharedPrefs = context.getSharedPreferences(GappsInstallerHelper.PREFS_GOOGLE_APPS_INSTALLER_DATA, Context.MODE_PRIVATE);
        int widgetCurrentState = sharedPrefs.getInt(GappsInstallerHelper.GOOGLE_APPS_INSTALLER_STATE, 0);
        switch (widgetCurrentState)
        {
            case GappsInstallerHelper.GAPPS_STATES_INITIAL:
                widget.setViewVisibility(R.id.installGroup, View.VISIBLE);
                widget.setViewVisibility(R.id.reinstallGroup, View.GONE);
                break;
            case GappsInstallerHelper.GAPPS_INSTALLED_STATE:
                widget.setViewVisibility(R.id.installGroup, View.GONE);
                widget.setViewVisibility(R.id.reinstallGroup, View.VISIBLE);
                break;

            default:
                break;
        }

        // update the widget data
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // Called in response to the ACTION_APPWIDGET_UPDATE broadcast when this
        // AppWidget provider
        // is being asked to provide RemoteViews for a set of AppWidgets.
        // Override this method to implement your own AppWidget functionality.

        // iterate through every instance of this widget
        // remember that it can have more than one widget of the same type.
        for (int i = 0; i < appWidgetIds.length; i++)
        { // See the dimensions
          // and
            System.out.println("Updating widget #" + i);
            updateUI(context, appWidgetManager, appWidgetIds[i]);
        }

    }
}
