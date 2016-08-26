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

package com.fairphone.updater;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;
import com.fairphone.updater.tools.PrivilegeChecker;
import com.fairphone.updater.tools.RSAUtils;
import com.fairphone.updater.tools.Utils;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.TimeoutException;


public class UpdaterService extends Service
{

    public static final String LAST_CONFIG_DOWNLOAD_IN_MS = "LAST_CONFIG_DOWNLOAD_IN_MS";
    private static final int CONFIG_FILE_DOWNLOAD_TIMEOUT_MILLIS = 23500;
    public static final String EXTRA_FORCE_CONFIG_FILE_DOWNLOAD = "FORCE_DOWNLOAD";
    
    private static final String TAG = UpdaterService.class.getSimpleName();

    public static final String PREFERENCE_LAST_CONFIG_DOWNLOAD_ID = "LastConfigDownloadId";
    public static final String PREFERENCE_REINSTALL_GAPPS = "ReinstallGappsOmnStartUp";
    public static final String PREFERENCE_CONFIG_MD_5 = "CONFIG_MD5";
    private DownloadManager mDownloadManager = null;
    private DownloadBroadCastReceiver mDownloadBroadCastReceiver = null;

    private static final int MAX_DOWNLOAD_RETRIES = 3;
    private int mDownloadRetries;
    private long mLatestFileDownloadId;
    private boolean mInternetConnectionAvailable;

	private SharedPreferences mSharedPreferences;

    private final static long DOWNLOAD_GRACE_PERIOD_IN_MS = 24 /* hour */ * Utils.MINUTES_IN_HOUR /* minute */ * Utils.SECONDS_IN_MINUTE /* second */ * 1000 /* millisecond */;
    private BroadcastReceiver networkStateReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // remove the logs
        clearDataLogs();
        
        if(Utils.isDeviceUnsupported(getApplicationContext())){
            stopSelf();
            return START_NOT_STICKY;
        }

        mSharedPreferences = getApplicationContext().getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

        mLatestFileDownloadId = mSharedPreferences.getLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, 0);

        setupDownloadManager();

        setupConnectivityMonitoring();

        if(Utils.isInternetEnabled(getApplicationContext()))
        {
            downloadConfigFile(intent != null && intent.getBooleanExtra(EXTRA_FORCE_CONFIG_FILE_DOWNLOAD, false));
        }

        // setup the gapps installer
        GappsInstallerHelper.checkGappsAreInstalled(getApplicationContext());

        runInstallationDisclaimer(getApplicationContext());

        return Service.START_STICKY;
    }

    private static void runInstallationDisclaimer(Context context)
    {
	    SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);
	    if (sharedPreferences.getBoolean(PREFERENCE_REINSTALL_GAPPS, true) && !UpdaterData.getInstance().isAppStoreListEmpty())
        {
            if(!GappsInstallerHelper.areGappsInstalled()){
                showReinstallAlert(context);
            }

            Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREFERENCE_REINSTALL_GAPPS, false);

            editor.apply();
        }
    }

    private static void showReinstallAlert(Context context)
    {
	    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        //Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getResources().getString(R.string.supportAppStoreUrl)));

        Intent notificationIntent = new Intent(context, FairphoneUpdater.class);
        notificationIntent.setAction(GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL);
        
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

	    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.updater_white)
			    .setContentTitle(context.getResources().getString(R.string.app_full_name))
			    .setContentText(context.getResources().getString(R.string.appStoreReinstall))
			    .setAutoCancel(true)
			    .setDefaults(Notification.DEFAULT_SOUND)
			    .setContentIntent(contentIntent);
        
        mNotificationManager.notify(0, mBuilder.build());
    }

    private void downloadConfigFile(boolean forceDownload)
    {
        long now = System.currentTimeMillis();
        long last_download = mSharedPreferences.getLong(LAST_CONFIG_DOWNLOAD_IN_MS, 0L);
        if( forceDownload || now > (last_download + DOWNLOAD_GRACE_PERIOD_IN_MS) ) {
            Log.i(TAG, "Downloading updater configuration file.");
            // remove the old file if its still there for some reason
            removeLatestFileDownload(getApplicationContext());
    
            // start the download of the latest file
            startDownloadLatest();

            mSharedPreferences.edit().putLong(LAST_CONFIG_DOWNLOAD_IN_MS, now).apply();
        }
    }

// --Commented out by Inspection START (06/02/2015 12:27):
//    public void updateGoogleAppsIntallerWidgets()
//    {
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, GoogleAppsInstallerWidget.class));
//        if (appWidgetIds.length > 0)
//        {
//            new GoogleAppsInstallerWidget().onUpdate(this, appWidgetManager, appWidgetIds);
//        }
//    }
// --Commented out by Inspection STOP (06/02/2015 12:27)

    private static void clearDataLogs()
    {
        if (PrivilegeChecker.isPrivilegedApp()) {
	        clearDataLogsPrivileged();
        } else {
	        clearDataLogsUnprivileged();
        }
    }

	private static void clearDataLogsPrivileged()
	{
		try
		{
			Log.d(TAG, "Clearing dump log data...");
			Process p = Runtime.getRuntime().exec("rm /data/log_other_mode/*_log");
            try
            {
                p.waitFor();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		} catch (IOException e)
		{
			Log.d(TAG, "Clearing dump log data failed: " + e.getLocalizedMessage());
		}
	}

	private static void clearDataLogsUnprivileged()
	{
		try
		{
			Log.d(TAG, "Clearing dump log data...");
			Shell.runCommand(new CommandCapture(0, "rm /data/log_other_mode/*_log"));
		} catch (IOException | TimeoutException e)
		{
			Log.d(TAG, "Clearing dump log data failed: " + e.getLocalizedMessage());
		}
	}


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    void startDownloadLatest()
    {
        Resources resources = getApplicationContext().getResources();
        String downloadLink = getConfigDownloadLink(getApplicationContext());
        // set the download for the latest version on the download manager
        Request request = createDownloadRequest(downloadLink, resources.getString(R.string.configFilename) + resources.getString(R.string.config_zip));
        request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);

        if (request != null && mDownloadManager != null)
        {
            // Allow download over mobile data and Wi-Fi
            request.setAllowedNetworkTypes(Request.NETWORK_MOBILE|Request.NETWORK_WIFI);

            //Guarantee that only we have only one download
            long oldDownloadId = mSharedPreferences.getLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, 0);
            if(oldDownloadId != 0){
                mDownloadManager.remove(oldDownloadId);
                saveLatestDownloadId(0);
            }
            
            mLatestFileDownloadId = mDownloadManager.enqueue(request);
            saveLatestDownloadId(mLatestFileDownloadId);
            
            final long currentId = mLatestFileDownloadId;
            // Cancel download if it is stuck since DownloadManager doesn't seem able to do it.
            new Handler().postAtTime(new Runnable() {
                @Override
                public void run() {
                    onDownloadStatus(currentId, new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "Configuration file download timed out");
                            mDownloadManager.remove(currentId);
                            mSharedPreferences.edit().remove(LAST_CONFIG_DOWNLOAD_IN_MS).apply();
                        }
                    });
                }
            }, SystemClock.uptimeMillis() + CONFIG_FILE_DOWNLOAD_TIMEOUT_MILLIS);
        }
        else
        {
            Log.e(TAG, "Invalid request for link " + downloadLink);
            Intent i = new Intent(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
            i.putExtra(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK, downloadLink);
            sendBroadcast(i);
        }
    }

    private void saveLatestDownloadId(long id)
    {
        mLatestFileDownloadId = id;
        Editor editor = mSharedPreferences.edit();
        editor.putLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, id);
        editor.commit();
    }

    private String getConfigDownloadLink(Context context)
    {

        Resources resources = context.getResources();

        StringBuilder sb = new StringBuilder();
        String download_url = mSharedPreferences.getString(FairphoneUpdater.PREFERENCE_OTA_DOWNLOAD_URL, getResources().getString(R.string.downloadUrl));

	    sb.append(download_url);
        sb.append(Build.MODEL.replaceAll("\\s", ""));
        sb.append(Utils.getPartitionDownloadPath(resources));
        sb.append("/");

        sb.append(resources.getString(R.string.configFilename));

        sb.append(resources.getString(R.string.config_zip));

        addUrlParameters(context, sb);

        String downloadLink = sb.toString();

        Log.d(TAG, "Download link: " + downloadLink);

        return downloadLink;
    }

    private static void addUrlParameters(Context context, StringBuilder sb)
    {
        sb.append("?");
        Version currentVersion = VersionParserHelper.getDeviceVersion(context.getApplicationContext());

        if (currentVersion != null) {
            try {
                final String defaultCharset = Charset.defaultCharset().displayName();

                String modelWithoutSpaces = Build.MODEL.replaceAll("\\s", "");
                if(modelWithoutSpaces.startsWith(context.getResources().getString(R.string.FP1Model))) {
                    sb.append("&b_n=").append(URLEncoder.encode(currentVersion.getBuildNumber(), defaultCharset));
                }
                sb.append("&ap=").append(URLEncoder.encode(String.valueOf(currentVersion.getId()), defaultCharset));
                if(modelWithoutSpaces.equals(context.getResources().getString(R.string.FP2Model))) {
                    sb.append("&bp=").append(URLEncoder.encode(String.valueOf(currentVersion.getBasebandVersion()), defaultCharset));
                }
                sb.append("&u=").append(URLEncoder.encode(String.valueOf(Utils.getVersionCode(context.getApplicationContext())), defaultCharset));
                if(FairphoneUpdater.BETA_MODE_ENABLED) {
                    sb.append("&beta=").append(BetaEnabler.BETA_ENABLED);
                }
                if(FairphoneUpdater.DEV_MODE_ENABLED) {
                    sb.append("&dev=").append("1");
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to add extra info on update request: "+e.getLocalizedMessage());
            }
        }
    }

    private static void setNotification(Context currentContext)
    {
        Context context = currentContext.getApplicationContext();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context).setSmallIcon(R.drawable.updater_white)
                        .setContentTitle(context.getResources().getString(R.string.app_full_name))
                        .setContentText(context.getResources().getString(R.string.fairphone_update_message));

        Intent resultIntent = new Intent(context, FairphoneUpdater.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(FairphoneUpdater.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        Notification notificationWhileRunnig = builder.build();

        // Add notification
        manager.notify(0, notificationWhileRunnig);
    }

    private Request createDownloadRequest(String url, String fileName)
    {

        Resources resources = getApplicationContext().getResources();
        Request request;

        try
        {
            request = new Request(Uri.parse(url));
	        final File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));
	        final boolean notMkDirs = !externalStoragePublicDirectory.mkdirs();
	        if(notMkDirs && !externalStoragePublicDirectory.exists()) {
		        throw new Exception("Couldn't create updater dir structures.");
	        }

	        request.setDestinationInExternalPublicDir(resources.getString(R.string.updaterFolder), fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setAllowedOverRoaming(false);
            request.setTitle(resources.getString(R.string.fairphone_update_message_title));
        } catch (Exception e)
        {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            request = null;
        }

        return request;
    }

    private void setupConnectivityMonitoring()
    {

        if (networkStateReceiver == null) {
            // Check current connectivity status
            mInternetConnectionAvailable = Utils.isInternetEnabled(getApplicationContext());

            // Setup monitoring for future connectivity status changes
            networkStateReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                        Log.i(TAG, "Lost network connectivity.");
                        mInternetConnectionAvailable = false;
                        if (mLatestFileDownloadId != 0 && mDownloadManager != null)
                        {
                            onDownloadStatus(mLatestFileDownloadId, new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Removing pending download.");
                                    mDownloadManager.remove(mLatestFileDownloadId);
                                    saveLatestDownloadId(0);
                                }
                            });
                        }
                    }
                    else
                    {
                        int conn_type = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                        if( conn_type == ConnectivityManager.TYPE_WIFI || conn_type == ConnectivityManager.TYPE_MOBILE) {
                            Log.i(TAG, "Network connectivity potentially available.");
                            if (!mInternetConnectionAvailable) {
                                downloadConfigFile(false);
                            }
                            mInternetConnectionAvailable = true;
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkStateReceiver, filter);

        }
    }

    private void setupDownloadManager()
    {
        if (mDownloadManager == null)
        {
            mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        }

        if (mDownloadBroadCastReceiver == null)
        {
            mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();

            getApplicationContext().registerReceiver(mDownloadBroadCastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private static void copyConfigToData(Context context) throws IOException {
        Resources resources = context.getApplicationContext().getResources();
        String targetPath = Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder);
        FileInputStream inStream = new FileInputStream(targetPath + resources.getString(R.string.configFilename) + resources.getString(R.string.config_xml));
        FileOutputStream outStream = context.openFileOutput(resources.getString(R.string.configFilename) + resources.getString(R.string.config_xml), MODE_PRIVATE);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    private static void checkVersionValidation(Context context)
    {
        Version latestVersion = VersionParserHelper.getLatestVersion(context.getApplicationContext());
        Version currentVersion = VersionParserHelper.getDeviceVersion(context.getApplicationContext());
        if (latestVersion != null)
        {
            if (latestVersion.isNewerVersionThan(currentVersion))
            {
                setNotification(context);
            }
        }
	    runInstallationDisclaimer(context);

        // to update the activity
        Intent updateIntent = new Intent(FairphoneUpdater.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
        context.sendBroadcast(updateIntent);
    }

    public static boolean readUpdaterData(Context context){
        Version latestVersion = VersionParserHelper.getLatestVersion(context.getApplicationContext());
        return latestVersion != null;
    }

    public static boolean updateUpdaterData(Context context)
    {

        boolean retVal = false;
        Resources resources = context.getApplicationContext().getResources();
        String targetPath = Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder);

        String filePath = targetPath + resources.getString(R.string.configFilename) + resources.getString(R.string.config_zip);

        File file = new File(filePath);

        if (file.exists())
        {
            String md5sum = Utils.calculateMD5(file);
            SharedPreferences sp = context.getApplicationContext().getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);
            if(sp.getString(PREFERENCE_CONFIG_MD_5, "").equals(md5sum)){
                retVal = true;
            } else {
                if (RSAUtils.checkFileSignature(context, filePath, targetPath)) {
                    try {
                        copyConfigToData(context);
                        checkVersionValidation(context);
                        retVal = true;
                        sp.edit().putString(PREFERENCE_CONFIG_MD_5, md5sum).apply();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to store configuration " + e.getLocalizedMessage());
                        retVal = false;
                    }
                } else {
                    //Toast.makeText(context, resources.getString(R.string.invalid_signature_download_message), Toast.LENGTH_LONG).show();
                    final boolean notDeleted = !file.delete();
                    if(notDeleted) {
                        Log.d(TAG, "Unable to delete "+file.getAbsolutePath());
                    }

                }
            }
        } else {
            Log.w(TAG, "No file");
        }

        return retVal;
    }

    private void removeLatestFileDownload(Context context)
    {
        if (mLatestFileDownloadId != 0 && mDownloadManager != null)
        {
            mDownloadManager.remove(mLatestFileDownloadId);
            saveLatestDownloadId(0);
        }
        VersionParserHelper.removeConfigFiles(context);
    }

    private boolean retryDownload(Context context)
    {
        Log.d(TAG, "Retry "+mDownloadRetries+" of "+MAX_DOWNLOAD_RETRIES);
        // invalid file
        boolean removeReceiver = true;
        removeLatestFileDownload(context);
        if (mDownloadRetries < MAX_DOWNLOAD_RETRIES)
        {
            startDownloadLatest();
            mDownloadRetries++;
            removeReceiver = false;
        }
        return removeReceiver;
    }

    private void onDownloadStatus(long id, Runnable ifRunning) {
        Cursor cursor = mDownloadManager != null ? mDownloadManager.query(new DownloadManager.Query().setFilterById(id)) : null;
        if (cursor != null && cursor.moveToFirst())
        {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch(status){
                case DownloadManager.STATUS_PAUSED:
                    if (ifRunning != null) {
                        ifRunning.run();
                    }
                    break;
                case DownloadManager.STATUS_PENDING:
                    if (ifRunning != null) {
                        ifRunning.run();
                    }
                    break;
                case DownloadManager.STATUS_RUNNING:
                    if (ifRunning != null) {
                        ifRunning.run();
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                case DownloadManager.STATUS_SUCCESSFUL:
                default:
                    break;
            }
        }
        if (cursor != null)
        {
            cursor.close();
        }
    }

    private class DownloadBroadCastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {

            boolean removeReceiver = false;
            DownloadManager.Query query = new DownloadManager.Query();

            query.setFilterById(mLatestFileDownloadId);

            Cursor cursor = mDownloadManager != null ? mDownloadManager.query(query) : null;

            if (cursor != null && cursor.moveToFirst())
            {
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                Resources resources = context.getApplicationContext().getResources();

                switch (status)
                {
                    case DownloadManager.STATUS_SUCCESSFUL:
                    {
                        Log.d(TAG, "Download successful.");

                        if (updateUpdaterData(context))
                        {
                            removeLatestFileDownload(context);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), resources.getString(R.string.invalid_signature_download_message), Toast.LENGTH_LONG).show();
                            removeReceiver = retryDownload(context);
                        }
                        break;
                    }
                    case DownloadManager.STATUS_FAILED:
                    {
                        Log.d(TAG, "Download failed.");
                        removeReceiver = retryDownload(context);
                        break;
                    }
                    default:
                    {
                        Log.d(TAG, "Status broadcast on mLatestFileDownloadId ("+mLatestFileDownloadId+"): "+ status);
                    }
                }
            }

            if (cursor != null)
            {
                cursor.close();
            }

            if (removeReceiver)
            {
                Log.d(TAG, "Configuration download failed. Clearing grace period.");
                mSharedPreferences.edit().remove(LAST_CONFIG_DOWNLOAD_IN_MS).apply();
            }
        }
    }
}
