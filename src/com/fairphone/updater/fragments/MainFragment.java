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

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.FairphoneUpdater.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;

public class MainFragment extends BaseFragment
{

    private LinearLayout mVersionUpToDateGroup;
    private TextView mVersionUpToDateCurrentVersionNameText;
    private LinearLayout mUpdateAvailableGroup;
    private TextView mUpdateAvailableCurrentVersionNameText;
    private TextView mUpdateAvailableNameText;
    private Button mUpdateAvailableInstallButton;
    private LinearLayout mOtherOSOptionsGroup;
    private Button mOtherOSOptionsButton;
    private Version mDeviceVersion;
    private BroadcastReceiver newVersionbroadcastReceiver;
    private LinearLayout mCurrentVersionGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setupLayout(inflater, view);

        return view;
    }

    private void updateHeader()
    {
        if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mDeviceVersion.getImageType()))
        {
            mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "", false);
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mDeviceVersion.getImageType()))
        {
            mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "", false);
        }
    }

    private void setupLayout(LayoutInflater inflater, View view)
    {
        setupCurrentVersionGroup(inflater, view);

        // Version up to date group
        mVersionUpToDateGroup = (LinearLayout) view.findViewById(R.id.version_up_to_date_group);
        mVersionUpToDateCurrentVersionNameText = (TextView) view.findViewById(R.id.version_up_to_date_current_version_name_text);

        // Update available group
        mUpdateAvailableGroup = (LinearLayout) view.findViewById(R.id.update_available_group);
        mUpdateAvailableCurrentVersionNameText = (TextView) view.findViewById(R.id.update_available_current_version_name_text);
        mUpdateAvailableNameText = (TextView) view.findViewById(R.id.update_available_name_text);
        mUpdateAvailableInstallButton = (Button) view.findViewById(R.id.install_update_button);

        // Other OS Options group
        mOtherOSOptionsGroup = (LinearLayout) view.findViewById(R.id.other_os_options_group);
        mOtherOSOptionsButton = (Button) view.findViewById(R.id.other_os_options_button);
    }

    private void setupCurrentVersionGroup(LayoutInflater inflater, View view)
    {
        mCurrentVersionGroup = (LinearLayout) view.findViewById(R.id.current_version_group);

        View updateGroupView = null;
        if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mainActivity.getDeviceVersion().getImageType()))
        {
            updateGroupView = inflater.inflate(R.layout.fragment_main_update_available_fairphone, null);
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mainActivity.getDeviceVersion().getImageType()))
        {
            updateGroupView = inflater.inflate(R.layout.fragment_main_update_available_android, null);
        }
        if (updateGroupView != null)
        {
            updateGroupView.setLayoutParams(mCurrentVersionGroup.getLayoutParams());
            mCurrentVersionGroup.addView(updateGroupView);
        }
    }

    private void updateOtherOSOptionsGroup()
    {
        if (!UpdaterData.getInstance().isFairphoneVersionListEmpty() || !UpdaterData.getInstance().isAOSPVersionListEmpty())
        {
            mOtherOSOptionsGroup.setVisibility(View.VISIBLE);
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
        else
        {
            mOtherOSOptionsGroup.setVisibility(View.GONE);
        }
    }

    public void toogleUpdateAvailableGroup()
    {
        updateCurrentVersionGroup();

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

        updateOtherOSOptionsGroup();
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
                    fragment.setupFragment(latestVersion, getDetailLayoutFromDeviceVersion(latestVersion));
                    mainActivity.changeFragment(fragment);
                }
            }
        });
    }

    private VersionDetailFragment.DetailLayoutType getDetailLayoutFromDeviceVersion(Version latestVersion)
    {
        VersionDetailFragment.DetailLayoutType type = DetailLayoutType.UPDATE_FAIRPHONE;
        if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(latestVersion.getImageType()))
        {
            type = DetailLayoutType.UPDATE_FAIRPHONE;
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(latestVersion.getImageType()))
        {
            type = DetailLayoutType.UPDATE_ANDROID;
        }
        return type;
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

        mDeviceVersion = mainActivity.getDeviceVersion();
        updateHeader();
        updateCurrentVersionGroup();
        toogleUpdateAvailableGroup();
        updateOtherOSOptionsGroup();
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

                if (FairphoneUpdater.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED.equals(action))
                {
                    mainActivity.updateLatestVersionFromConfig();
                    if (mainActivity.getCurrentUpdaterState() == UpdaterState.NORMAL)
                    {
                        toogleUpdateAvailableGroup();
                    }
                }
                else if (FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED.equals(action))
                {
                    String link = intent.getStringExtra(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK);
                    Toast.makeText(context.getApplicationContext(),
                            context.getResources().getString(R.string.config_file_download_link_error_message) + " " + link, Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private void registerBroadCastReceiver()
    {
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(FairphoneUpdater.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
        iFilter.addAction(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
        mainActivity.registerReceiver(newVersionbroadcastReceiver, iFilter);
    }

    private void unregisterBroadCastReceiver()
    {
        mainActivity.unregisterReceiver(newVersionbroadcastReceiver);
    }

}
