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
package com.fairphone.updater.gappsinstaller;

import java.io.File;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.fairphone.updater.widgets.gapps.GoogleAppsInstallerWidget;

public class GappsInstallerHelper
{
    protected static final String TAG = GappsInstallerHelper.class.getSimpleName();

    public static final String PREFS_GOOGLE_APPS_INSTALLER_DATA = "FAIRPHONE_GOOGLE_APPS_INSTALLER_DATA";
    public static final String GOOGLE_APPS_INSTALLER_STATE = "com.fairphone.updater.gapps.WIDGET_STATE";
    public static final String GAPPS_REINSTALATION = "com.fairphone.updater.gapps.GAPPS_REINSTALATION_REQUEST";

    public static final int GAPPS_STATES_INITIAL = 0;
    public static final int GAPPS_INSTALLED_STATE = 1;

    public static String EXTRA_START_GAPPS_INSTALL = "com.fairphone.updater.gapps.EXTRA_START_GAPPS_INSTALL";;

    private Context mContext;
    private SharedPreferences mSharedPrefs;

    public GappsInstallerHelper(Context context)
    {
        mContext = context;

        mSharedPrefs = mContext.getSharedPreferences(PREFS_GOOGLE_APPS_INSTALLER_DATA, Context.MODE_PRIVATE);

        checkGappsAreInstalled();
    }

    public static boolean areGappsInstalled()
    {
        File f = new File("/system/app/OneTimeInitializer.apk");

        return f.exists();
    }

    private boolean checkGappsAreInstalled()
    {
        if (areGappsInstalled())
        {
            updateWidgetState(GAPPS_INSTALLED_STATE);
            return true;
        }

        updateWidgetState(GAPPS_STATES_INITIAL);

        return false;
    }

    private void updateGoogleAppsIntallerWidgets()
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(mContext, GoogleAppsInstallerWidget.class));
        if (appWidgetIds.length > 0)
        {
            new GoogleAppsInstallerWidget().onUpdate(mContext, appWidgetManager, appWidgetIds);
        }
    }

    private void updateInstallerState(int state)
    {
        // alter State
        SharedPreferences.Editor prefEdit = mSharedPrefs.edit();
        prefEdit.putInt(GOOGLE_APPS_INSTALLER_STATE, state);
        prefEdit.commit();
    }

    public void updateWidgetState(int state)
    {
        updateInstallerState(state);

        updateGoogleAppsIntallerWidgets();
    }
}
