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

package com.fairphone.updater.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.fairphone.updater.FairphoneUpdater2Activity;
import com.fairphone.updater.FairphoneUpdater2Activity.UpdaterState;
import com.fairphone.updater.UpdaterService;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;

public class Utils {
	
	private static final String TAG = Utils.class.getSimpleName();

	public static double getPartitionSizeInGBytes(File path) {
		double availableBlocks = getPartitionSizeInBytes(path);
		double sizeInGB = (((double) availableBlocks / 1024d) / 1024d) / 1024d;
		Log.d(TAG, path.getPath() + " size(GB): " + sizeInGB);
		return sizeInGB;
	}

	public static double getPartitionSizeInMBytes(File path) {
		double availableBlocks = getPartitionSizeInBytes(path);
		double sizeInMB = (((double) availableBlocks / 1024d)) / 1024d;
		Log.d(TAG, path.getPath() + " size(MB): " + sizeInMB);
		return sizeInMB;
	}

	public static long getPartitionSizeInBytes(File path) {
		android.os.StatFs stat = new android.os.StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getBlockCount() * blockSize;
		return availableBlocks;
	}
	
	public static long getAvailablePartitionSizeInBytes(File path) {
		android.os.StatFs stat = new android.os.StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks() * blockSize;
		return availableBlocks;
	}
	
	public static boolean areGappsInstalling(Context context){
		SharedPreferences gappsSharedPrefs = context.getSharedPreferences(
				GappsInstallerHelper.PREFS_GOOGLE_APPS_INSTALLER_DATA, Context.MODE_PRIVATE);
		
		int currentState = gappsSharedPrefs.getInt(GappsInstallerHelper.GOOGLE_APPS_INSTALLER_STATE, GappsInstallerHelper.GAPPS_STATES_INITIAL);
		return currentState != GappsInstallerHelper.GAPPS_STATES_INITIAL && currentState != GappsInstallerHelper.GAPPS_INSTALLED_STATE;
	}
	
	public static boolean isUpdaterInstalling(Context context){		
		SharedPreferences updaterSharedPrefs = context.getSharedPreferences(
				FairphoneUpdater2Activity.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);
		
		String currentState = updaterSharedPrefs.getString(FairphoneUpdater2Activity.PREFERENCE_CURRENT_UPDATER_STATE, UpdaterState.NORMAL.name());
		UpdaterState state = UpdaterState.valueOf(currentState);
		return state != UpdaterState.NORMAL;
	}
	
	public static void startUpdaterService(Context context,
			boolean forceDownload) {
		boolean isRunning = isServiceRunning(context);

		if (!isRunning) {
			Log.e(TAG, "Starting Updater Service...");
			Intent i = new Intent(context, UpdaterService.class);
			context.startService(i);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (forceDownload) {
			downloadConfigFile(context);
		}
	}

	public static boolean isServiceRunning(Context context) {
		boolean isRunning = false;
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (UpdaterService.class.getName().equals(
					service.service.getClassName())) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}

	public static void stopUpdaterService(Context context) {
		boolean isRunning = isServiceRunning(context);

		if (isRunning) {
			Log.e(TAG, "Stoping Updater Service...");
			Intent i = new Intent(context, UpdaterService.class);
			context.stopService(i);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void downloadConfigFile(Context context) {
		Intent i = new Intent(
				UpdaterService.ACTION_FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD);
		context.sendBroadcast(i);
	}
	
	// **************************************************************************************************************
		// HELPERS
		// **************************************************************************************************************

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
}
