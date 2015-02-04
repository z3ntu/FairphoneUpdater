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

import com.fairphone.updater.R;
import com.fairphone.updater.tools.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
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

    public static Version getDeviceVersion(Context context)
    {

        Version version = new Version();
        String[] suportedDevices = context.getResources().getString(R.string.knownFPDevices).split(";");
        String modelWithoutSpaces = Build.MODEL.replaceAll("\\s", "");
        boolean knownFPDevice = false;
        for(String device : suportedDevices){
            knownFPDevice = knownFPDevice || device.equals(modelWithoutSpaces);
        }
        
        try
        {
            version.setNumber(Integer.valueOf(getSystemData(context, CURRENT_VERSION_NUMBER, knownFPDevice)));
        } catch (NumberFormatException e)
        {
            version.setNumber(context.getResources().getInteger(R.integer.defaultVersionNumber));
        }
        version.setName(getSystemData(context, CURRENT_VERSION_NAME, knownFPDevice));
        version.setBuildNumber(getSystemData(context, CURRENT_VERSION_BUILD_NUMBER, knownFPDevice));
        version.setAndroidVersion(getSystemData(context, CURRENT_ANDROID_VERSION, knownFPDevice));
        version.setImageType(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, knownFPDevice));
        version.setReleaseDate(getSystemData(context, CURRENT_VERSION_BUILD_DATE, knownFPDevice));
        version.setBetaStatus(getSystemData(context, CURRENT_BETA_STATUS, knownFPDevice));

        Version versionData = UpdaterData.getInstance().getVersion(version.getImageType(), version.getNumber());
        version.setThumbnailLink(versionData != null ? versionData.getThumbnailLink() : "");
        version.setReleaseNotes(Locale.getDefault().getLanguage(), versionData != null ? versionData.getReleaseNotes(Locale.getDefault().getLanguage()) : "");

        return version;
    }

    private static String getSystemData(Context context, String property, boolean useDefaults)
    {
		String result;
	    switch (property) {
		    case CURRENT_VERSION_NUMBER:
			    result = Utils.getprop(CURRENT_VERSION_NUMBER, useDefaults ? String.valueOf(context.getResources().getInteger(R.integer.defaultVersionNumber)) : "");
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
        String filePath =
                Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + resources.getString(R.string.configFilename);
        File file = new File(filePath + resources.getString(R.string.config_xml));

        if (file.exists())
        {
            try
            {
                latest = parseLatestXML(context, file);
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
        RELEASES, AOSP, FAIRPHONE, VERSION, NAME, BUILD_NUMBER, ANDROID_VERSION, RELEASE_NOTES, RELEASE_DATE, MD5SUM, THUMBNAIL_LINK, UPDATE_LINK, ERASE_DATA_WARNING, DEPENDENCIES, STORES, STORE, SHOW_DISCLAIMER
    }

    // @formatter:on

    private static Version parseLatestXML(Context context, File latestFile) throws XmlPullParserException, IOException
    {

        Version version = null;
        Store store = null;

        UpdaterData update = UpdaterData.getInstance();
        update.resetUpdaterData();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        FileInputStream fis = new FileInputStream(latestFile);

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
                    switch (currentTag)
                    {
                        case AOSP:
                            if (tagName.equalsIgnoreCase(XML_TAGS.AOSP.name()))
                            {
                                update.setLatestAOSPVersionNumber(xpp.getAttributeValue(0));
                            }
                            version = readVersion(version, xpp, tagName);
                            version.setImageType(Version.IMAGE_TYPE_AOSP);
                            break;
                        case FAIRPHONE:
                            if (tagName.equalsIgnoreCase(XML_TAGS.FAIRPHONE.name()))
                            {
                                update.setLatestFairphoneVersionNumber(xpp.getAttributeValue(0));
                            }
                            version = readVersion(version, xpp, tagName);
                            version.setImageType(Version.IMAGE_TYPE_FAIRPHONE);
                            break;
                        case STORES:
                            store = readStore(store, xpp, tagName);
                            break;
                        default:
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = xpp.getName();
                    currentTag = getCurrentXmlElement(tagName, currentTag);
                    switch (currentTag)
                    {
                        case AOSP:
                            if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
                            {
                                update.addAOSPVersion(version);
                                version = null;
                            }
                            break;
                        case FAIRPHONE:
                            if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
                            {
                                update.addFairphoneVersion(version);
                                version = null;
                            }
                            break;
                        case STORES:
                            if (tagName.equalsIgnoreCase(XML_TAGS.STORE.name()))
                            {
                                update.addAppStore(store);
                                store = null;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }

            eventType = xpp.next();
        }
        fis.close();

        removeZipContents(context);
        return update.getLatestVersion(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE, true));
    }

    private static Version readVersion(Version version, XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException
    {

        if (version == null)
        {
            version = new Version();
        }

        readDownloadableItem(version, xpp, tagName);

        if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name()))
        {
            version.setNumber(xpp.getAttributeValue(0));
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.ANDROID_VERSION.name()))
        {
            version.setAndroidVersion(xpp.nextText());
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.ERASE_DATA_WARNING.name()))
        {
            version.setEraseAllPartitionWarning(true);
        }

        return version;
    }

    private static Store readStore(Store store, XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException
    {

        if (store == null)
        {
            store = new Store();
        }

        readDownloadableItem(store, xpp, tagName);

        if (tagName.equalsIgnoreCase(XML_TAGS.STORE.name()))
        {
            store.setNumber(xpp.getAttributeValue(0));
        }
        else if (tagName.equalsIgnoreCase(XML_TAGS.SHOW_DISCLAIMER.name()))
        {
            store.setShowDisclaimer(true);
        }

        return store;
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
        else if (tagName.equalsIgnoreCase(XML_TAGS.DEPENDENCIES.name()))
        {
            item.setVersionDependencies(xpp.nextText());
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
	        final boolean delete = file.delete();
	        if (!delete) {
		        Log.d(TAG, "Couldn't delete file: " + file.getAbsolutePath());
	        }
        }
    }

}
