package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.R;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.fragments.VersionListFragment.ListLayoutType;

public class OtherOSOptionsFragment extends BaseFragment
{

    private Button olderFairphoneOSButton;
    private Button androidOSButton;
    private Button mAppStoreButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_os_options, container, false);

        olderFairphoneOSButton = (Button) view.findViewById(R.id.older_fairphone_os_button);
        androidOSButton = (Button) view.findViewById(R.id.android_os_button);
        mAppStoreButton = (Button) view.findViewById(R.id.app_store_install_button);

        mainActivity.updateHeader(HeaderType.OTHER_OS, mainActivity.getResources().getString(R.string.other_os_options), false);

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setupOlderFairphoneVersionsButton();
        setupAndroidVersionsButton();
        setupAppStoreButton();
    }

    private void setupAppStoreButton()
    {
        if (!UpdaterData.getInstance().isAppStoreListEmpty())
        {
            mAppStoreButton.setVisibility(View.VISIBLE);
            mAppStoreButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    VersionListFragment newFragment = new VersionListFragment();
                    newFragment.setupFragment(ListLayoutType.APP_STORE);
                    mainActivity.changeFragment(newFragment);
                }
            });
        }
        else
        {
            mAppStoreButton.setVisibility(View.GONE);
        }
    }

    private void setupAndroidVersionsButton()
    {
        if (UpdaterData.getInstance().isAOSPVersionListNotEmpty())
        {
            androidOSButton.setVisibility(View.VISIBLE);
            androidOSButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    VersionListFragment newFragment = new VersionListFragment();
                    newFragment.setupFragment(ListLayoutType.ANDROID);
                    mainActivity.changeFragment(newFragment);
                }
            });
        }
        else
        {
            androidOSButton.setVisibility(View.GONE);
        }
    }

    private void setupOlderFairphoneVersionsButton()
    {
        if (UpdaterData.getInstance().isFairphoneVersionListNotEmpty())
        {
            olderFairphoneOSButton.setVisibility(View.VISIBLE);
            olderFairphoneOSButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    VersionListFragment newFragment = new VersionListFragment();
                    newFragment.setupFragment(ListLayoutType.FAIRPHONE);
                    mainActivity.changeFragment(newFragment);
                }
            });
        }
        else
        {
            olderFairphoneOSButton.setVisibility(View.GONE);
        }
    }
}