package com.fairphone.updater.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.fairphone.updater.FairphoneUpdater2Activity;

public class BaseFragment extends Fragment
{
    protected FairphoneUpdater2Activity mainActivity;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mainActivity = (FairphoneUpdater2Activity) activity;

        } catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString() + " must implement " + FairphoneUpdater2Activity.class.getName());
        }
    }
}
