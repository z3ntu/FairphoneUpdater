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
import android.content.BroadcastReceiver;
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

import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class UpdaterService extends Service {

	private static final String TAG = UpdaterService.class.getSimpleName();
	
	private static final String PREFERENCE_LAST_CONFIG_DOWNLOAD_ID = "LastConfigDownloadId";
	private DownloadManager mDownloadManager;
	private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

    private static final int MAX_DOWNLOAD_RETRIES = 3;
    private int mDownloadRetries;
	private long mLatestFileDownloadId;

	private SharedPreferences mSharedPreferences;
	
	final static long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
	    // remove the logs
	    clearDataLogs();
	    
		mSharedPreferences = getApplicationContext().getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

		mLatestFileDownloadId = mSharedPreferences.getLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, 0);
		
	    if(hasInternetConnection() ){
			
	        setupDownloadManager();
	
			// remove the old file if its still there for some reason
            removeLatestFileDownload(getApplicationContext());
            
			// start the download of the latest file
			startDownloadLatest();
		}
		
		return Service.START_NOT_STICKY;
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
		if(hasConnection()){
			Resources resources = getApplicationContext().getResources();
	        String downloadLink = getConfigDownloadLink(resources);
			// set the download for the latest version on the download manager
			Request request = createDownloadRequest(downloadLink, resources.getString(R.string.versionFilename) + resources.getString(R.string.versionFilename_zip));
			mLatestFileDownloadId = mDownloadManager.enqueue(request);
			
			Editor editor = mSharedPreferences.edit();
	        editor.putLong(PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, mLatestFileDownloadId);
	        editor.commit();
		}
	}

	private String getConfigDownloadLink(Resources resources) {
		StringBuilder sb = new StringBuilder();
		sb.append(resources.getString(R.string.downloadUrl));
		sb.append(Build.MODEL);
		sb.append("/");
		sb.append(resources.getString(R.string.versionFilename));
		sb.append(resources.getString(R.string.versionFilename_zip));
		String downloadLink = sb.toString();
		return downloadLink;
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

		Intent resultIntent = new Intent(context, FairphoneUpdater.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

		stackBuilder.addParentStack(FairphoneUpdater.class);

		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(resultPendingIntent);

		Notification notificationWhileRunnig = builder.build();
		
		// Add notification
		manager.notify(0, notificationWhileRunnig);
		
		//to update the activity
		Intent updateIntent = new Intent(FairphoneUpdater.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
        context.sendBroadcast(updateIntent);
	}

	private Request createDownloadRequest(String url, String fileName) {

	    
		Request request = new Request(Uri.parse(url));
		Environment.getExternalStoragePublicDirectory(
				Environment.getExternalStorageDirectory()
						+ VersionParserHelper.UPDATER_FOLDER).mkdirs();

		request.setDestinationInExternalPublicDir(
				VersionParserHelper.UPDATER_FOLDER, fileName);
		request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setAllowedOverRoaming(false);
		
		Resources resources = getApplicationContext().getResources();
		request.setTitle(resources.getString(R.string.downloadUpdateTitle));

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
		mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

		mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();

		getApplicationContext().registerReceiver(mDownloadBroadCastReceiver,
				new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	private void removeBroadcastReceiver() {
		getApplicationContext().unregisterReceiver(mDownloadBroadCastReceiver);
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
		}
	}
	
    public static boolean readUpdaterData(Context context) {

        boolean retVal = false;
        Resources resources = context.getApplicationContext().getResources();
        String targetPath = Environment.getExternalStorageDirectory()
                + VersionParserHelper.UPDATER_FOLDER;

        String filePath = targetPath + resources.getString(R.string.versionFilename)
                + resources.getString(R.string.versionFilename_zip);

        File file = new File(filePath);

        if (file.exists() && RSAUtils.checkFileSignature(context, filePath, targetPath)) {
            checkVersionValidation(context);
            retVal = true;
        }

        return retVal;
    }

    private void removeLatestFileDownload(Context context) {
        if(mLatestFileDownloadId != 0){
        	mDownloadManager.remove(mLatestFileDownloadId);
        	mLatestFileDownloadId = 0;
        }
        VersionParserHelper.removeFiles(context);
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

				if (status == DownloadManager.STATUS_SUCCESSFUL) {
				    
				    String filePath = mDownloadManager.getUriForDownloadedFile(
				            mLatestFileDownloadId).getPath();
				    
				    String targetPath = Environment.getExternalStorageDirectory()
		                    + VersionParserHelper.UPDATER_FOLDER;
                    
				    if(RSAUtils.checkFileSignature(context, filePath, targetPath)){
    					checkVersionValidation(context);
					}else{
					    //invalid file
					    removeLatestFileDownload(context);
					    if(mDownloadRetries < MAX_DOWNLOAD_RETRIES){
    					    startDownloadLatest();
    				        mDownloadRetries++;
    					    removeReceiver = false;
					    }
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