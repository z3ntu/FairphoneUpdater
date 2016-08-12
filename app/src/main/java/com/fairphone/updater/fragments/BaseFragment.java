package com.fairphone.updater.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;

import com.fairphone.updater.FairphoneUpdater;

public class BaseFragment extends Fragment
{
    FairphoneUpdater mainActivity;
    SharedPreferences mSharedPreferences;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mainActivity = (FairphoneUpdater) activity;
            
            mSharedPreferences = mainActivity.getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);

        } catch (ClassCastException e)
        {
            throw new ClassCastException(activity + " must implement " + FairphoneUpdater.class.getName() + ": " + e.getLocalizedMessage());
        }
    }
}
