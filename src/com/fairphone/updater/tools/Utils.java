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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.updater.BetaEnabler;
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Utils
{
    private static final String TAG = Utils.class.getSimpleName();
    private static final int DELAY_100_MILLIS = 100;
    public static final int DELAY_HALF_SECOND = 500;
    public static final long SECONDS_IN_MINUTE = 60L;
    public static final long MINUTES_IN_HOUR = 60L;

    private static final double BUFFER_1024_BYTES = 1024d;
    // --Commented out by Inspection (06/02/2015 12:27):public static final int BUFFER_SIZE_4_KBYTES = 4096;
    public static final int BUFFER_SIZE_2_KBYTES = 2048;
    private static final int BUFFER_SIZE_8_KBYTES = 8192;
    public static final int BUFFER_SIZE_10_MBYTES = 10240;
    private static final int RADIX_BASE_16 = 16;
    private static final double PERCENT_100 = 100d;
    private static final char CHAR_SPACE = ' ';
    private static final char CHAR_ZERO = '0';

    private static double getPartitionSizeInGBytes(File path)
    {
        double availableBlocks = getPartitionSizeInBytes(path);
        double sizeInGB = ((availableBlocks / BUFFER_1024_BYTES) / BUFFER_1024_BYTES) / BUFFER_1024_BYTES;
        Log.d(TAG, path.getPath() + " size(GB): " + sizeInGB);
        return sizeInGB;
    }

// --Commented out by Inspection START (06/02/2015 12:26):
//    public static double getPartitionSizeInMBytes(File path)
//    {
//        double availableBlocks = getPartitionSizeInBytes(path);
//        double sizeInMB = ((availableBlocks / BUFFER_1024_BYTES)) / BUFFER_1024_BYTES;
//        return sizeInMB;
//    }
// --Commented out by Inspection STOP (06/02/2015 12:26)

    private static long getPartitionSizeInBytes(File path)
    {
        android.os.StatFs stat = new android.os.StatFs(path.getPath());
	    long blockSize, blockCount;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
        boolean isNotRunning = !isServiceRunning(context);

        if (isNotRunning)
        {
            Log.e(TAG, "Starting Updater Service...");
            Intent i = new Intent(context, UpdaterService.class);
            context.startService(i);
            try
            {
                Thread.sleep(DELAY_100_MILLIS);
            } catch (InterruptedException e)
            {
                Log.w(TAG, "Start Updater service delay error: " + e.getLocalizedMessage());
            }
        }
        else if (forceDownload)
        {
            downloadConfigFile(context, true);
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

// --Commented out by Inspection START (06/02/2015 12:26):
//    public static void stopUpdaterService(Context context)
//    {
//        boolean isRunning = isServiceRunning(context);
//
//        if (isRunning)
//        {
//            Log.i(TAG, "Stoping Updater Service...");
//            Intent i = new Intent(context, UpdaterService.class);
//            context.stopService(i);
//            try
//            {
//                Thread.sleep(DELAY_100_MILLIS * 2);
//            } catch (InterruptedException e)
//            {
//                Log.w(TAG, "Stop Updater service delay error: " + e.getLocalizedMessage());
//            }
//        }
//    }
// --Commented out by Inspection STOP (06/02/2015 12:26)

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

        if (md5 == null || md5.isEmpty())
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

        byte[] buffer = new byte[BUFFER_SIZE_8_KBYTES];
        int read;
        try
        {
            while ((read = is.read(buffer)) > 0)
            {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(RADIX_BASE_16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(CHAR_SPACE, CHAR_ZERO);
            return output;
        } catch (IOException e)
        {
            Log.e(TAG, "Error digesting MD5: " + e.getLocalizedMessage());
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

	public static void copy(File src, File dst) throws IOException {
		if (PrivilegeChecker.isPrivilegedApp()) {
			copyPrivileged(src, dst);
		} else {
			copyUnprivileged(src, dst);
		}
	}

	private static void copyUnprivileged(File src, File dst) throws IOException {
		if (RootTools.isAccessGiven()) {
			RootTools.copyFile(src.getPath(), dst.getPath(), false, false);
		} else {
			throw new IOException("No root permissions granted.");
		}
	}

	private static void copyPrivileged(File src, File dst) throws IOException {
		FileInputStream inStream = new FileInputStream(src);
		FileOutputStream outStream = new FileOutputStream(dst);
		FileChannel inChannel = inStream.getChannel();
		FileChannel outChannel = outStream.getChannel();
		inChannel.transferTo(0, inChannel.size(), outChannel);
		inStream.close();
		outStream.close();
    }

    public static void clearCache()
    {
        if(PrivilegeChecker.isPrivilegedApp()) {
            File f = Environment.getDownloadCacheDirectory();
            File[] files = f.listFiles();
            if (files != null) {
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
        } else {
            if(RootTools.isAccessGiven()) {
                try {
                    Shell.runRootCommand(new CommandCapture(0, "rm -f *.zip"));
                } catch (IOException | TimeoutException |RootDeniedException e) {
                    Log.w(TAG, "Failed to clear cache: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public static boolean hasUnifiedPartition(Resources resources)
    {
        File path = Environment.getDataDirectory();
        double sizeInGB = Utils.getPartitionSizeInGBytes(path);
        double roundedSize = Math.ceil(sizeInGB * PERCENT_100) / PERCENT_100;
        Log.d(TAG, "/data size: " + roundedSize + "Gb");

        double fp1DataPartitionSize = (double) resources.getInteger(R.integer.FP1DataPartitionSizeMb) / PERCENT_100;
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
        return fileSize > 0 && cacheSize >= fileSize;
    }

    public static String getFilenameFromDownloadableItem(DownloadableItem item, boolean isVersion)
    {
        StringBuilder filename;

        if(isVersion)
        {
            filename = getFilenameForItem(item, "update_");
        }
        else
        {
            filename = getFilenameForItem(item, "store_");
        }

        return filename.toString();
    }

    private static StringBuilder getFilenameForItem(DownloadableItem item, String type) {
        StringBuilder filename = new StringBuilder();
        filename.append("fp_");
        if (item != null)
        {
            filename.append(type);
            filename.append(item.getNumber());
        }
        filename.append(".zip");
        return filename;
    }
    
    public static String getDownloadTitleFromDownloadableItem(Resources resources, DownloadableItem item, boolean isVersion){
        String title = "";
        if (item != null)
        {
            if (isVersion)
            {
                Version version = (Version) item;
                title = version.getName() + " " + version.getImageTypeDescription(resources);
            }
            else
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
            if (prop.isEmpty())
            {
                return defaultValue;
            }
            return prop;
        } catch (NoSuchElementException e)
        {
            Log.w(TAG, "Error reading prop "+name+". Defaulting to " + defaultValue + ": " + e.getLocalizedMessage());
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

	public static void setBetaPropToEnable() {
		if (PrivilegeChecker.isPrivilegedApp()) {
			setBetaPropToEnablePrivileged();
		} else {
			setBetaPropToEnableUnprivileged();
		}
	}

	private static void setBetaPropToEnablePrivileged() {
	    ProcessBuilder pb = new ProcessBuilder("/system/bin/setprop", BetaEnabler.FAIRPHONE_BETA_PROPERTY, BetaEnabler.BETA_ENABLED);
	    try {
		    Process p = pb.start();
		    p.waitFor();
	    } catch (IOException | InterruptedException e) {
		    Log.d(TAG, "Failed to setprop: " + e.getLocalizedMessage());
	    }
	}

	private static void setBetaPropToEnableUnprivileged()
    {
        if(RootTools.isAccessGiven()) {
            CommandCapture command = new CommandCapture(0, "setprop "+ BetaEnabler.FAIRPHONE_BETA_PROPERTY+" "+BetaEnabler.BETA_ENABLED);
            try {
                Shell.runRootCommand(command);
            } catch (IOException | TimeoutException | RootDeniedException e) {
	            Log.d(TAG, "Failed to setprop: " + e.getLocalizedMessage());
            }
        }
    }

    public static String getOtaPackagePath(Resources resources, DownloadableItem item, boolean isVersion){
        String path;

        if (Utils.hasUnifiedPartition(resources))
        {
            path = resources.getString(R.string.recoveryCachePath) + Utils.getFilenameFromDownloadableItem(item, isVersion);
        }
        else
        {
            path = resources.getString(R.string.recoverySdCardPath) + resources.getString(R.string.updaterFolder) + Utils.getFilenameFromDownloadableItem(item, isVersion);
        }

        return path;
    }

    public static void writeCacheCommand(Context context, String otaPackagePath) throws IOException, TimeoutException, RootDeniedException, Resources.NotFoundException {
        if (PrivilegeChecker.isPrivilegedApp()) {
            File recovery_dir = new File("/cache/recovery/");
            final boolean mkdirs = recovery_dir.mkdirs();
            if(! (mkdirs || recovery_dir.exists()) ) {
                String errorMessage = context.getResources().getString(R.string.failed_mkdirs_cache_message);
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                throw new IOException(errorMessage);
            }

            File command = new File("/cache/recovery/command");
            File extendedCommand = new File("/cache/recovery/extendedcommand");
            final boolean deleteFailed = !extendedCommand.delete();
            if (deleteFailed) {
                Log.d(TAG, "Couldn't delete "+extendedCommand.getAbsolutePath());
            }

            String updateCommand = "--update_package=" + otaPackagePath;
            PrintWriter writer = new PrintWriter(command, "UTF-8");
            writer.println("--wipe_cache");
            writer.println(updateCommand);
            writer.flush();
            writer.close();
        }else {
            if(RootTools.isAccessGiven()) {
                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/command"));
                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/extendedcommand"));
                Shell.runRootCommand(new CommandCapture(0, "echo '--wipe_cache' >> /cache/recovery/command"));
                Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=" + otaPackagePath + "' >> /cache/recovery/command"));
            }else{
                throw new RootDeniedException("Root Denied");
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean rebootToRecovery(Context context) {
        boolean result;
        if (PrivilegeChecker.isPrivilegedApp()) {
            ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).reboot("recovery");
            result = false;
        } else {
            if(RootTools.isAccessGiven()) {
                try {
                    Shell.runRootCommand(new CommandCapture(0, "reboot recovery"));
                    result = true;
                } catch (IOException | TimeoutException | RootDeniedException e) {
                    Log.e(TAG, "Error rebooting to recovery: " + e.getLocalizedMessage());
                    result = false;
                }
            }else{
                result = false;
            }

        }
        return result;
    }
    
// --Commented out by Inspection START (06/02/2015 12:25):
//    public static void printStack(String moreLogs)
//    {
//        StringBuilder sb = new StringBuilder(moreLogs);
//        sb.append("\nStack --> ");
//        for (StackTraceElement ste : Thread.currentThread().getStackTrace())
//        {
//            sb.append(ste.getFileName()).append(" : ").append(ste.getMethodName()).append(":").append(ste.getLineNumber()).append(" -- ");
//        }
//        Log.wtf(TAG, sb.toString());
//    }
// --Commented out by Inspection STOP (06/02/2015 12:25)

    public static boolean fileExists(String otaPackagePath) {
        boolean fileExists;
        if(PrivilegeChecker.isPrivilegedApp()){
            File f = new File(otaPackagePath);
            fileExists = f.exists();
        }else {
            fileExists = RootTools.exists(otaPackagePath);
        }
        return fileExists;
    }

    private final static String[] SHELL_COMMANDS_ERASE_DATA = {
            // remove data
            "rm -rf /data/data/com.android.providers.media*",
            "rm -rf /data/data/com.android.keychain*",
            "rm -rf /data/data/com.android.location.fused*",
            "rm -rf /data/data/com.android.providers.applications*",
            "rm -rf /data/data/com.android.providers.media*",
            "rm -rf /data/data/com.android.vending*",
            "rm -rf /data/data/com.google.android.apps.genie.geniewidget*",
            "rm -rf /data/data/com.google.android.apps.plus*",
            "rm -rf /data/data/com.google.android.ears*",
            "rm -rf /data/data/com.google.android.gms*",
            "rm -rf /data/data/com.google.android.googlequicksearchbox*",
            "rm -rf /data/data/com.google.android.location*",
            "rm -rf /data/data/com.google.android.marvin.talkback*",
            // remove cache
//            "rm -rf /data/dalvik-cache",
            // remove data/app
            "rm -rf /data/app/com.android.apps.plus*",
            "rm -rf /data/app/com.android.vending*",
            "rm -rf /data/app/com.android.easr*",
            "rm -rf /data/app/com.android.gms*",
            "rm -rf /data/app/com.android.tts*"
    };

    public final static String SHELL_COMMAND_ERASE_DALVIK_CACHE = "rm -rf /data/dalvik-cache";
    public final static String SHELL_COMMAND_EXIT = "exit";

    public static void clearGappsData() throws RootDeniedException, IOException, InterruptedException {

        if (PrivilegeChecker.isPrivilegedApp()) {
            Process p = Runtime.getRuntime().exec(SHELL_COMMAND_ERASE_DALVIK_CACHE);
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : SHELL_COMMANDS_ERASE_DATA) {
                os.writeBytes(tmpCmd+"\n");
            }
            os.writeBytes(SHELL_COMMAND_EXIT+"\n");
            os.flush();
            p.waitFor();
        }else {
            if(RootTools.isAccessGiven()) {
                try {
                    Shell.runRootCommand(new CommandCapture(0, SHELL_COMMAND_ERASE_DALVIK_CACHE));
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                for (String tmpCmd : SHELL_COMMANDS_ERASE_DATA) {
                    try {
                        Shell.runRootCommand(new CommandCapture(0, tmpCmd));
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                throw new RootDeniedException("Root Denied");
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
