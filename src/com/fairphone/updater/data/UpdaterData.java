package com.fairphone.updater.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdaterData
{

    private static final String TAG = UpdaterData.class.getSimpleName();

    private static UpdaterData mInstance;

    private int mLatestAOSPVersionNumber;

    private int mLatestFairphoneVersionNumber;

    private final Map<Integer, Version> mAOSPVersionMap;

    private final Map<Integer, Version> mFairphoneVersionMap;
    
    private final Map<Integer, Store> mAppStoresMap;

    public static UpdaterData getInstance()
    {
        if (mInstance == null)
        {
            mInstance = new UpdaterData();
        }
        return mInstance;
    }

    private UpdaterData()
    {
        mLatestAOSPVersionNumber = 0;
        mLatestFairphoneVersionNumber = 0;
        mAOSPVersionMap = new HashMap<>();
        mFairphoneVersionMap = new HashMap<>();
        mAppStoresMap = new HashMap<>();
    }

    public void resetUpdaterData()
    {
        mLatestAOSPVersionNumber = 0;
        mLatestFairphoneVersionNumber = 0;
        mAOSPVersionMap.clear();
        mFairphoneVersionMap.clear();
        mAppStoresMap.clear();
    }

    public void setLatestAOSPVersionNumber(String latestVersion)
    {
        mLatestAOSPVersionNumber = getLatestVersionFromTag(latestVersion);
    }

    private static int getLatestVersionFromTag(String latestVersion)
    {
        int latestVersionNumber;
        try
        {
            latestVersionNumber = Integer.valueOf(latestVersion);
        } catch (NumberFormatException e)
        {
            Log.w(TAG, "Error decoding latest version number. Defaulting to 0: " + e.getLocalizedMessage());
            latestVersionNumber = 0;
        }
        return latestVersionNumber;
    }

    public void setLatestFairphoneVersionNumber(String latestVersion)
    {
        mLatestFairphoneVersionNumber = getLatestVersionFromTag(latestVersion);
    }

    public void addAOSPVersion(Version version)
    {
        mAOSPVersionMap.put(version.getNumber(), version);
    }

    public void addFairphoneVersion(Version version)
    {
        mFairphoneVersionMap.put(version.getNumber(), version);
    }
    
    public void addAppStore(Store store)
    {
        mAppStoresMap.put(store.getNumber(), store);
    }

    public Version getLatestVersion(String imageType)
    {
        Version version = null;
        if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(imageType))
        {
            version = mAOSPVersionMap.get(mLatestAOSPVersionNumber);
        }
        else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(imageType))
        {
            version = mFairphoneVersionMap.get(mLatestFairphoneVersionNumber);
        }

        return version;
    }

    private static List<Version> mapToOrderedVersionList(Collection<Version> a)
    {
        List<Version> retval = new ArrayList<>();
        for (Version version : a)
        {
            retval.add(version);
        }
        Collections.sort(retval);
        return retval;
    }
    
    private static List<Store> mapToOrderedStoreList(Collection<Store> a)
    {
        List<Store> retval = new ArrayList<>();
        for (Store store : a)
        {
            retval.add(store);
        }
        Collections.sort(retval);
        return retval;
    }

    public List<Version> getAOSPVersionList()
    {
	    return mapToOrderedVersionList(mAOSPVersionMap.values());
    }

    public List<Version> getFairphoneVersionList()
    {
	    return mapToOrderedVersionList(mFairphoneVersionMap.values());
    }
    
    public List<Store> getAppStoreList()
    {

	    return mapToOrderedStoreList(mAppStoresMap.values());
    }

    public Version getVersion(String imageType, int versionNumber)
    {
        Version version = null;
        if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(imageType))
        {
            version = mAOSPVersionMap.get(versionNumber);
        }
        else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(imageType))
        {
            version = mFairphoneVersionMap.get(versionNumber);
        }

        return version;
    }
    
    public Store getStore(int storeNumber)
    {
        return mAppStoresMap.get(storeNumber);
    }

    public boolean isAOSPVersionListNotEmpty()
    {
        return !mAOSPVersionMap.isEmpty();
    }

    public boolean isFairphoneVersionListNotEmpty()
    {
        return !mFairphoneVersionMap.isEmpty();
    }
    
    public boolean isAppStoreListEmpty()
    {
        return mAppStoresMap.isEmpty();
    }
}
