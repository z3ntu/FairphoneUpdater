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

package com.fairphone.updater.data;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.fairphone.updater.R;
import com.fairphone.updater.tools.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class VersionParserHelper
{

    private static final String TAG = VersionParserHelper.class.getSimpleName();

    private static final String CURRENT_VERSION_NUMBER = "fairphone.ota.version.number";
    private static final String CURRENT_VERSION_NAME = "fairphone.ota.version.name";
    private static final String CURRENT_VERSION_BUILD_NUMBER = "fairphone.ota.build_number";
    private static final String CURRENT_BETA_STATUS = "fairphone.ota.beta";
    private static final String CURRENT_ANDROID_VERSION = "fairphone.ota.android_version";
    private static final String CURRENT_VERSION_IMAGE_TYPE = "fairphone.ota.image_type";
    private static final String CURRENT_VERSION_BUILD_DATE = "ro.build.date.utc";
    private static final String CURRENT_VERSION_ID = "ro.build.version.incremental";                // for FP2


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

            versionBuilder.setAndroidVersion(getSystemData(context, CURRENT_ANDROID_VERSION, knownFPDevice));
            versionBuilder.setImageType(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, knownFPDevice));
            versionBuilder.setReleaseDate(getSystemData(context, CURRENT_VERSION_BUILD_DATE, knownFPDevice));
            versionBuilder.setBetaStatus(getSystemData(context, CURRENT_BETA_STATUS, knownFPDevice));

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
		    case CURRENT_ANDROID_VERSION:
			    result = Utils.getprop(CURRENT_ANDROID_VERSION, useDefaults ? context.getResources().getString(R.string.defaultAndroidVersionNumber) : "");
			    break;
		    case CURRENT_VERSION_BUILD_NUMBER:
			    result = Utils.getprop(CURRENT_VERSION_BUILD_NUMBER, useDefaults ? context.getResources().getString(R.string.defaultBuildNumber) : "");
			    break;
		    case CURRENT_VERSION_IMAGE_TYPE:
			    result = Utils.getprop(CURRENT_VERSION_IMAGE_TYPE, useDefaults ? context.getResources().getString(R.string.defaultImageType) : "");
			    break;
		    case CURRENT_VERSION_BUILD_DATE:
			    result = Utils.getprop(CURRENT_VERSION_BUILD_DATE, useDefaults ? context.getResources().getString(R.string.defaultBuildDate) : "");
			    break;
		    case CURRENT_BETA_STATUS:
			    result = Utils.getprop(CURRENT_BETA_STATUS, useDefaults ? context.getResources().getString(R.string.defaultBetaStatus) : "0");
			    break;
            case CURRENT_VERSION_ID:
                result = Utils.getprop(CURRENT_VERSION_ID, useDefaults ? "" : ""); // TODO: define default value for fingerprint
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
                latest = parseLatestXML(context, fis);
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

    // @formatter:off
    public enum XML_LEVEL_TAGS
    {
        NONE, AOSP, FAIRPHONE, STORES
    }

    public enum XML_TAGS
    {
        /*RELEASES,*/ AOSP, FAIRPHONE, VERSION, NAME, BUILD_NUMBER, ANDROID_VERSION, RELEASE_NOTES, RELEASE_DATE, MD5SUM, THUMBNAIL_LINK, UPDATE_LINK, ERASE_DATA_WARNING, DEPENDENCIES, /*STORES,*/ STORE, SHOW_DISCLAIMER
    }

    // @formatter:on

    private static Version parseLatestXML(Context context, File latestFile) throws XmlPullParserException, IOException {
        FileInputStream fis = new FileInputStream(latestFile);
        return parseLatestXML(context, fis);
    }

    private static Version parseLatestXML(Context context, FileInputStream fis) throws XmlPullParserException, IOException
    {

        Version version = null;
        Store store = null;
        Pair<Version, Store> result;

        UpdaterData update = UpdaterData.getInstance();
        update.resetUpdaterData();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);


        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new InputStreamReader(fis));

        int eventType = xpp.getEventType();

        XML_LEVEL_TAGS currentTag = XML_LEVEL_TAGS.NONE;

        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            String tagName;
            switch (eventType)
            {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    tagName = xpp.getName();
                    currentTag = getCurrentXmlElement(tagName, currentTag);
                    result = readStartTag(xpp, currentTag, tagName, update, version, store);
                    version = result.first;
                    store = result.second;
                    break;
                case XmlPullParser.END_TAG:
                    tagName = xpp.getName();
                    currentTag = getCurrentXmlElement(tagName, currentTag);
                    result = readEndTag(currentTag, tagName, update, version, store);
                    version = result.first;
                    store = result.second;
                    break;
                default:
                    break;
            }

            eventType = xpp.next();
        }
        fis.close();

        removeZipContents(context);
        return update.getLatestVersion(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, true));
    }

    private static Pair<Version, Store> readStartTag(XmlPullParser xpp, XML_LEVEL_TAGS currentTag, String tagName, UpdaterData update, Version version, Store store) throws XmlPullParserException, IOException
    {
        Version updateVersion = null;
        Store updateStore = null;
        switch (currentTag)
        {
            case AOSP:
                if (tagName.equalsIgnoreCase(XML_TAGS.AOSP.name()))
                {
                    update.setLatestAOSPVersionNumber(xpp.getAttributeValue(0));
                }
                updateVersion = readVersion(version, xpp, tagName);
                updateVersion.setImageType(Version.IMAGE_TYPE_AOSP);
                break;
            case FAIRPHONE:
                if (tagName.equalsIgnoreCase(XML_TAGS.FAIRPHONE.name()))
                {
                    update.setLatestFairphoneVersionNumber(xpp.getAttributeValue(0));
                }
                updateVersion = readVersion(version, xpp, tagName);
                updateVersion.setImageType(Version.IMAGE_TYPE_FAIRPHONE);
                break;
            case STORES:
                updateStore = readStore(store, xpp, tagName);
                break;
            case NONE:
            default:
                if(version != null)
                {
                    updateVersion = new Version(version);
                }

                if(store != null)
                {
                    updateStore = new Store(store);
                }
                break;
        }

        return new Pair<>(updateVersion, updateStore);
    }

    private static Pair<Version, Store> readEndTag(XML_LEVEL_TAGS currentTag, String tagName, UpdaterData update, Version version, Store store) {
        Version updateVersion = null;
        Store updateStore = null;
        switch (currentTag)
        {
            case AOSP:
                if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
                {
                    update.addAOSPVersion(version);
                    updateVersion = null;
                }
                else
                {
                    if(version != null)
                    {
                        updateVersion = new Version(version);
                    }
                }
                break;
            case FAIRPHONE:
                if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
                {
                    update.addFairphoneVersion(version);
                    updateVersion = null;
                }
                else
                {
                    if(version != null)
                    {
                        updateVersion = new Version(version);
                    }
                }
                break;
            case STORES:
                if (tagName.equalsIgnoreCase(XML_TAGS.STORE.name()))
                {
                    update.addAppStore(store);
                    updateStore = null;
                }
                else
                {
                    if(store != null)
                    {
                        updateStore = new Store(store);
                    }
                }
                break;
            case NONE:
            default:
                if(version != null)
                {
                    updateVersion = new Version(version);
                }

                if(store != null)
                {
                    updateStore = new Store(store);
                }
                break;
        }

        return new Pair<>(updateVersion, updateStore);
    }

    private static Version readVersion(Version version, XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException
    {
        Version updateVersion;
        if (version == null)
        {
            updateVersion = new Version();
        }
        else
        {
            updateVersion = new Version(version);
        }

        readDownloadableItem(updateVersion, xpp, tagName);

        if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
        {
            updateVersion.setId(xpp.getAttributeValue(0));
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.ANDROID_VERSION.name()))
        {
            updateVersion.setAndroidVersion(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.DEPENDENCIES.name()))
        {
            updateVersion.setVersionDependencies(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.ERASE_DATA_WARNING.name()))
        {
            updateVersion.setEraseAllPartitionWarning();
        }

        return updateVersion;
    }

    private static Store readStore(Store store, XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException
    {

        Store updateStore;
        if (store == null)
        {
            updateStore = new Store();
        }
        else
        {
            updateStore = new Store(store);
        }

        readDownloadableItem(updateStore, xpp, tagName);

        if (tagName.equalsIgnoreCase(XML_TAGS.STORE.name()))
        {
            updateStore.setId(xpp.getAttributeValue(0));
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.SHOW_DISCLAIMER.name()))
        {
            updateStore.setShowDisclaimer();
        }

        return updateStore;
    }

    private static void readDownloadableItem(DownloadableItem item, XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException
    {
        if (tagName.equalsIgnoreCase(XML_TAGS.NAME.name()))
        {
            item.setName(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.BUILD_NUMBER.name()))
        {
            item.setBuildNumber(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.RELEASE_NOTES.name()))
        {
            item.setReleaseNotes(Version.DEFAULT_NOTES_LANG, xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.RELEASE_NOTES.name() + "_" + Locale.getDefault().getLanguage()))
        {
            item.setReleaseNotes(Locale.getDefault().getLanguage(), xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.RELEASE_DATE.name()))
        {
            item.setReleaseDate(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.MD5SUM.name()))
        {
            item.setMd5Sum(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.THUMBNAIL_LINK.name()))
        {
            item.setThumbnailLink(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.UPDATE_LINK.name()))
        {
            item.setDownloadLink(xpp.nextText());
        }
    }

    private static XML_LEVEL_TAGS getCurrentXmlElement(String tagName, XML_LEVEL_TAGS current)
    {
        XML_LEVEL_TAGS retval = current;

        if (tagName.equalsIgnoreCase(XML_LEVEL_TAGS.AOSP.name()))
        {
            retval = XML_LEVEL_TAGS.AOSP;
        }
        else if (tagName.equalsIgnoreCase(XML_LEVEL_TAGS.FAIRPHONE.name()))
        {
            retval = XML_LEVEL_TAGS.FAIRPHONE;
        }
        else if (tagName.equalsIgnoreCase(XML_LEVEL_TAGS.STORES.name()))
        {
            retval = XML_LEVEL_TAGS.STORES;
        }
        return retval;
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
