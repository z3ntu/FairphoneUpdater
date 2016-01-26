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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!this.getNumber().equals(another.getNumber()) && this.mImageType.equalsIgnoreCase(another.mImageType))
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

    /**
     *
     * @param fingerprint String containing sdfasdf=number.number.nunmber
     * @return the version as string or null if no version was found
     */
    private String getVersionNumberFromFingerPrint(String fingerprint){
        Pattern pattern = Pattern.compile(".*?\\d+.*?(\\d+)(\\.)(\\d+)(\\.)(\\d+)",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);    // is this pattern sufficient?
        Matcher matcher = pattern.matcher(fingerprint);                             // get version number from fingerprint
        if (matcher.find()) {
            return  matcher.group(1) + "." +matcher.group(3) + "." + matcher.group(5);
        }
        Log.d(TAG,String.format("Failed to determine version number from fingerprint: %s",fingerprint));
        return null; /* we don't know what version is here */

    }

    /**
     * This method uses the different strategies to find a human readable name for this version
     * It tries to find a pattern of "a.b.c" numbers in the string returned from "getNumber()"
     * and appends some strings to it
     * @return A human (english) version name to be displayed on the screen
     */
    public String getHumanReadableName() {
        String fingerprint = getNumber();

        if (fingerprint == null) { /* this for example currently happens on a zip install */

            /* try to make something nice */
            StringBuffer sb = new StringBuffer();

            if (getName() != null) {
                sb.append(getName());
            }
            if (getBuildNumber() != null) {
                if (sb.length() != 0) {
                    sb.append(" ");/* it not empty add a space */
                }
                sb.append(getBuildNumber());
            }
            return sb.toString();
        }

        String actualNumber = getVersionNumberFromFingerPrint(fingerprint);
        if (actualNumber == null) {
                        /* we can not extract the a.b.c version number from the fingerprint */
                        /* just return the fingerprint/version thingy that was passed to us*/
            return fingerprint;
        }

        //@TODO Version currently only has the concept of image type  "AOSP" or Fairphone. this concept
        //need to be elaborated and different versions shoud be possible.
        //
        if (fingerprint.contains("sibon")) {
            return "Fairphone Open Source OS " + actualNumber;
        } else if (fingerprint.contains("gms")) {
            return "Fairphone OS " + actualNumber;
        } else {
            /* we do have a version but.. we don't know about it. return the full "number"
             */
            return fingerprint;
        }
    }
}
