package com.fairphone.updater.data;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fairphone.updater.R;
import com.fairphone.updater.tools.Utils;
import com.fairphone.updater.tools.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

public class VersionParserHelper {

    private static final String TAG = VersionParserHelper.class.getSimpleName();

    private static final String CURRENT_VERSION_NUMBER = "fairphone.ota.version.number";
    private static final String CURRENT_VERSION_NAME = "fairphone.ota.version.name";
    private static final String CURRENT_VERSION_BUILD_NUMBER = "fairphone.ota.build_number";
    private static final String CURRENT_VERSION_IMAGE_TYPE = "fairphone.ota.image_type";
    private static final String CURRENT_VERSION_ID = "ro.build.version.incremental";                // for FP2
    private static final String CURRENT_VERSION_BASEBAND_VERSION = "gsm.version.baseband";

    private static Version version;
    public static Version getDeviceVersion(Context context)
    {
        if (version == null){
            Version versionBuilder = new Version();
            String[] supportedDevices = context.getResources().getString(R.string.knownFPDevices).split(";");
            String modelWithoutSpaces = Build.MODEL.replaceAll("\\s", "");
            boolean knownFPDevice = false;
            for (String device : supportedDevices) {
                knownFPDevice = knownFPDevice || device.equals(modelWithoutSpaces);
            }

            if(modelWithoutSpaces.equals(context.getResources().getString(R.string.FP2Model))) {
                // FP2
                try {
                    versionBuilder.setId(getSystemData(context, CURRENT_VERSION_ID, knownFPDevice));
                } catch (NumberFormatException e) {
                    String defaultVersionId = context.getResources().getString(R.string.defaultVersionId);
                    Log.w(TAG, "Error parsing current version id. Defaulting to " + defaultVersionId + ": " + e.getLocalizedMessage());
                    versionBuilder.setId(defaultVersionId);
                }
                versionBuilder.setName(versionBuilder.getCurrentImageType());
                versionBuilder.setBuildNumber(versionBuilder.getBuildNumberFromId());
                versionBuilder.setBasebandVersion(getSystemData(context, CURRENT_VERSION_BASEBAND_VERSION, knownFPDevice));
            } else {
                // FP1(U)
                try
                {
                    versionBuilder.setId(getSystemData(context, CURRENT_VERSION_NUMBER, knownFPDevice) );
                } catch (NumberFormatException e) {
                    String defaultVersionNumber = context.getResources().getString(R.string.defaultVersionId);
                    Log.w(TAG, "Error parsing current version number. Defaulting to " + defaultVersionNumber + ": " + e.getLocalizedMessage());
                    versionBuilder.setId(defaultVersionNumber);
                }
                versionBuilder.setName(getSystemData(context, CURRENT_VERSION_NAME, knownFPDevice));
                versionBuilder.setBuildNumber(getSystemData(context, CURRENT_VERSION_BUILD_NUMBER, knownFPDevice));
            }

            versionBuilder.setImageType(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, knownFPDevice));

            Version versionData = UpdaterData.getInstance().getVersion(versionBuilder.getImageType(), versionBuilder.getId());
            versionBuilder.setThumbnailLink(versionData != null ? versionData.getThumbnailLink() : "");
            versionBuilder.setReleaseNotes(Locale.getDefault().getLanguage(), versionData != null ? versionData.getReleaseNotes(Locale.getDefault().getLanguage()) : "");
            version = versionBuilder;
        }

        return version;
    }

    private static String getSystemData(Context context, String property, boolean useDefaults)
    {
        String result;
        switch (property) {
            case CURRENT_VERSION_NUMBER:
                result = Utils.getprop(CURRENT_VERSION_NUMBER, useDefaults ? String.valueOf(context.getResources().getString(R.string.defaultVersionId)) : "");
                break;
            case CURRENT_VERSION_NAME:
                result = Utils.getprop(CURRENT_VERSION_NAME, useDefaults ? context.getResources().getString(R.string.defaultVersionName) : "");
                break;
            case CURRENT_VERSION_BUILD_NUMBER:
                result = Utils.getprop(CURRENT_VERSION_BUILD_NUMBER, useDefaults ? context.getResources().getString(R.string.defaultBuildNumber) : "");
                break;
            case CURRENT_VERSION_IMAGE_TYPE:
                result = Utils.getprop(CURRENT_VERSION_IMAGE_TYPE, useDefaults ? context.getResources().getString(R.string.defaultImageType) : "");
                break;
            case CURRENT_VERSION_ID:
                result = Utils.getprop(CURRENT_VERSION_ID, useDefaults ? "" : ""); // TODO: define default value for fingerprint
                break;
            case CURRENT_VERSION_BASEBAND_VERSION:
                result = Utils.getprop(CURRENT_VERSION_BASEBAND_VERSION, useDefaults ? "" : ""); // TODO: define default value for baseband version
                break;
            default:
                result = "";
                break;
        }

        return result;
    }

    public static Version getLatestVersion(Context context)
    {

        Version latest = null;
        Resources resources = context.getResources();
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(resources.getString(R.string.configFilename) + resources.getString(R.string.config_xml));
        } catch (FileNotFoundException e){
        }

        if (fis != null)
        {
            try
            {
                XmlParser xmlParser = new XmlParser();
                UpdaterData updaterData = xmlParser.parse(fis);
                latest = updaterData.getLatestVersion(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, true));
            } catch (XmlPullParserException e)
            {
                Log.e(TAG, "Could not start the XML parser", e);
            } catch (IOException e)
            {
                Log.e(TAG, "Invalid data in File", e);
                // remove the files
                removeConfigFiles(context);
            }
        }

        removeZipContents(context);

        return latest;
    }

    public static void removeConfigFiles(Context context)
    {
        Resources resources = context.getResources();
        String filePath =
                Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + resources.getString(R.string.configFilename);

        removeFile(filePath + resources.getString(R.string.config_zip));
        removeZipContents(context);
    }

    private static void removeZipContents(Context context)
    {
        Resources resources = context.getResources();
        String filePath =
                Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + resources.getString(R.string.configFilename);

        removeFile(filePath + resources.getString(R.string.config_xml));
        removeFile(filePath + resources.getString(R.string.config_sig));
    }

    private static void removeFile(String filePath)
    {
        File file = new File(filePath);
        if (file.exists())
        {
            final boolean notDeleted = !file.delete();
            if (notDeleted) {
                Log.d(TAG, "Couldn't delete file: " + file.getAbsolutePath());
            }
        }
    }
}
