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

package com.fairphone.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class VersionParserHelper {

	private static final String TAG = VersionParserHelper.class.getSimpleName();
	
    private static final String CURRENT_VERSION_NUMBER = "fairphone.ota.version.number";
	private static final String CURRENT_VERSION_NAME = "fairphone.ota.version.name";
	private static final String CURRENT_VERSION_BUILD_NUMBER = "fairphone.ota.build_number";
	private static final String CURRENT_ANDROID_VERSION = "fairphone.ota.android_version";
	private static final String CURRENT_VERSION_IMAGE_TYPE = "fairphone.ota.image_type";
    
	public static String getNameFromVersion(Version version) {
		return "fp_update_" + version.getNumber() + ".zip";
	}
	
    public static Version getDeviceVersion(Context context) {

        Version version = new Version();

        try {
            version.setNumber(Integer.valueOf(getSystemData(context, CURRENT_VERSION_NUMBER)));
        } catch (NumberFormatException e) {
            version.setNumber(context.getResources().getInteger(R.integer.defaultVersionNumber));
        }
        version.setName(getSystemData(context, CURRENT_VERSION_NAME));
        version.setBuildNumber(getSystemData(context, CURRENT_VERSION_BUILD_NUMBER));
        version.setAndroidVersion(getSystemData(context, CURRENT_ANDROID_VERSION));
        version.setImageType(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE));

        Version versionData = UpdaterData.getInstance().getVersion(version.getImageType(),
                version.getNumber());
        version.setThumbnailLink(versionData != null ? versionData.getThumbnailLink() : null);
        version.setReleaseNotes(versionData != null ? versionData.getReleaseNotes() : "");
        

        return version;
    }
	   
	public static String getSystemData(Context context, String property) {

        if (property.equals(CURRENT_VERSION_NUMBER)) {
            return getprop(CURRENT_VERSION_NUMBER, String.valueOf(context.getResources()
                    .getInteger(R.integer.defaultVersionNumber)));
        } else if (property.equals(CURRENT_VERSION_NAME)) {
            return getprop(CURRENT_VERSION_NAME,
                    context.getResources().getString(R.string.defaultVersionName));
        } else if (property.equals(CURRENT_ANDROID_VERSION)) {
            return getprop(CURRENT_ANDROID_VERSION,
                    context.getResources().getString(R.string.defaultAndroidVersionNumber));
        } else if (property.equals(CURRENT_VERSION_BUILD_NUMBER)) {
            return getprop(CURRENT_VERSION_BUILD_NUMBER,
                    context.getResources().getString(R.string.defaultBuildNumber));
        } else if (property.equals(CURRENT_VERSION_IMAGE_TYPE)) {
            return getprop(CURRENT_VERSION_IMAGE_TYPE,
                    context.getResources().getString(R.string.defaultImageType));
        }

		return null;
	}
	
    public static Version getLatestVersion(Context context) {

        Version latest = null;
        Resources resources = context.getResources();
        String filePath = Environment.getExternalStorageDirectory()
                + resources.getString(R.string.updaterFolder)
                + resources.getString(R.string.configFilename);
        File file = new File(filePath
                + resources.getString(R.string.config_xml));

        if (file.exists()) {
            try {
                latest = parseLatestXML(context, file);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Could not start the XML parser", e);
            } catch (IOException e) {
                Log.e(TAG, "Invalid data in File", e);
                // remove the files
                removeFiles(context);
            }
        }
        
        removeZipContents(context);

        return latest;
    }
	
    // @formatter:off
    public enum XML_LEVEL_TAGS {
        NONE, AOSP, FAIRPHONE
    }

    public enum XML_TAGS {
        RELEASES, AOSP, FAIRPHONE, VERSION, NAME, BUILD_NUMBER, ANDROID_VERSION, RELEASE_NOTES, RELEASE_DATE, MD5SUM, THUMBNAIL_LINK, UPDATE_LINK, ERASE_DATA_WARNING, DEPENDENCIES;
    }

    // @formatter:on

    private static Version parseLatestXML(Context context, File latestFile)
            throws XmlPullParserException, IOException {

        Version version = null;

        UpdaterData update = UpdaterData.getInstance();
        update.resetUpdaterData();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        FileInputStream fis = new FileInputStream(latestFile);

        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new InputStreamReader(fis));

        int eventType = xpp.getEventType();

        XML_LEVEL_TAGS currentTag = XML_LEVEL_TAGS.NONE;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = null;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    tagName = xpp.getName();
                    currentTag = getCurrentXmlElement(tagName, currentTag);
                    switch (currentTag) {
                        case AOSP:
                            if (tagName.equalsIgnoreCase(XML_TAGS.AOSP.name())) {
                                update.setLatestAOSPVersionNumber(xpp.getAttributeValue(0));
                            }
                            version = readVersion(version, xpp, tagName);
                            if (TextUtils.isEmpty(version.getImageType())) {
                                version.setImageType(Version.IMAGE_TYPE_AOSP);
                            }
                            break;
                        case FAIRPHONE:
                            if (tagName.equalsIgnoreCase(XML_TAGS.FAIRPHONE.name())) {
                                update.setLatestFairphoneVersionNumber(xpp.getAttributeValue(0));
                            }
                            version = readVersion(version, xpp, tagName);
                            if (TextUtils.isEmpty(version.getImageType())) {
                                version.setImageType(Version.IMAGE_TYPE_FAIRPHONE);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = xpp.getName();
                    currentTag = getCurrentXmlElement(tagName, currentTag);
                    switch (currentTag) {
                        case AOSP:
                            if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name())) {
                                update.addAOSPVersion(version);
                                version = null;
                            }
                            break;
                        case FAIRPHONE:
                            if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name())) {
                                update.addFairphoneVersion(version);
                                version = null;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }

            eventType = xpp.next();
        }
        removeZipContents(context);
        return update.getLatestVersion(getSystemData(context, CURRENT_VERSION_IMAGE_TYPE));
    }
	
    public static Version readVersion(Version version, XmlPullParser xpp, String tagName)
            throws XmlPullParserException, IOException {
        
        if (version == null) {
            version = new Version();
        }

        if (tagName.equalsIgnoreCase(XML_TAGS.VERSION.name())) {
            version.setNumber(xpp.getAttributeValue(0));
        } else if (tagName.equalsIgnoreCase(XML_TAGS.NAME.name())) {
            version.setName(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.BUILD_NUMBER.name())) {
            version.setBuildNumber(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.ANDROID_VERSION.name())) {
            version.setAndroidVersion(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.DEPENDENCIES.name())) {
            version.setVersionDependencies(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.RELEASE_NOTES.name())) {
            version.setReleaseNotes(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.RELEASE_DATE.name())) {
            version.setReleaseDate(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.MD5SUM.name())) {
            version.setMd5Sum(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.THUMBNAIL_LINK.name())) {
            version.setThumbnailLink(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.UPDATE_LINK.name())) {
            version.setDownloadLink(xpp.nextText());
        } else if (tagName.equalsIgnoreCase(XML_TAGS.ERASE_DATA_WARNING.name())) {
            version.setEraseAllPartitionWarning(true);
        }

        return version;
    }

    private static XML_LEVEL_TAGS getCurrentXmlElement(String tagName, XML_LEVEL_TAGS current) {
        XML_LEVEL_TAGS retval = current;

        if (tagName.equalsIgnoreCase(XML_LEVEL_TAGS.AOSP.name())) {
            retval = XML_LEVEL_TAGS.AOSP;
        } else if (tagName.equalsIgnoreCase(XML_LEVEL_TAGS.FAIRPHONE.name())) {
            retval = XML_LEVEL_TAGS.FAIRPHONE;
        }
        return retval;
    }

	private static String getprop(String name, String defaultValue) {
        ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop", name);
        pb.redirectErrorStream(true);
        
        Process p = null;
        InputStream is = null;
        try {
            p = pb.start();
            is = p.getInputStream();
            Scanner scan = new Scanner(is);
            scan.useDelimiter("\n");
            String prop = scan.next();
            if (prop.length() == 0) {
                return defaultValue;
            }
            return prop;
        } catch (NoSuchElementException e) {
            return defaultValue;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return defaultValue;
    }
	
	public static void removeFiles(Context context) {
		Resources resources = context.getResources();
        String filePath = Environment.getExternalStorageDirectory()
                + resources.getString(R.string.updaterFolder)
                + resources.getString(R.string.configFilename);

        removeFile(filePath + resources.getString(R.string.config_zip));
        removeZipContents(context);
    }

    public static void removeZipContents(Context context) {
    	Resources resources = context.getResources();
        String filePath = Environment.getExternalStorageDirectory()
                + resources.getString(R.string.updaterFolder)
                + resources.getString(R.string.configFilename);
        
        removeFile(filePath + resources.getString(R.string.config_xml));
        removeFile(filePath + resources.getString(R.string.config_sig));
    }

    private static void removeFile(String filePath) {
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }
    }
    
}
