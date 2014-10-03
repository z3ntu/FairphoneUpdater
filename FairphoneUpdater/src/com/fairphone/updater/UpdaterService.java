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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;
import com.fairphone.updater.tools.RSAUtils;
import com.fairphone.updater.tools.Utils;
import com.fairphone.updater.widgets.gapps.GoogleAppsInstallerWidget;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class UpdaterService extends Service {

	public static final String ACTION_FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD = "FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD";

	private static final String TAG = UpdaterService.class.getSimpleName();
	
	private static final String PREFERENCE_LAST_CONFIG_DOWNLOAD_ID = "LastConfigDownloadId";
	private DownloadManager mDownloadManager = null;
	private DownloadBroadCastReceiver mDownloadBroadCastReceiver = null;

    private static final int MAX_DOWNLOAD_RETRIES = 3;
    private int mDownloadRetries;
	private long mLatestFileDownloadId;

	private SharedPreferences mSharedPreferences;
	
	final static long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
	
    private GappsInstallerHelper mGappsInstaller;

	private BroadcastReceiver mBCastConfigFileDownload;
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
	    // remove the logs
	    clearDataLogs();
	    
		mSharedPreferences = getApplicationContext().getSharedPreferences(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

		mLatestFileDownloadId = mSharedPreferences.getLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, 0);
		
	    if(hasInternetConnection() ){
			
	        setupDownloadManager();
	
			downloadConfigFile();
		}
		
	    // setup the gapps installer
     	mGappsInstaller = new GappsInstallerHelper(getApplicationContext());
     	
     	mBCastConfigFileDownload = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if(hasInternetConnection()){
					downloadConfigFile();
				}
			}
		};

		getApplicationContext().registerReceiver(mBCastConfigFileDownload, new IntentFilter(
				ACTION_FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD));
     	
		return Service.START_STICKY;
	}

	private void downloadConfigFile() {
		// remove the old file if its still there for some reason
		removeLatestFileDownload(getApplicationContext());
		
		// start the download of the latest file
		startDownloadLatest();
	}

    public void updateGoogleAppsIntallerWidgets() {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(this, GoogleAppsInstallerWidget.class));
		if (appWidgetIds.length > 0) {
			new GoogleAppsInstallerWidget().onUpdate(this, appWidgetManager, appWidgetIds);
		}
	}
    
    protected void clearDataLogs(){
        try
        {
            Log.d(TAG, "Clearing dump log data...");
            Shell.runCommand(new CommandCapture(0, "rm /data/log_other_mode/*_log"));
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TimeoutException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void startDownloadLatest() {
		if (hasConnection()) {
			Resources resources = getApplicationContext().getResources();
			String downloadLink = getConfigDownloadLink(getApplicationContext());
			// set the download for the latest version on the download manager
			Request request = createDownloadRequest(
					downloadLink,
					resources.getString(R.string.configFilename)
							+ resources.getString(R.string.config_zip));
			
			if (request != null && mDownloadManager != null) {
				mLatestFileDownloadId = mDownloadManager.enqueue(request);

				Editor editor = mSharedPreferences.edit();
				editor.putLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID,
						mLatestFileDownloadId);
				editor.commit();
			} else {
				Log.e(TAG, "Invalid request for link " + downloadLink);
				Intent i = new Intent(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
				i.putExtra(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK, downloadLink);
				sendBroadcast(i);
			}
		}
	}

	private String getConfigDownloadLink(Context context) {
		
	    Resources resources = context.getResources();
	    
		StringBuilder sb = new StringBuilder();
		if(FairphoneUpdater2Activity.DEV_MODE_ENABLED){
		    sb.append(resources.getString(R.string.downloadDevUrl));
		} else {
		    sb.append(resources.getString(R.string.downloadUrl));
		}
		sb.append(Build.MODEL.replaceAll("\\s",""));
		sb.append(getPartitionDownloadPath(resources));
		sb.append("/");
		
		sb.append(resources.getString(R.string.configFilename));
		
		sb.append(resources.getString(R.string.config_zip));
		
		addModelAndOS(context, sb);
		
		String downloadLink = sb.toString();
		
		Log.d(TAG, "Download link: " + downloadLink);
		
		return downloadLink;
	}

    private void addModelAndOS(Context context, StringBuilder sb)
    {
        // attach the model and the os
		sb.append("?");
		sb.append("model="+Build.MODEL.replaceAll("\\s",""));
		Version currentVersion = VersionParserHelper
                .getDeviceVersion(context.getApplicationContext());
        
		if(currentVersion != null){
		    sb.append("&");
		    sb.append("os="+currentVersion.getAndroidVersion());
		}
    }

	private String getPartitionDownloadPath(Resources resources) {
		String downloadPath = "";
		String modelWithoutSpaces = Build.MODEL.replaceAll("\\s","");
		if (modelWithoutSpaces.equals(resources.getString(R.string.FP1Model))) {
			File path = Environment.getDataDirectory();
			double sizeInGB = Utils.getPartitionSizeInGBytes(path);
			double roundedSize = (double) Math.ceil(sizeInGB * 100d) / 100d;
			Log.d(TAG, path.getPath() + " size: " + roundedSize + "Gb");
			
			double fp1DataPartitionSize = (double)resources.getInteger(R.integer.FP1DataPartitionSizeMb) / 100d;
			//Add a little buffer to the 1gb default just in case
			downloadPath = roundedSize <= fp1DataPartitionSize ? resources
					.getString(R.string.oneGBDataPartition) : resources
					.getString(R.string.unifiedDataPartition);
		}
		return downloadPath;
	}

	private boolean hasConnection() {
		return isWiFiEnabled();
	}
	
	private boolean isWiFiEnabled() {

		ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();

		return isWifi;
	}

	private static void setNotification(Context currentContext) {

		Context context = currentContext.getApplicationContext();

		NotificationManager manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context)
				.setSmallIcon(R.drawable.fairphone_updater_tray_icon_small)
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.fairphone_updater_tray_icon))
				.setContentTitle(
						context.getResources().getString(R.string.app_name))
				.setContentText(context.getResources().getString(R.string.fairphoneUpdateMessage));

		Intent resultIntent = new Intent(context, FairphoneUpdater2Activity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

		stackBuilder.addParentStack(FairphoneUpdater2Activity.class);

		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(resultPendingIntent);

		Notification notificationWhileRunnig = builder.build();
		
		// Add notification
		manager.notify(0, notificationWhileRunnig);
	}

	private Request createDownloadRequest(String url, String fileName) {

		Resources resources = getApplicationContext().getResources();
		Request request;
		
		try {
			request = new Request(Uri.parse(url));
			Environment.getExternalStoragePublicDirectory(
					Environment.getExternalStorageDirectory()
							+ resources.getString(R.string.updaterFolder))
					.mkdirs();

			request.setDestinationInExternalPublicDir(
					resources.getString(R.string.updaterFolder), fileName);
			request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
			request.setAllowedOverRoaming(false);
			request.setTitle(resources.getString(R.string.downloadUpdateTitle));
		} catch (Exception e) {
			Log.e(TAG, "Error creating request: " + e.getMessage());
			request = null;
		}

		return request;
	}

	private boolean hasInternetConnection() {

		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.isConnectedOrConnecting();

		boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();

		return isWifi || is3g;
	}

	private void setupDownloadManager() {
		if(mDownloadManager == null){
			mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		}

		if(mDownloadBroadCastReceiver != null){
			mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();
	
			getApplicationContext().registerReceiver(mDownloadBroadCastReceiver,
					new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		}
	}

	private void removeBroadcastReceiver() {
		getApplicationContext().unregisterReceiver(mDownloadBroadCastReceiver);
		mDownloadBroadCastReceiver = null;
	}
	
	private static void checkVersionValidation(Context context){
	    
		Version latestVersion = VersionParserHelper
				.getLatestVersion(context.getApplicationContext());
		Version currentVersion = VersionParserHelper
				.getDeviceVersion(context.getApplicationContext());
		
		if(latestVersion != null){
			if(latestVersion.isNewerVersionThan(currentVersion)){			
				setNotification(context);
			} 
			//to update the activity
			Intent updateIntent = new Intent(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
	        context.sendBroadcast(updateIntent);
		}
	}
	
    public static boolean readUpdaterData(Context context) {

        boolean retVal = false;
        Resources resources = context.getApplicationContext().getResources();
        String targetPath = Environment.getExternalStorageDirectory()
                + resources.getString(R.string.updaterFolder);

        String filePath = targetPath + resources.getString(R.string.configFilename)
                + resources.getString(R.string.config_zip);

        File file = new File(filePath);

        if (file.exists()){
        	if(RSAUtils.checkFileSignature(context, filePath, targetPath)) {
	            checkVersionValidation(context);
	            retVal = true;
        	}
        	else {
        		Toast.makeText(context, resources.getString(R.string.invalidSignatureDownloadMessage), Toast.LENGTH_LONG).show();
        		file.delete();
            }
        } 

        return retVal;
    }

    private void removeLatestFileDownload(Context context) {
        if(mLatestFileDownloadId != 0 && mDownloadManager != null){
        	mDownloadManager.remove(mLatestFileDownloadId);
        	mLatestFileDownloadId = 0;
        }
        VersionParserHelper.removeFiles(context);
    }

    private boolean retryDownload(Context context) {
		//invalid file
    	boolean removeReceiver = true;
		removeLatestFileDownload(context);
		if(mDownloadRetries < MAX_DOWNLOAD_RETRIES){
		    startDownloadLatest();
		    mDownloadRetries++;
		    removeReceiver = false;
		}
		if(removeReceiver){
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.configFileDownloadError), Toast.LENGTH_LONG).show();
		}
		return removeReceiver;
	}

	private class DownloadBroadCastReceiver extends BroadcastReceiver {

        @Override
		public void onReceive(Context context, Intent intent) {
		    
		    boolean removeReceiver = false;
		    
			DownloadManager.Query query = new DownloadManager.Query();

			query.setFilterById(mLatestFileDownloadId);
			Cursor cursor = mDownloadManager.query(query);
			
			if (cursor.moveToFirst()) {
				int columnIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);
				Resources resources = context.getApplicationContext().getResources();
				
				switch (status) {
					case DownloadManager.STATUS_SUCCESSFUL:
					{
					    String filePath = mDownloadManager.getUriForDownloadedFile(
					            mLatestFileDownloadId).getPath();
					    
					    String targetPath = Environment.getExternalStorageDirectory()
			                    + resources.getString(R.string.updaterFolder);
	                    
					    if(RSAUtils.checkFileSignature(context, filePath, targetPath)){
	    					checkVersionValidation(context);
						}else{
							Toast.makeText(getApplicationContext(), resources.getString(R.string.invalidSignatureDownloadMessage), Toast.LENGTH_LONG).show();
						    removeReceiver = retryDownload(context);
						}
					    break;
					}
					case DownloadManager.STATUS_FAILED:
					{
	            		removeReceiver = retryDownload(context);
		                break;
					}
				}
			}
			cursor.close();

			if(removeReceiver){
			    removeBroadcastReceiver();
			}
		}
	}
}