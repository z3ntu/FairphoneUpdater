package com.fairphone.updater.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater2Activity;
import com.fairphone.updater.FairphoneUpdater2Activity.HeaderType;
import com.fairphone.updater.FairphoneUpdater2Activity.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.Version;

public class MainFragment extends BaseFragment
{

    private LinearLayout mVersionUpToDateGroup;
    private TextView mVersionUpToDateCurrentVersionNameText;
    private LinearLayout mUpdateAvailableGroup;
    private TextView mUpdateAvailableCurrentVersionNameText;
    private TextView mUpdateAvailableNameText;
    private Button mUpdateAvailableInstallButton;
    private Button mOtherOSOptionsButton;
    private Version mDeviceVersion;
    private BroadcastReceiver newVersionbroadcastReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setupLayout(view);

        mDeviceVersion = mainActivity.getDeviceVersion();
        updateHeader();
        updateCurrentVersionGroup();
        toogleUpdateAvailableGroup();
        updateOtherOSOptionsGroup();

        return view;
    }

    private void updateHeader()
    {
        if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mDeviceVersion.getImageType()))
        {
            mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mDeviceVersion.getImageType()))
        {
            mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "");
        }
    }

    private void setupLayout(View view)
    {
        // Version up to date group
        mVersionUpToDateGroup = (LinearLayout) view.findViewById(R.id.version_up_to_date_group);
        mVersionUpToDateCurrentVersionNameText = (TextView) view.findViewById(R.id.version_up_to_date_current_version_name_text);

        // Update available group
        mUpdateAvailableGroup = (LinearLayout) view.findViewById(R.id.update_available_group);
        mUpdateAvailableCurrentVersionNameText = (TextView) view.findViewById(R.id.update_available_current_version_name_text);
        mUpdateAvailableNameText = (TextView) view.findViewById(R.id.update_available_name_text);
        mUpdateAvailableInstallButton = (Button) view.findViewById(R.id.install_update_button);

        // Other OS Options group
        mOtherOSOptionsButton = (Button) view.findViewById(R.id.other_os_options_button);
    }

    private void updateOtherOSOptionsGroup()
    {
        mOtherOSOptionsButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                OtherOSOptionsFragment newFragment = new OtherOSOptionsFragment();
                mainActivity.changeFragment(newFragment);
            }
        });
    }

    public void toogleUpdateAvailableGroup()
    {
        if (mainActivity.isUpdateAvailable())
        {
            mVersionUpToDateGroup.setVisibility(View.GONE);
            mUpdateAvailableGroup.setVisibility(View.VISIBLE);

            updateUpdateAvailableGroup();
        }
        else
        {
            mUpdateAvailableGroup.setVisibility(View.GONE);
            mVersionUpToDateGroup.setVisibility(View.VISIBLE);
        }
    }

    private void updateUpdateAvailableGroup()
    {
        mUpdateAvailableNameText.setText(mainActivity.getLatestVersionName());
        mUpdateAvailableInstallButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                VersionDetailFragment fragment = new VersionDetailFragment();

                Version latestVersion = mainActivity.getLatestVersion();
                if (latestVersion != null)
                {
                    fragment.setupFragment(latestVersion, VersionDetailFragment.DetailLayoutType.UPDATE);
                    mainActivity.changeFragment(fragment);
                }
            }
        });
    }

    private void updateCurrentVersionGroup()
    {
        String currentVersionName = mainActivity.getDeviceVersionName();
        mVersionUpToDateCurrentVersionNameText.setText(currentVersionName);
        mVersionUpToDateCurrentVersionNameText.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mainActivity.onEnableDevMode();
            }
        });

        mUpdateAvailableCurrentVersionNameText.setText(currentVersionName);
        mUpdateAvailableCurrentVersionNameText.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mainActivity.onEnableDevMode();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setupBroadcastReceiver();

        registerBroadCastReceiver();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        unregisterBroadCastReceiver();
    }

    protected void setupBroadcastReceiver()
    {
        newVersionbroadcastReceiver = new BroadcastReceiver()
        {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                if (FairphoneUpdater2Activity.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED.equals(action))
                {
                    mainActivity.updateLatestVersionFromConfig();
                    if (mainActivity.getCurrentUpdaterState() == UpdaterState.NORMAL)
                    {
                        toogleUpdateAvailableGroup();
                    }
                }
                else if (FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED.equals(action))
                {
                    String link = intent.getStringExtra(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK);
                    Toast.makeText(context.getApplicationContext(), context.getResources().getString(R.string.configFileDownloadLinkError) + " " + link,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private void registerBroadCastReceiver()
    {
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
        iFilter.addAction(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
        mainActivity.registerReceiver(newVersionbroadcastReceiver, iFilter);
    }

    private void unregisterBroadCastReceiver()
    {
        mainActivity.unregisterReceiver(newVersionbroadcastReceiver);
    }

}
