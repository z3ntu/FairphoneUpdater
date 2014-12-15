package com.fairphone.updater.data;

import java.util.ArrayList;

public class Store extends DownloadableItem
{
    private String mStoreDescription;
    
    public Store(String name, String link, String md5)
    {
        super();
        
        mDependencies = new ArrayList<Integer>();
    }

    public String getDescription()
    {
        return mStoreDescription;
    }

    public void setDescription(String storeDescription)
    {
        this.mStoreDescription = storeDescription;
    }
    
}
