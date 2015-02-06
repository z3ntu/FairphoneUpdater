/*
 * Copyright (C) 2014 Fairphone Project
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

import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

public abstract class DownloadableItem
{
    private static final String TAG = DownloadableItem.class.getSimpleName();

    public static final String DEFAULT_NOTES_LANG = "en";

    private int mNumber;

    private String mName;

    private String mOTADownloadLink;

    private String mOTAMd5Sum;

    private String mBuildNumber;
    
    private final Map<String, String> mReleaseNotesMap;

    private String mReleaseDate;

    private String mThumbnailImageLink;

    DownloadableItem()
    {
        setNumber(0);
        setName("");
        setDownloadLink("");
        setMd5Sum("");
        setBuildNumber("");
        setReleaseDate("");
        setThumbnailLink("");

        mReleaseNotesMap = new HashMap<>();
    }

    public int getNumber()
    {
        return mNumber;
    }

    public void setNumber(String number)
    {
        try
        {
            this.mNumber = Integer.valueOf(number);
        } catch (NumberFormatException e)
        {
            this.mNumber = 0;
        }
    }

    public void setNumber(int number)
    {
        this.mNumber = number;
    }

    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        this.mName = name;
    }

    public String getDownloadLink()
    {
        return mOTADownloadLink;
    }

    public void setDownloadLink(String mDownloadLink)
    {
        this.mOTADownloadLink = mDownloadLink;
    }

    public String getMd5Sum()
    {
        return mOTAMd5Sum;
    }

    public void setMd5Sum(String mMd5Sum)
    {
        this.mOTAMd5Sum = mMd5Sum;
    }

    public boolean isNewerVersionThan(DownloadableItem item)
    {

        boolean result = false;

        if (item != null)
        {
            result = this.getNumber() > item.getNumber();
        }
        else
        {
            Log.e(TAG, "Invalid Number for Version");
        }

        return result;
    }

    public void setBuildNumber(String buildNumber)
    {
        mBuildNumber = buildNumber;
    }

    public String getBuildNumber()
    {
        return mBuildNumber;
    }

    public void setReleaseDate(String releaseDate)
    {
        mReleaseDate = releaseDate;
    }

    public String getReleaseDate()
    {
        return mReleaseDate;
    }

    public void setThumbnailLink(String thumbnailImageLink)
    {
        mThumbnailImageLink = thumbnailImageLink;
    }

    public String getThumbnailLink()
    {
        return mThumbnailImageLink;
    }

    public void setReleaseNotes(String language, String releaseNotes)
    {
        mReleaseNotesMap.put(language.toLowerCase(), releaseNotes);
    }

    public String getReleaseNotes(String language)
    {
        String releaseNotes = "";

        if (mReleaseNotesMap.containsKey(language))
        {
            releaseNotes = mReleaseNotesMap.get(language);
        }
        else if (mReleaseNotesMap.containsKey(DEFAULT_NOTES_LANG))
        {
            releaseNotes = mReleaseNotesMap.get(DEFAULT_NOTES_LANG);
        }
        return TextUtils.isEmpty(releaseNotes) ? "" : releaseNotes;
    }

// --Commented out by Inspection START (06/02/2015 12:36):
//    void resetReleaseNotes()
//    {
//        mReleaseNotesMap.clear();
//    }
// --Commented out by Inspection STOP (06/02/2015 12:36)
}
