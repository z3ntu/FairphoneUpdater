package com.fairphone.updater.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.fairphone.updater.FairphoneUpdater;

public class BaseFragment extends Fragment
{
    protected FairphoneUpdater mainActivity;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mainActivity = (FairphoneUpdater) activity;

        } catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString() + " must implement " + FairphoneUpdater.class.getName());
        }
    }
}
