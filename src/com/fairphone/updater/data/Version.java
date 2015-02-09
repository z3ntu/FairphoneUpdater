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
import android.util.Log;

import com.fairphone.updater.R;

import java.util.ArrayList;
import java.util.List;

public class Version extends DownloadableItem implements Comparable<Version>
{
    private static final String TAG = Version.class.getSimpleName();

    private static final String DEPENDENCY_SEPARATOR = ",";

    public static final String IMAGE_TYPE_AOSP = "AOSP";

    public static final String IMAGE_TYPE_FAIRPHONE = "FAIRPHONE";

    private String mImageType;

    private String mAndroidVersion;

    private String mBetaStatus;

    private boolean mErasePartitionsWarning;

    private final List<Integer> mDependencies;

    public Version()
    {
        super();
        mDependencies = new ArrayList<>();
        mAndroidVersion = "";
        mImageType = IMAGE_TYPE_FAIRPHONE;
        mErasePartitionsWarning = false;
        mBetaStatus = "";
    }

    public Version(Version other)
    {
        super(other);
        mDependencies = other.mDependencies;
        mAndroidVersion = other.mAndroidVersion;
        mImageType = other.mImageType;
        mErasePartitionsWarning = other.hasEraseAllPartitionWarning();
        mBetaStatus = other.mBetaStatus;
    }

    public void setEraseAllPartitionWarning()
    {
        mErasePartitionsWarning = true;
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
            if (this.getNumber() < another.getNumber() && this.mImageType.equalsIgnoreCase(another.mImageType))
            {
                retVal = 1;
            }
            else if (this.getNumber() == another.getNumber() && this.mImageType.equalsIgnoreCase(another.mImageType))
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

    public void setVersionDependencies(String dependencyList)
    {
        if (TextUtils.isEmpty(dependencyList))
        {
            mDependencies.clear();
        }
        else
        {
            String[] dependencies = dependencyList.split(DEPENDENCY_SEPARATOR);
            for (String dependency : dependencies)
            {
                try
                {
                    mDependencies.add(Integer.valueOf(dependency));
                } catch (NumberFormatException e)
                {
                    Log.e(TAG, "Invalid dependency: " + e.getLocalizedMessage());
                }
            }
        }
    }

// --Commented out by Inspection START (09/02/2015 19:47):
//    List<Integer> getVersionDependencies()
//    {
//        return mDependencies;
//    }
// --Commented out by Inspection STOP (09/02/2015 19:47)
}
