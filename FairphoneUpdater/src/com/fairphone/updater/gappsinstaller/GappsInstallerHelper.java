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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.updater.R;
import com.fairphone.updater.tools.RSAUtils;
import com.fairphone.updater.tools.Utils;
import com.fairphone.updater.widgets.gapps.GoogleAppsInstallerWidget;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class GappsInstallerHelper {

    private static final String GOOGLE_APPS_DOWNLOAD_ID = "com.fairphone.updater.gapps.DOWNLOAD_ID";

	public static final String GAPPS_ACTION_DISCLAIMER = "com.fairphone.updater.gapps.DISCLAIMER";
	public static final String GAPPS_ACTION_DOWNLOAD_CONFIGURATION_FILE = "com.fairphone.updater.gapps.START_DONWLOAD_CONFIGURATION";
	public static final String GOOGLE_APPS_INSTALL_DOWNLOAD_CANCEL = "com.fairphone.updater.gapps.START_DOWNLOAD_CANCEL";
	public static final String GOOGLE_APPS_INSTALL_REBOOT = "com.fairphone.updater.gapps.REBOOT";

	public static final String PREFS_GOOGLE_APPS_INSTALLER_DATA = "FAIRPHONE_GOOGLE_APPS_INSTALLER_DATA";
	public static final String GOOGLE_APPS_INSTALLER_STATE = "com.fairphone.updater.gapps.WIDGET_STATE";
	public static final String GOOGLE_APPS_INSTALLER_PROGRESS = "com.fairphone.updater.gapps.WIDGET_SEEKBAR_PROGRESS";
	public static final String GOOGLE_APPS_INSTALLER_PROGRESS_MAX = "com.fairphone.updater.gapps.WIDGET_SEEKBAR_PROGRESS_MAX";
	public static final String GAPPS_ACTION_GO_PERMISSIONS = "com.fairphone.updater.gapps.GAPPS_ACTION_GO_PERMISSIONS";
	public static final String GAPPS_REINSTALATION = "com.fairphone.updater.gapps.GAPPS_REINSTALATION_REQUEST";
    public static final String GAPPS_REINSTALL_FLAG = "com.fairphone.updater.gapps.GAPPS_REINSTALL_FLAG";

	public static final int GAPPS_STATES_INITIAL = 0;
	public static final int GAPPS_STATES_DOWNLOAD_CONFIGURATION_FILE = 1;
	public static final int GAPPS_STATES_DOWNLOAD_GOOGLE_APPS_FILE = 2;
	public static final int GAPPS_STATES_EXTRACT_FILES = 3;
	public static final int GAPPS_STATES_PERMISSION_CHECK = 4;
	public static final int GAPPS_STATE_INSTALLATION = 5;

	public static final int GAPPS_REBOOT_STATE = 6;
	public static final int GAPPS_INSTALLED_STATE = 7;
	public static final int GAPPS_INSTALLATION_FAILED_STATE = 8;
	public static final int GAPPS_DOWNLOAD_FAILED_STATE = 9;
	protected static final String TAG = GappsInstallerHelper.class
			.getSimpleName();

	private static String DOWNLOAD_PATH = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			.getAbsolutePath();

	private static String RECOVERY_CACHE_PATH = "cache/";
    private static String RECOVERY_SDCARD_PATH = "sdcard/Download/";


	private static String ZIP_CONTENT_PATH = "/googleapps/";

	private DownloadManager mDownloadManager;
	private Context mContext;
	private SharedPreferences mSharedPrefs;

	private DownloadBroadCastReceiver mDownloadBroacastReceiver;
	private long mConfigFileDownloadId;
	private long mGappsFileDownloadId;
	private String mMD5hash;

	public GappsInstallerHelper(Context context) {
		mContext = context;

		mSharedPrefs = mContext.getSharedPreferences(
				PREFS_GOOGLE_APPS_INSTALLER_DATA, Context.MODE_PRIVATE);

		resume();

		int currentState = getInstallerState();

		if (currentState == GAPPS_REBOOT_STATE) {
			updateWidgetState(GAPPS_STATES_PERMISSION_CHECK);
		}

		if (currentState != GAPPS_STATE_INSTALLATION &&
			currentState != GAPPS_INSTALLED_STATE) {

			// clean files that must be rechecked
			forceCleanUnzipDirectory();
			forceCleanConfigurationFile();

			updateInstallerState(GAPPS_STATES_INITIAL);
		}
		
		if(!checkGappsAreInstalled()){
		    //updateInstallerState(GAPPS_STATES_INITIAL);
		    updateWidgetState(GAPPS_STATES_INITIAL);
		}
	}

	private boolean checkGappsAreInstalled() {

	    //boolean retVal = false;
	    
	    if (mSharedPrefs.getBoolean(GAPPS_REINSTALL_FLAG, false)) {
	        showReinstallAlert();
			forceCleanGappsZipFile();
        }
        
		File f = new File("/system/app/OneTimeInitializer.apk");

		if (f.exists()) {
		    updateWidgetState(GAPPS_INSTALLED_STATE);
		    forceCleanGappsZipFile();
	        return true;
		}
		
	    updateWidgetState(GAPPS_STATES_INITIAL);
		return false;
	}
	
	public void setReinstallAlertFlag() {

		SharedPreferences.Editor editor = mSharedPrefs.edit();
		editor.putBoolean(GAPPS_REINSTALL_FLAG, false);
		editor.commit();
	}

	public void showReinstallAlert() {
		showDialogOnTransparentActivity(TransparentActivity.SHOW_GAPPS_REINSTALL_DIALOG);	
	}

	private void showDialogOnTransparentActivity(String dialogToShow) {
		Intent i = new Intent(mContext, TransparentActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(dialogToShow);
		
		mContext.startActivity(i);
	}

	public void resume() {
		// setup the download manager
		setupDownloadManager();

		// setup the states broadcasts receivers
		setupTheStatesBroadCasts();
	}

	public void pause() {
		// clear the download manager
		clearDownloadManager();

		// clean the broadcast receivers
		clearBroadcastReceivers();
	}

	private int getCurrentState() {
		return mSharedPrefs.getInt(GOOGLE_APPS_INSTALLER_STATE,
				GAPPS_STATES_INITIAL);
	}

	private void setupDownloadManager() {
		mDownloadManager = (DownloadManager) mContext
				.getSystemService(Context.DOWNLOAD_SERVICE);

		mDownloadBroacastReceiver = new DownloadBroadCastReceiver();

		mContext.registerReceiver(mDownloadBroacastReceiver, new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	private void clearDownloadManager() {
		mContext.unregisterReceiver(mDownloadBroacastReceiver);

		mDownloadBroacastReceiver = null;
	}

	private boolean isWiFiEnabled() {

		ConnectivityManager manager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();

		return isWifi;
	}

	private boolean hasAlreadyDownloadedZipFile(String mMD5hash) {
		String filename = mContext.getResources().getString(
				R.string.gapps_installer_filename);
		File file = new File(DOWNLOAD_PATH + "/" + filename);
		return GappsInstallerHelper.checkMD5(mMD5hash, file);
	}

	private String[] getGappsUrlFromConfigFile(String filePath) {

		String[] result = new String[2];

		File configFile = new File(filePath);

		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));

			result[0] = br.readLine();
			result[1] = br.readLine();

			br.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Configuration file not find", e);
			result = null;
		} catch (IOException e) {
			Log.e(TAG, "Configuration file could not be read", e);
			result = null;
		}

		return result;
	}

	private void deleteFile(String file, String location) {
		File f = new File(location + file);

		if (f.exists()) {
			deleteRecursive(f);
		}
	}

	private void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			for (File child : fileOrDirectory.listFiles()) {
				deleteRecursive(child);
			}
		}

		fileOrDirectory.delete();
	}

	private void forceCleanConfigurationFile() {

		if (mConfigFileDownloadId != 0) {
			mDownloadManager.remove(mConfigFileDownloadId);
		}

		String configFileName = mContext.getResources().getString(
				R.string.gapps_installer_config_file);
		String configFileZip = mContext.getResources().getString(
                R.string.gapps_installer_zip);
		String configFileCfg = mContext.getResources().getString(
                R.string.gapps_installer_cfg);
		String configFileSig = mContext.getResources().getString(
                R.string.gapps_installer_sig);

		deleteFile("/" + configFileName + configFileZip, DOWNLOAD_PATH);
		deleteFile("/" + configFileName + configFileCfg, DOWNLOAD_PATH);
		deleteFile("/" + configFileName + configFileSig, DOWNLOAD_PATH);
	}

	private void forceCleanGappsZipFile() {

		long downloadID = mSharedPrefs.getLong(GOOGLE_APPS_DOWNLOAD_ID, 0);

		if (downloadID != 0) {
			mDownloadManager.remove(downloadID);
			
			SharedPreferences.Editor prefEdit = mSharedPrefs.edit();
			// Save the download id
			prefEdit.putLong(GOOGLE_APPS_DOWNLOAD_ID, 0);
			prefEdit.putInt(GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS, 0);
			prefEdit.putInt(GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS_MAX, 0);

			prefEdit.commit();
		}

		String gappsFileName = mContext.getResources().getString(
				R.string.gapps_installer_filename);

		deleteFile("/" + gappsFileName, DOWNLOAD_PATH);
	}

	private void forceCleanUnzipDirectory() {
		deleteFile(ZIP_CONTENT_PATH, DOWNLOAD_PATH);
	}

	private BroadcastReceiver mBCastDisclaimer;
	private BroadcastReceiver mBCastDownloadConfiguration;
	private BroadcastReceiver mBCastInstallDownloadCancel;
	private BroadcastReceiver mBCastGoPermissions;
	private BroadcastReceiver mBCastGappsInstallReboot;
	private BroadcastReceiver mBCastReinstallGapps;
	private BroadcastReceiver mBCastSetReinstallGappsFlag;
	private BroadcastReceiver mBCastChangeGappsStateToInitial;

	private void setupTheStatesBroadCasts() {
		// launching the application

		mBCastDisclaimer = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				showDisclaimer();
			}
		};

		mContext.registerReceiver(mBCastDisclaimer, new IntentFilter(
				GAPPS_ACTION_DISCLAIMER));

		mBCastDownloadConfiguration = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// clean the configuration files
				forceCleanConfigurationFile();

				if (isWiFiEnabled()) {
					Resources resources = mContext.getResources();

					String configFileName =resources.getString(
							R.string.gapps_installer_config_file);
					String configFileZip = resources.getString(
			                R.string.gapps_installer_zip);

					Request request = createDownloadRequest(getConfigDownloadLink(resources), configFileName + configFileZip);
					mConfigFileDownloadId = mDownloadManager.enqueue(request);

					updateWidgetState(GAPPS_STATES_DOWNLOAD_CONFIGURATION_FILE);
				} else {
					showWifiWarning();
				}
			}
		};

		mContext.registerReceiver(mBCastDownloadConfiguration,
				new IntentFilter(GAPPS_ACTION_DOWNLOAD_CONFIGURATION_FILE));

		mBCastInstallDownloadCancel = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				updateWidgetState(GAPPS_STATES_INITIAL);
			}

		};

		mContext.registerReceiver(mBCastInstallDownloadCancel,
				new IntentFilter(GOOGLE_APPS_INSTALL_DOWNLOAD_CANCEL));

		mBCastGoPermissions = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				String filename = mContext.getResources().getString(
						R.string.gapps_installer_filename);

				pushFileToRecovery(filename);
			}
		};

		mContext.registerReceiver(mBCastGoPermissions, new IntentFilter(
				GAPPS_ACTION_GO_PERMISSIONS));

		mBCastGappsInstallReboot = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
//				String filename = mContext.getResources().getString(
//						R.string.gapps_installer_filename);
//
//				pushFileToRecovery(filename);
				
				// alter State
				updateWidgetState(GAPPS_INSTALLED_STATE);
				
				// reboot
				rebootToRecovery();
			}
		};

		mContext.registerReceiver(mBCastGappsInstallReboot, new IntentFilter(
				GOOGLE_APPS_INSTALL_REBOOT));

		mBCastReinstallGapps = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                SharedPreferences.Editor editor = mSharedPrefs.edit();
                if(checkGappsAreInstalled()){
                    editor.putBoolean(GAPPS_REINSTALL_FLAG, true);
                } else {
                    editor.putBoolean(GAPPS_REINSTALL_FLAG, false);
                }
                editor.commit();
            }
        };

        mContext.registerReceiver(mBCastReinstallGapps, new IntentFilter(
                GAPPS_REINSTALATION));
        
        mBCastSetReinstallGappsFlag = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                setReinstallAlertFlag();
            }
        };

        mContext.registerReceiver(mBCastSetReinstallGappsFlag, new IntentFilter(
                TransparentActivity.ACTION_SET_GAPPS_REINSTALL_FLAG));
        
        mBCastChangeGappsStateToInitial = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
            	updateWidgetState(GAPPS_STATES_INITIAL);
            }
        };

        mContext.registerReceiver(mBCastChangeGappsStateToInitial, new IntentFilter(
                TransparentActivity.ACTION_CHANGE_GAPPS_STATE_TO_INITIAL));
	}
	
	private String getConfigDownloadLink(Resources resources) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(resources.getString(R.string.gapps_installer_download_url));
		sb.append(Build.MODEL);
		sb.append(getPartitionDownloadPath(resources));
		sb.append("/");
		sb.append(resources.getString(R.string.gapps_installer_config_file));
		sb.append(resources.getString(R.string.gapps_installer_zip));
		String downloadLink = sb.toString();
		Log.d(TAG, "Download link: " + downloadLink);
		return downloadLink;
	}

	private String getPartitionDownloadPath(Resources resources) {
		String downloadPath = "";
		if (Build.MODEL.equals(resources.getString(R.string.FP1Model))) {
			File path = Environment.getDataDirectory();
			double sizeInGB = Utils.getPartitionSizeInGBytes(path);
			double roundedSize = (double) Math.ceil(sizeInGB * 100d) / 100d;
			Log.d(TAG, "/data size: " + roundedSize + "Gb");
			
			double fp1DataPartitionSize = (double)resources.getInteger(R.integer.FP1DataPartitionSizeMb) / 100d;
			//Add a little buffer to the 1gb default just in case
			downloadPath = roundedSize <= fp1DataPartitionSize ? resources
					.getString(R.string.oneGBDataPartition) : resources
					.getString(R.string.unifiedDataPartition);
		}
		return downloadPath;
	}

	public void pushFileToRecovery(String fileName) {
		if (RootTools.isAccessGiven()) {
			// set the command for the recovery
			
			copyGappsToCache();
			
			try {
			     Shell.runRootCommand(new CommandCapture(0,
			            "rm -f /cache/recovery/command"));
			    
	             Shell.runRootCommand(new CommandCapture(0,
	                        "rm -f /cache/recovery/extendedcommand"));
	             
	             Shell.runRootCommand(new CommandCapture(0,
	                     "echo '--wipe_cache' >> /cache/recovery/command"));
	             
	             Shell.runRootCommand(new CommandCapture(0,
	                     "echo '--update_package=/" + (canCopyToCache() ? RECOVERY_CACHE_PATH : RECOVERY_SDCARD_PATH)
	                        + fileName + "' >> /cache/recovery/command"));
	             
				
	             /*p = Runtime.getRuntime().exec("su");

				DataOutputStream os = new DataOutputStream(p.getOutputStream());
				os.writeBytes("rm -f /cache/recovery/command\n");
				os.writeBytes("rm -f /cache/recovery/extendedcommand\n");

				os.writeBytes("echo '--wipe_cache' >> /cache/recovery/command\n");

				os.writeBytes("echo '--update_package=/" + RECOVERY_PATH
						+ fileName + "' >> /cache/recovery/command\n");

				os.writeBytes("sync\n");
				os.writeBytes("exit\n");
				os.flush();
				p.waitFor();*/
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} catch (TimeoutException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RootDeniedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}

		updateWidgetState(GAPPS_REBOOT_STATE);
	}

	private void copyGappsToCache() {
		if (canCopyToCache()) {
			String filename = mContext.getResources().getString(
					R.string.gapps_installer_filename);
			File file = new File(DOWNLOAD_PATH + "/" + filename);
			CopyFileToCacheTask copyTask = new CopyFileToCacheTask();
			copyTask.execute(file.getPath(), Environment.getDownloadCacheDirectory() + "/" + filename);
		} else{
			Log.d(TAG, "No space on cache. Defaulting to Sdcard");
		}
	}
	
	public boolean canCopyToCache(){
    	Resources resources = mContext.getResources();
		double cacheSize = Utils.getPartitionSizeInMBytes(Environment.getDownloadCacheDirectory());
		return cacheSize > resources.getInteger(R.integer.FP1CachePartitionSizeMb) && 
				cacheSize > resources.getInteger(R.integer.minimalCachePartitionSizeMb);
    }
	
	private void clearCache() {
		File f = Environment.getDownloadCacheDirectory();        
		File files[] = f.listFiles();
		if(files !=null){
			Log.d(TAG, "Size: "+ files.length);
			for (int i=0; i < files.length; i++)
			{
			    String filename = files[i].getName();
			    
			    if(filename.endsWith(".zip")){
			    	files[i].delete();
		    	    Log.d(TAG, "Deleted file " + filename);
			    }
			}
		}
	}

	public void rebootToRecovery() {
		if (RootTools.isAccessGiven()) {
			updateWidgetState(GAPPS_INSTALLED_STATE);

			try
            {
                Shell.runRootCommand(new CommandCapture(0,
                        "reboot recovery"));
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RootDeniedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		} else {
			Resources resources = mContext.getResources();

			AlertDialog permissionsDialog = new AlertDialog.Builder(mContext)
					.create();

			permissionsDialog.setTitle(resources
					.getText(R.string.google_apps_denied_permissions_title));

			// Setting Dialog Message
			permissionsDialog
					.setMessage(resources
							.getText(R.string.google_apps_denied_permissions_description));

			permissionsDialog.setButton(AlertDialog.BUTTON_POSITIVE,
					resources.getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							forceCleanConfigurationFile();
							// forceCleanUnzipDirectory();

							updateWidgetState(GAPPS_STATES_INITIAL);

						}
					});

			permissionsDialog.show();
		}
	}

	private void clearBroadcastReceivers() {
		mContext.unregisterReceiver(mBCastDisclaimer);
		mContext.unregisterReceiver(mBCastDownloadConfiguration);
		mContext.unregisterReceiver(mBCastGoPermissions);
		mContext.unregisterReceiver(mBCastGappsInstallReboot);
		mContext.unregisterReceiver(mBCastInstallDownloadCancel);
		mContext.unregisterReceiver(mBCastReinstallGapps);
		mContext.unregisterReceiver(mBCastSetReinstallGappsFlag);
		mContext.unregisterReceiver(mBCastChangeGappsStateToInitial);
	}

	private void updateGoogleAppsIntallerWidgets() {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(mContext);
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(mContext,
						GoogleAppsInstallerWidget.class));
		if (appWidgetIds.length > 0) {
			new GoogleAppsInstallerWidget().onUpdate(mContext,
					appWidgetManager, appWidgetIds);
		}
	}

	private Request createDownloadRequest(String url, String fileName) {

		Request request = new Request(Uri.parse(url));
		Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).mkdirs();

		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS, fileName);
		request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setAllowedOverRoaming(false);

		String download = mContext.getResources().getString(
				R.string.google_apps_download_title);
		request.setTitle(download);

		return request;
	}

	private void startDownloadProgressUpdateThread(final long download_id) {
		new Thread(new Runnable() {

			@Override
			public void run() {

				boolean downloading = true;

				while (downloading) {

					DownloadManager.Query q = new DownloadManager.Query();
					q.setFilterById(download_id);

					Cursor cursor = mDownloadManager.query(q);
					if (cursor != null) {
						cursor.moveToFirst();
						try {
							int bytes_downloaded = cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
							int bytes_total = cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
	
							if (cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
								downloading = false;
	
								bytes_downloaded = 0;
								bytes_total = 0;
							}
	
							SharedPreferences.Editor prefEdit = mSharedPrefs.edit();
							prefEdit.putInt(
									GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS,
									bytes_downloaded);
							prefEdit.putInt(
									GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS_MAX,
									bytes_total);
							prefEdit.commit();
	
							updateGoogleAppsIntallerWidgets();
						}catch(Exception e){
							downloading = false;
							updateWidgetState(GAPPS_STATES_INITIAL);
							mDownloadManager.remove(download_id);
							SharedPreferences.Editor prefEdit = mSharedPrefs.edit();
							prefEdit.putInt(
									GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS,
									0);
							prefEdit.putInt(
									GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS_MAX,
									0);
							prefEdit.commit();
							Log.e(TAG, "Error updating Gapps download progress: " + e.getMessage());
						}

						cursor.close();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	private void updateInstallerState(int state) {
		// alter State
		SharedPreferences.Editor prefEdit = mSharedPrefs.edit();
		prefEdit.putInt(GOOGLE_APPS_INSTALLER_STATE, state);
		prefEdit.commit();
	}

	private int getInstallerState() {
		return mSharedPrefs.getInt(GOOGLE_APPS_INSTALLER_STATE,
				GAPPS_STATES_INITIAL);
	}

	private void updateWidgetState(int state) {
		updateInstallerState(state);

		updateGoogleAppsIntallerWidgets();
	}

	public static boolean checkMD5(String md5, File updateFile) {

		if (!updateFile.exists()) {
			return false;
		}

		if (md5 == null || md5.equals("") || updateFile == null) {
			Log.e(TAG, "MD5 String NULL or UpdateFile NULL");
			return false;
		}

		String calculatedDigest = calculateMD5(updateFile);
		if (calculatedDigest == null) {
			Log.e(TAG, "calculatedDigest NULL");
			return false;
		}

		Log.i(TAG, "Calculated digest: " + calculatedDigest);
		Log.i(TAG, "Provided digest: " + md5);

		return calculatedDigest.equalsIgnoreCase(md5);
	}

	public static String calculateMD5(File updateFile) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Exception while getting Digest", e);
			return null;
		}

		InputStream is;
		try {
			is = new FileInputStream(updateFile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Exception while getting FileInputStream", e);
			return null;
		}

		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			return output;
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.e(TAG, "Exception on closing MD5 input stream", e);
			}
		}
	}

	private class DownloadBroadCastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			DownloadManager.Query query = new DownloadManager.Query();

			long downloadID = 0;

			switch (getCurrentState()) {
			case GAPPS_STATES_DOWNLOAD_CONFIGURATION_FILE:
				downloadID = mConfigFileDownloadId;
				break;
			default:
				downloadID = mSharedPrefs.getLong(GOOGLE_APPS_DOWNLOAD_ID, 0);
			}
			
			if(downloadID != 0) {
				Cursor cursor = mDownloadManager.query(query);
				query.setFilterById(downloadID);
	
				if (cursor.moveToFirst()) {
					int columnIndex = cursor
							.getColumnIndex(DownloadManager.COLUMN_STATUS);
					int status = cursor.getInt(columnIndex);
					int columnReason = cursor
							.getColumnIndex(DownloadManager.COLUMN_REASON);
					int reason = cursor.getInt(columnReason);
	
					if (status == DownloadManager.STATUS_SUCCESSFUL) {
						// Retrieve the saved download id
						if (downloadID == mConfigFileDownloadId) {
						    
						    String targetPath = DOWNLOAD_PATH + ZIP_CONTENT_PATH;
						    String cfgFilename = mContext.getResources().getString(R.string.gapps_installer_config_file);
	                        String fileCfgExt = mContext.getResources().getString(R.string.gapps_installer_cfg);
						    
	                        String cfgFile = targetPath + cfgFilename + fileCfgExt;
	                        
	                        // file to where the download happened
	    					Uri filePath = mDownloadManager.getUriForDownloadedFile(
	    							downloadID);
	                        if(filePath!=null && !checkFileSignature(filePath.getPath(), targetPath)){
	                            Toast.makeText(mContext,
	                                    R.string.google_apps_download_error,
	                                    Toast.LENGTH_LONG).show();
	
	                            updateWidgetState(GAPPS_STATES_INITIAL);
	                            if(mConfigFileDownloadId!=0){
	                            	mDownloadManager.remove(mConfigFileDownloadId);
	                            }
	                            cursor.close();
	                            return;
	                        }
	
							// read the gapps url
							String[] downloadData = getGappsUrlFromConfigFile(cfgFile);
	
							if (downloadData == null) {
								Toast.makeText(mContext,
										R.string.google_apps_download_error,
										Toast.LENGTH_LONG).show();
	
								updateWidgetState(GAPPS_STATES_INITIAL);
								if(mConfigFileDownloadId!=0){
	                            	mDownloadManager.remove(mConfigFileDownloadId);
	                            }
								cursor.close();
								return;
							}
	                        
	
							// read the md5
							mMD5hash = downloadData[1];
	
							String filename = mContext.getResources().getString(
									R.string.gapps_installer_filename);
	
							if (hasAlreadyDownloadedZipFile(mMD5hash)) {
								updateWidgetState(GAPPS_STATES_PERMISSION_CHECK);
	//							updateWidgetState(GAPPS_REBOOT_STATE);
							} else {
								Log.d(TAG, "GAPPS> file does not match");
	
								forceCleanGappsZipFile();
	
								// enqueue of gapps request
								Request request = createDownloadRequest(
										downloadData[0], filename);
	
								mGappsFileDownloadId = mDownloadManager
										.enqueue(request);
	
								SharedPreferences.Editor prefEdit = mSharedPrefs
										.edit();
								// Save the download id
								prefEdit.putLong(GOOGLE_APPS_DOWNLOAD_ID,
										mGappsFileDownloadId);
	
								startDownloadProgressUpdateThread(mGappsFileDownloadId);
	
								prefEdit.putInt(
										GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS,
										0);
								prefEdit.putInt(
										GappsInstallerHelper.GOOGLE_APPS_INSTALLER_PROGRESS_MAX,
										0);
	
								prefEdit.commit();
	
								// alter Widget State
								updateGoogleAppsIntallerWidgets();
								updateWidgetState(GAPPS_STATES_DOWNLOAD_GOOGLE_APPS_FILE);
							}
						} else if(hasAlreadyDownloadedZipFile(mMD5hash)){
							updateWidgetState(GAPPS_STATES_PERMISSION_CHECK);
	//						updateWidgetState(GAPPS_REBOOT_STATE);
						}else{
							Toast.makeText(mContext,
									R.string.google_apps_download_error,
									Toast.LENGTH_LONG).show();
	
							updateWidgetState(GAPPS_STATES_INITIAL);
							if(mConfigFileDownloadId != 0){
	                        	mDownloadManager.remove(mConfigFileDownloadId);
	                        }
						}
					} else if (status == DownloadManager.STATUS_FAILED) {
						Toast.makeText(mContext,
								"FAILED!\n" + "reason of " + reason,
								Toast.LENGTH_LONG).show();
	
						forceCleanConfigurationFile();
	
						forceCleanUnzipDirectory();
	
						updateWidgetState(GAPPS_STATES_INITIAL);
					} 
				}
				cursor.close();
			}else {
				Log.d(TAG, "Download id id " + downloadID + ". Should be a Updater download not Gapps.");
				//Toast.makeText(mContext, "download ID is " + downloadID, Toast.LENGTH_LONG).show();
			}
		}
	}
	
    private boolean  checkFileSignature(String filePath, String targetPath){
        boolean valid = false;

        unzip(filePath, targetPath);
        
        try {
            String cfgFilename = mContext.getResources().getString(R.string.gapps_installer_config_file);
            String fileCfgExt = mContext.getResources().getString(R.string.gapps_installer_cfg);
            String fileSigExt = mContext.getResources().getString(R.string.gapps_installer_sig);
            
            PublicKey pubKey = RSAUtils.readPublicKeyFromPemFormat(mContext, R.raw.public_key);
            byte[] sign = RSAUtils.readSignature(targetPath + cfgFilename + fileSigExt);
            valid =  RSAUtils.verifySignature(targetPath + cfgFilename + fileCfgExt, RSAUtils.SIGNATURE_ALGORITHM, sign, pubKey);
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return valid;
    }
    
    public void unzip(String filePath, String targetPath) {
        new File(targetPath).mkdirs();
        try {
            FileInputStream fin = new FileInputStream(filePath);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;

            while ((ze = zin.getNextEntry()) != null) {
                Log.d(TAG, "Unzipping " + ze.getName());

                if (ze.isDirectory()) {
                    _dirChecker(ze.getName(), targetPath);
                } else {
                    FileOutputStream fout = new FileOutputStream(targetPath + ze.getName());
                    byte buffer[] = new byte[2048];

                    int count = 0;

                    while ((count = zin.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }

                    zin.closeEntry();
                    fout.close();
                }
            }
            zin.close();
            fin.close();
        } catch (Exception e) {
            Log.e("Decompress", "unzip", e);
        }
    }
    
    private void _dirChecker(String dir, String location) {
        File f = new File(location + dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
    
    private void showDisclaimer() {
    	showDialogOnTransparentActivity(TransparentActivity.SHOW_GAPPS_DISCLAIMER_DIALOG);
	}

	private void showWifiWarning() {
		showDialogOnTransparentActivity(TransparentActivity.SHOW_GAPPS_WIFI_WARNING_DIALOG);
	}

	private class CopyFileToCacheTask extends AsyncTask<String, Integer, Integer>{

        boolean isProgressShowing = false;
        @Override
        protected Integer doInBackground(String... params)
        {
            // check the correct number of params
            if(params.length != 2){
                return -1;
            }
            
            String originalFilePath = params[0];
            String destinyFilePath = params[1];
            
            if (RootTools.isAccessGiven())
            {
            	clearCache();
            	
                File otaFilePath = new File(originalFilePath);
                File otaFileCache = new File(destinyFilePath);
                
                if (!otaFileCache.exists())
                {
                    RootTools.copyFile(otaFilePath.getPath(), otaFileCache.getPath(), false, false);
                }
            }
            
            return 1;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            
        }
        
        protected void onPreExecute() {            
            if(!isProgressShowing){
            	showDialogOnTransparentActivity(TransparentActivity.SHOW_GAPPS_PROGRESS_SPINNER);
            	isProgressShowing = true;
            }
        }

		protected void onPostExecute(Integer result) {
			// disable the spinner
			if (isProgressShowing) {
				Intent i = new Intent();
				i.setAction(TransparentActivity.HIDE_GAPPS_PROGRESS_SPINNER);
				mContext.sendBroadcast(i);
				isProgressShowing = false;
			}
		}    
    }
}
