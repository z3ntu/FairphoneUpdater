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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.fairphone.updater.R;
import com.fairphone.updater.UpdaterService;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Utils
{

    private static final String TAG = Utils.class.getSimpleName();

    private static double getPartitionSizeInGBytes(File path)
    {
        double availableBlocks = getPartitionSizeInBytes(path);
        double sizeInGB = ((availableBlocks / 1024d) / 1024d) / 1024d;
        Log.d(TAG, path.getPath() + " size(GB): " + sizeInGB);
        return sizeInGB;
    }

    public static double getPartitionSizeInMBytes(File path)
    {
        double availableBlocks = getPartitionSizeInBytes(path);
        double sizeInMB = ((availableBlocks / 1024d)) / 1024d;
        Log.d(TAG, path.getPath() + " size(MB): " + sizeInMB);
        return sizeInMB;
    }

    private static long getPartitionSizeInBytes(File path)
    {
        android.os.StatFs stat = new android.os.StatFs(path.getPath());
	    long blockSize, blockCount;
	    if (Build.VERSION.SDK_INT >= 18) {
		    blockSize = stat.getBlockSizeLong();
		    blockCount = stat.getBlockCountLong();
	    } else {
		    // deprectation warnings disabled due to the need to support SDK 17 (FP1)
		    //noinspection deprecation
		    blockSize = stat.getBlockSize();
		    //noinspection deprecation
		    blockCount = stat.getBlockCount();
	    }
	    return blockCount * blockSize;
    }

    public static long getAvailablePartitionSizeInBytes(File path)
    {
        android.os.StatFs stat = new android.os.StatFs(path.getPath());

	    long blockSize, blockCount;
	    if (Build.VERSION.SDK_INT >= 18) {
		    blockSize = stat.getBlockSizeLong();
		    blockCount = stat.getAvailableBlocksLong();
	    } else {
		    // deprectation warnings disabled due to the need to support SDK 17 (FP1)
		    //noinspection deprecation
		    blockSize = stat.getBlockSize();
		    //noinspection deprecation
		    blockCount = stat.getAvailableBlocks();
	    }
	    return blockCount * blockSize;
    }

    public static void startUpdaterService(Context context, boolean forceDownload)
    {
        boolean isRunning = isServiceRunning(context);

        if (!isRunning)
        {
            Log.e(TAG, "Starting Updater Service...");
            Intent i = new Intent(context, UpdaterService.class);
            context.startService(i);
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (forceDownload)
        {
            downloadConfigFile(context);
        }
    }

    private static boolean isServiceRunning(Context context)
    {
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (UpdaterService.class.getName().equals(service.service.getClassName()))
            {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    public static void stopUpdaterService(Context context)
    {
        boolean isRunning = isServiceRunning(context);

        if (isRunning)
        {
            Log.e(TAG, "Stoping Updater Service...");
            Intent i = new Intent(context, UpdaterService.class);
            context.stopService(i);
            try
            {
                Thread.sleep(200);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private static void downloadConfigFile(Context context) {
        downloadConfigFile(context, false);
    }

    public static void downloadConfigFile(Context context, boolean forceDownload)
    {
        Intent i = new Intent(UpdaterService.ACTION_FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD);
        i.putExtra(UpdaterService.EXTRA_FORCE_CONFIG_FILE_DOWNLOAD, forceDownload);
        context.sendBroadcast(i);
    }

    // **************************************************************************************************************
    // HELPERS
    // **************************************************************************************************************

    public static boolean checkMD5(String md5, File updateFile)
    {

        if (updateFile == null || !updateFile.exists())
        {
            return false;
        }

        if (md5 == null || md5.equals("") )
        {
            Log.e(TAG, "MD5 String NULL or UpdateFile NULL");
            return false;
        }

        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null)
        {
            Log.e(TAG, "calculatedDigest NULL");
            return false;
        }

        return calculatedDigest.equalsIgnoreCase(md5);
    }

    private static String calculateMD5(File updateFile)
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "Exception while getting Digest", e);
            return null;
        }

        InputStream is;
        try
        {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e)
        {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try
        {
            while ((read = is.read(buffer)) > 0)
            {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e)
        {
            Log.e(TAG, "Error digesting MD5", e);
            return null;
//            throw new RuntimeException("Unable to process file for MD5", e);
        } finally
        {
            try
            {
                is.close();
            } catch (IOException e)
            {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    public static String getModelAndOS(Context context)
    {
        StringBuilder sb = new StringBuilder();

        // attach the model and the os
        sb.append("?");
        sb.append("model=").append(Build.MODEL.replaceAll("\\s", ""));
        Version currentVersion = VersionParserHelper.getDeviceVersion(context);

        if (currentVersion != null)
        {
            sb.append("&");
            sb.append("os=").append(currentVersion.getAndroidVersion());
        }

        return sb.toString();
    }

    public static void clearCache()
    {
        File f = Environment.getDownloadCacheDirectory();
        File files[] = f.listFiles();
        if (files != null)
        {
            Log.d(TAG, "Size: " + files.length);
	        for (File file : files) {
		        String filename = file.getName();

		        if (filename.endsWith(".zip")) {
			        final boolean delete = file.delete();
			        if (delete) {
				        Log.d(TAG, "Deleted file " + filename);
			        } else {
				        Log.d(TAG, "Failed to delete file " + filename);
			        }
		        }
	        }
        }
    }

    public static boolean hasUnifiedPartition(Resources resources)
    {
        File path = Environment.getDataDirectory();
        double sizeInGB = Utils.getPartitionSizeInGBytes(path);
        double roundedSize = Math.ceil(sizeInGB * 100d) / 100d;
        Log.d(TAG, "/data size: " + roundedSize + "Gb");

        double fp1DataPartitionSize = (double) resources.getInteger(R.integer.FP1DataPartitionSizeMb) / 100d;
        // Add a little buffer to the 1gb default just in case
        return roundedSize > fp1DataPartitionSize;
    }

    public static String getPartitionDownloadPath(Resources resources)
    {
        String downloadPath = "";
        if (Build.MODEL.equals(resources.getString(R.string.FP1Model)))
        {
            downloadPath =
                    Utils.hasUnifiedPartition(resources) ? resources.getString(R.string.unifiedDataPartition) : resources
                            .getString(R.string.oneGBDataPartition);
        }
        return downloadPath;
    }

    public static boolean canCopyToCache(File file)
    {
        double fileSize = file.length();
        double cacheSize = Utils.getPartitionSizeInBytes(Environment.getDownloadCacheDirectory());
        return cacheSize >= fileSize;
    }
    public static String getFilenameFromDownloadableItem(DownloadableItem item)
    {
        StringBuilder filename = new StringBuilder();
        filename.append("fp_");
        if (item != null)
        {
            if (item instanceof Version)
            {
                filename.append("update_");
            }
            else if (item instanceof Store)
            {
                filename.append("store_");
            }
            filename.append(item.getNumber());
        }
        filename.append(".zip");
        return filename.toString();
    }
    
    public static String getDownloadTitleFromDownloadableItem(Resources resources, DownloadableItem item){
        String title = "";
        if (item != null)
        {
            if (item instanceof Version)
            {
                Version version = (Version) item;
                title = version.getName() + " " + version.getImageTypeDescription(resources);
            }
            else if (item instanceof Store)
            {
                Store store = (Store) item;
                title = store.getName();
            }
        }
        return title;
    }

    public static boolean isDeviceUnsupported(Context context)
    {
        Version deviceVersion = VersionParserHelper.getDeviceVersion(context);
        return deviceVersion == null || TextUtils.isEmpty(deviceVersion.getName());
    }
    
    public static String getprop(String name, String defaultValue)
    {
        ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop", name);
        pb.redirectErrorStream(true);

        Process p;
        InputStream is = null;
        try
        {
            p = pb.start();
            is = p.getInputStream();
            Scanner scan = new Scanner(is);
            scan.useDelimiter("\n");
            String prop = scan.next();
            if (prop.length() == 0)
            {
                return defaultValue;
            }
            return prop;
        } catch (NoSuchElementException e)
        {
            return defaultValue;
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                } catch (Exception e)
                {
	                Log.d(TAG, "Unexpected issue: "+e.getLocalizedMessage() );
                }
            }
        }
        return defaultValue;
    }
    
    public static void setprop(String key, String value)
    {
        if(RootTools.isAccessGiven()) {
            CommandCapture command = new CommandCapture(0, "setprop "+key+" "+value);
            try {
                Shell.runRootCommand(command);
            } catch (IOException | TimeoutException | RootDeniedException e) {
	            Log.d(TAG, "Failed to setprop: " + e.getLocalizedMessage());
            }
        }
    }
    
    public static void printStack(String moreLogs)
    {
        StringBuilder sb = new StringBuilder(moreLogs);
        sb.append("\nStack --> ");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) 
        {
            sb.append(ste.getFileName()).append(" : ").append(ste.getMethodName()).append(":").append(ste.getLineNumber()).append(" -- ");
        }
        Log.wtf(TAG, sb.toString());
    }
}
