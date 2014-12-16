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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.text.TextUtils;

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.R;

public class Version extends DownloadableItem implements Comparable<Version>
{
    public static final String FAIRPHONE_VERSION_NUMBER = "FairphoneUpdateVersionNumber";

    public static final String FAIRPHONE_VERSION_NAME = "FairphoneUpdateVersionName";

    public static final String FAIRPHONE_VERSION_BUILD_NUMBER = "FairphoneUpdateVersionBuildNumber";

    public static final String FAIRPHONE_ANDROID_VERSION = "FairphoneUpdateAndroidVersion";

    public static final String FAIRPHONE_VERSION_OTA_DOWNLOAD_LINK = "FairphoneUpdateVersionOTADownloadLink";

    public static final String FAIRPHONE_VERSION_THUMBNAIL_DOWNLOAD_LINK = "FairphoneUpdateVersionThumbnailDownloadLink";

    public static final String FAIRPHONE_VERSION_OTA_MD5 = "FairphoneUpdateVersionOTAMD5";

    public static final String IMAGE_TYPE_AOSP = "AOSP";

    public static final String IMAGE_TYPE_FAIRPHONE = "FAIRPHONE";

    protected String mImageType;

    protected String mAndroidVersion;

    protected boolean mErasePartitionsWarning;

    public Version()
    {
        mDependencies = new ArrayList<Integer>();

        mNumber = 0;
        mName = "";
        mAndroidVersion = "";
        mOTADownloadLink = "";
        mOTAMd5Sum = "";
        mBuildNumber = "";
        mReleaseDate = "";
        mThumbnailImageLink = "";
        mImageType = IMAGE_TYPE_FAIRPHONE;
        mErasePartitionsWarning = false;
    }

    public static Version getVersionFromSharedPreferences(Context context)
    {
        Version version = new Version();
        SharedPreferences sharedPrefs = context.getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);
        Resources resources = context.getResources();

        int defaultVersionNumber = resources.getInteger(R.integer.defaultVersionNumber);
        version.setNumber(sharedPrefs.getInt(FAIRPHONE_VERSION_NUMBER, defaultVersionNumber));

        String defaultVersionName = resources.getString(R.string.defaultVersionName);
        version.setName(sharedPrefs.getString(FAIRPHONE_VERSION_NAME, defaultVersionName));

        String defaultVersionBuildNumber = resources.getString(R.string.defaultBuildNumber);
        version.setBuildNumber(sharedPrefs.getString(FAIRPHONE_VERSION_BUILD_NUMBER, defaultVersionBuildNumber));

        String defaultVersionAndroid = resources.getString(R.string.defaultAndroidVersionNumber);
        version.setAndroidVersion(sharedPrefs.getString(FAIRPHONE_ANDROID_VERSION, defaultVersionAndroid));

        version.setDownloadLink(sharedPrefs.getString(FAIRPHONE_VERSION_OTA_DOWNLOAD_LINK, ""));

        version.setThumbnailLink(sharedPrefs.getString(FAIRPHONE_VERSION_THUMBNAIL_DOWNLOAD_LINK, ""));

        version.setMd5Sum(sharedPrefs.getString(FAIRPHONE_VERSION_OTA_MD5, ""));

        if (TextUtils.isEmpty(version.getMd5Sum()) || TextUtils.isEmpty(version.getMd5Sum()))
        {
            return null;
        }

        version.setEraseAllPartitionWarning(false);

        return version;
    }

    public void saveToSharedPreferences(Context context)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);

        Editor editor = sharedPrefs.edit();
        editor.putInt(FAIRPHONE_VERSION_NUMBER, getNumber());
        editor.putString(FAIRPHONE_VERSION_NAME, getName());
        editor.putString(FAIRPHONE_ANDROID_VERSION, getAndroidVersion());
        editor.putString(FAIRPHONE_VERSION_BUILD_NUMBER, getBuildNumber());
        editor.putString(FAIRPHONE_VERSION_OTA_DOWNLOAD_LINK, getDownloadLink());
        editor.putString(FAIRPHONE_VERSION_THUMBNAIL_DOWNLOAD_LINK, getThumbnailLink());
        editor.putString(FAIRPHONE_VERSION_OTA_MD5, getMd5Sum());
        editor.commit();
    }

    public void setEraseAllPartitionWarning(boolean erasePartitionsWarning)
    {
        mErasePartitionsWarning = erasePartitionsWarning;
    }

    public boolean hasEraseAllPartitionWarning()
    {
        return mErasePartitionsWarning;
    }

    public void deleteFromSharedPreferences(Context context)
    {
        setNumber(1);
        setName(null);
        setBuildNumber(null);
        setAndroidVersion(null);
        resetReleaseNotes();
        setReleaseDate(null);
        setMd5Sum(null);
        setThumbnailLink(null);
        setDownloadLink(null);
        saveToSharedPreferences(context);
    }

    public String getAndroidVersion()
    {
        return mAndroidVersion;
    }

    public void setAndroidVersion(String mAndroid)
    {
        this.mAndroidVersion = mAndroid;
    }

    public void setImageType(String imageType)
    {
        mImageType = imageType;
    }

    public String getImageType()
    {
        return mImageType;
    }

    public String getImageTypeDescription(Resources resources)
    {
        return Version.getImageTypeDescription(mImageType, resources);
    }

    public static String getImageTypeDescription(String imageType, Resources resources)
    {
        String description = resources.getString(R.string.fairphone);
        if (!TextUtils.isEmpty(imageType))
        {
            if (imageType.equalsIgnoreCase(IMAGE_TYPE_AOSP))
            {
                description = resources.getString(R.string.android);
            }
            if (imageType.equalsIgnoreCase(IMAGE_TYPE_FAIRPHONE))
            {
                description = resources.getString(R.string.fairphone);
            }
        }
        return description;
    }

    public String getAndroidVersion(Resources resources)
    {
        String retVal = "";
        if (!TextUtils.isEmpty(mAndroidVersion))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(resources.getString(R.string.android));
            sb.append(" ");
            sb.append(mAndroidVersion);
            retVal = sb.toString();
        }
        return retVal;
    }
    
    @Override
    public int compareTo(Version another)
    {
        int retVal;
        if (another != null)
        {
            if (this.getNumber() < another.getNumber() && this.getImageType().equalsIgnoreCase(another.getImageType()))
            {
                retVal = 1;
            }
            else if (this.getNumber() == another.getNumber() && this.getImageType().equalsIgnoreCase(another.getImageType()))
            {
                retVal = 0;
            }
            else
            {
                retVal = -1;
            }
        }
        else
        {
            retVal = 1;
        }
        return retVal;
    }

}
