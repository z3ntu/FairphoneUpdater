package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.fairphone.updater.FairphoneUpdater2Activity.HeaderType;
import com.fairphone.updater.R;
import com.fairphone.updater.UpdaterData;
import com.fairphone.updater.fragments.VersionListFragment.ListLayoutType;

public class OtherOSOptionsFragment extends BaseFragment
{

    private static final String TAG = OtherOSOptionsFragment.class.getSimpleName();
    private Button olderFairphoneOSButton;
    private Button androidOSButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_os_options, container, false);

        olderFairphoneOSButton = (Button) view.findViewById(R.id.older_fairphone_os_button);
        androidOSButton = (Button) view.findViewById(R.id.android_os_button);

        mainActivity.updateHeader(HeaderType.OTHER_OS, "Other OS");

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setupOlderFairphoneVersionsButton();
        setupAndroidVersionsButton();
    }

    private void setupAndroidVersionsButton()
    {
        if (!UpdaterData.getInstance().isAOSPVersionListEmpty())
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
        if (!UpdaterData.getInstance().isFairphoneVersionListEmpty())
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