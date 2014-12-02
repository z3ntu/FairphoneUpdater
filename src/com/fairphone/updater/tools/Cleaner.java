package com.fairphone.updater.tools;

import java.io.File;

import android.app.DownloadManager;
import android.content.Context;

import com.fairphone.updater.R;

public class Cleaner
{
    public static void forceCleanConfigurationFiles(Context context, String downloadPath )
    {
        String configFileName = context.getResources().getString(R.string.gapps_installer_config_file);
        String configFileZip = context.getResources().getString(R.string.gapps_installer_zip);
        String configFileCfg = context.getResources().getString(R.string.gapps_installer_cfg);
        String configFileSig = context.getResources().getString(R.string.gapps_installer_sig);

        deleteFile("/" + configFileName + configFileZip, downloadPath);
        deleteFile("/" + configFileName + configFileCfg, downloadPath);
        deleteFile("/" + configFileName + configFileSig, downloadPath);
    }
    
    public static void deleteFile(String file, String location)
    {
        File f = new File(location + file);

        if (f.exists())
        {
            deleteRecursive(f);
        }
    }

    public static void deleteRecursive(File fileOrDirectory)
    {
        if (fileOrDirectory.isDirectory())
        {
            for (File child : fileOrDirectory.listFiles())
            {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

}
