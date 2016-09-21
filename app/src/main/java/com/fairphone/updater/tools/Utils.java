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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.R;
import com.fairphone.updater.UpdaterService;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
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
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final String GAPPS_STORE_NUMBER = "0";

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
        Intent i = new Intent(context, UpdaterService.class);
        i.putExtra(UpdaterService.EXTRA_FORCE_CONFIG_FILE_DOWNLOAD, forceDownload);
        context.startService(i);
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

    public static String calculateMD5(File updateFile)
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

        double fp1DataPartitionSize = (double) resources.getInteger(R.integer.FP1DataPartitionSizeMb) / BUFFER_1024_BYTES;
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
                title = version.getHumanReadableName();
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

    private static Map<String,String> buildProps;
    public static Map<String,String> getpropAll(){

        if(buildProps==null) {
            buildProps = new HashMap<>();
            ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop");
            pb.redirectErrorStream(true);
            Pattern propRegex = Pattern.compile("\\[([^\\]]+)\\]: \\[([^\\]]+)\\]");

            Process p;
            InputStream is = null;
            try {
                p = pb.start();
                is = p.getInputStream();
                Scanner scan = new Scanner(is);
                String prop;
                do {
                    prop = scan.nextLine();
                    Matcher match = propRegex.matcher(prop);
                    if(match.find()){
                        buildProps.put(match.group(1), match.group(2));
                    }
                } while(!prop.isEmpty());
            } catch (NoSuchElementException e) {
            } catch (IOException e) {
            }
        }
        return buildProps;
    }

    public static String getprop(String name, String defaultValue)
    {
        String result;
        if (getpropAll().containsKey(name)){
            result = getpropAll().get(name);
        } else {
            result = defaultValue;
        }
        return result;
    }

    public static void enableBeta(Context context) {
        SharedPreferences settings = context.getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(FairphoneUpdater.PREFERENCE_BETA_MODE, true);
        editor.commit();
    }

    public static String getOtaPackagePath(Resources resources, DownloadableItem item, boolean isVersion, boolean isZipInstall){
        String path;

        if (Utils.hasUnifiedPartition(resources))
        {
            path = resources.getString(R.string.recoveryCachePath) + Utils.getFilenameFromDownloadableItem(item, isVersion);
        }
        else
        {
            if(isZipInstall && Build.MODEL.equalsIgnoreCase(resources.getString(R.string.FP1Model)))
            {
                //TODO: Find a way to not have this hardcoded
                String zipPath = item.getDownloadLink();
                path = zipPath.replace("/storage/sdcard0", resources.getString(R.string.recoverySdCardPath));
            }
            else
            {
                path = resources.getString(R.string.recoverySdCardPath) + resources.getString(R.string.updaterFolder) + Utils.getFilenameFromDownloadableItem(item, isVersion);
            }
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

    public static String getPath(final Context context, final Uri uri)
    {
        String filePath = uri.getPath();
        if ("content".equalsIgnoreCase(uri.getScheme())) {

            //Get the zip file name
            String[] path = filePath.split("/");
            String downloadIdStr = "";
            if (path != null && path.length > 0) {
                downloadIdStr = path[path.length - 1];
            }
            long downloadId = 0;
            try {
                downloadId = Long.parseLong(downloadIdStr);
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "NumberFormatException: " + nfe.getMessage());
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();

            query.setFilterById(downloadId);

            Cursor cursor = downloadManager != null ? downloadManager.query(query) : null;

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);

                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL: {
                        filePath = downloadManager.getUriForDownloadedFile(downloadId).getPath();
                        break;
                    }
                    case DownloadManager.STATUS_FAILED:
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_PENDING:
                    case DownloadManager.STATUS_RUNNING:
                    default:
                        filePath = "";
                        break;
                }
            }
        }

        return filePath;
    }

    public static boolean isInternetEnabled(Context context) {
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

	public static boolean isWiFiEnabled(Context context)
	{

		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
	}

    public static boolean isBatteryLevelOk(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;

        if(batteryPct >= Float.parseFloat(context.getResources().getString(R.string.minimumBatteryLevel))) {
            return true;
        }
        return false;
    }

    public static Store getGappsStore()
    {
        return UpdaterData.getInstance().getStore(GAPPS_STORE_NUMBER);
    }

    public static void restartUpdater(Activity activity) {
        Intent startActivity = new Intent(activity.getApplicationContext(), FairphoneUpdater.class);
        int pendingIntentId = 123456;
        PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)activity.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        System.exit(0);
    }

    public static int getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();

        int versionCode = 0;

        try {
            versionCode = packageManager.getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "App versionCode cannot be retrieved", e);
        }

        return versionCode;
    }
}
