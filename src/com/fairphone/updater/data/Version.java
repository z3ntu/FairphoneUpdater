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

import android.content.res.Resources;
import android.text.TextUtils;

import com.fairphone.updater.R;

import java.util.ArrayList;

public class Version extends DownloadableItem implements Comparable<Version>
{

    public static final String IMAGE_TYPE_AOSP = "AOSP";

    public static final String IMAGE_TYPE_FAIRPHONE = "FAIRPHONE";

    private String mImageType;

    private String mAndroidVersion;

    private String mBetaStatus;

    private boolean mErasePartitionsWarning;

    public Version()
    {
        mDependencies = new ArrayList<>();

        mNumber = 0;
        mName = "";
        mAndroidVersion = "";
        mOTADownloadLink = "";
        mOTAMd5Sum = "";
        mBuildNumber = "";
        mReleaseDate = "";
        mThumbnailImageLink = "";
        mImageType = IMAGE_TYPE_FAIRPHONE;
        setEraseAllPartitionWarning(false);
        mBetaStatus = "";
    }

    public void setEraseAllPartitionWarning(boolean erasePartitionsWarning)
    {
        mErasePartitionsWarning = erasePartitionsWarning;
    }

    public boolean hasEraseAllPartitionWarning()
    {
        return mErasePartitionsWarning;
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

    public void setBetaStatus(String betaStatus)
    {
        mBetaStatus = betaStatus;
    }

    public String getBetaStatus()
    {
        return mBetaStatus;
    }

    public String getImageTypeDescription(Resources resources)
    {
        return Version.getImageTypeDescription(mImageType, resources);
    }

    private static String getImageTypeDescription(String imageType, Resources resources)
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

// --Commented out by Inspection START (06/02/2015 12:25):
//    public String getAndroidVersion(Resources resources)
//    {
//        String retVal = "";
//        if (!TextUtils.isEmpty(mAndroidVersion))
//        {
//	        retVal = resources.getString(R.string.android) + " " + mAndroidVersion;
//        }
//        return retVal;
//    }
// --Commented out by Inspection STOP (06/02/2015 12:25)

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") Version another)
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
