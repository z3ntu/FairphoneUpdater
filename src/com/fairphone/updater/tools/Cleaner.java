package com.fairphone.updater.tools;

import java.io.File;

import android.content.Context;

import com.fairphone.updater.R;

public class Cleaner
{
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
    
    public static void createDirPath(String dir, String location)
    {
        File f = new File(location + dir);

        if (!f.isDirectory())
        {
            f.mkdirs();
        }
    }
}
