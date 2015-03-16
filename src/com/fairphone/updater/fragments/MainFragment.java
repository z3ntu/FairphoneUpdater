package com.fairphone.updater.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.FairphoneUpdater.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;

public class MainFragment extends BaseFragment
{

	private static final String TAG = MainFragment.class.getSimpleName();
    public static final String SHARED_PREFERENCES_ENABLE_GAPPS = "SHARED_PREFERENCES_ENABLE_GAPPS_POPUP";

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

	private RelativeLayout mGappsIcon;
    private Button mGappsButton;
    private Button mGappsDismissButton;
    private LinearLayout mDevModeUrlContainer;
    private EditText mDevModeUrlEditText;
    private Button mDevModeUrlButton;
	private int mIsDevModeCounter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setupLayout(inflater, view);
	    mDevModeUrlContainer.setVisibility(FairphoneUpdater.DEV_MODE_ENABLED ? View.VISIBLE : View.GONE);
	    mIsDevModeCounter = FairphoneUpdater.DEV_MODE_ENABLED ? 0 : 10;

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

        // gapps
        mGappsButton = (Button) view.findViewById(R.id.install_gapps_button);
        mGappsDismissButton = (Button) view.findViewById(R.id.install_gapps_dismiss_button);
        mGappsIcon = (RelativeLayout) view.findViewById(R.id.gapps_reminder_group);

        // Dev mode
        mDevModeUrlEditText = (EditText)view.findViewById(R.id.dev_mode_url_edit_text);
        mDevModeUrlButton = (Button)view.findViewById(R.id.dev_mode_url_ok_button);
        mDevModeUrlContainer = (LinearLayout)view.findViewById(R.id.dev_mode_url_container);

        mDevModeUrlButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mDevModeUrlEditText.getText().toString();

                // set the URL for the shared prefs
                mainActivity.changeOTADownloadURL(url);

                // download new config
                mainActivity.forceConfigDownload();
            }
        });

    }

    private boolean getGappsInstalationButtonState()
    {
        boolean showGappsGroup = mSharedPreferences.getBoolean(SHARED_PREFERENCES_ENABLE_GAPPS, true);
        boolean gappsNotInstalled = !GappsInstallerHelper.areGappsInstalled();
        boolean hasStoreInfo = getSelectedStoreFromSharedPreferences() != null;
        return  showGappsGroup && gappsNotInstalled && hasStoreInfo;
    }
    
    Store getSelectedStoreFromSharedPreferences()
    {
        return UpdaterData.getInstance().getStore(mSharedPreferences.getInt(FairphoneUpdater.PREFERENCE_SELECTED_STORE_NUMBER, 0));
    }

    private void disableGappsInstalationButton()
    {
        Editor edit = mSharedPreferences.edit();
        edit.putBoolean(SHARED_PREFERENCES_ENABLE_GAPPS, false);

        edit.apply();
    }

    private void setupCurrentVersionGroup(LayoutInflater inflater, View view)
    {
	    LinearLayout mCurrentVersionGroup = (LinearLayout) view.findViewById(R.id.current_version_group);

        View updateGroupView = null;
        if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mainActivity.getDeviceVersion().getImageType()))
        {
            updateGroupView = inflater.inflate(R.layout.fragment_main_update_available_fairphone, mCurrentVersionGroup);
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mainActivity.getDeviceVersion().getImageType()))
        {
            updateGroupView = inflater.inflate(R.layout.fragment_main_update_available_android, mCurrentVersionGroup);
        }
        if (updateGroupView != null)
        {
            updateGroupView.setLayoutParams(mCurrentVersionGroup.getLayoutParams());
        }
    }

    private void enableGappsGroup(boolean showAndEnable)
    {
        if (showAndEnable)
        {
            mGappsIcon.setVisibility(View.VISIBLE);

            mGappsButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    startGappsInstall();
                }
            });

            mGappsDismissButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    mGappsIcon.setVisibility(View.GONE);
                    disableGappsInstalationButton();
                }
            });
        }
        else
        {
            mGappsIcon.setVisibility(View.GONE);
        }
    }

    private void startGappsInstall()
    {
        Fragment gappsFragment = mainActivity.startGappsInstall();
        mainActivity.changeFragment(gappsFragment);
    }

    private void updateOtherOSOptionsGroup()
    {
        if (UpdaterData.getInstance().isFairphoneVersionListNotEmpty() || UpdaterData.getInstance().isAOSPVersionListNotEmpty())
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

    void toogleUpdateAvailableGroup()
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
            if (mUpdateAvailableGroup != null)
            {
                mUpdateAvailableGroup.setVisibility(View.GONE);
            }
            
            if( mVersionUpToDateGroup != null){
                mVersionUpToDateGroup.setVisibility(View.VISIBLE);
            }
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
                VersionDetailFragment fragment = new VersionDetailFragment(true);

                Version latestVersion = mainActivity.getLatestVersion();
                if (latestVersion != null)
                {
                    fragment.setupFragment(latestVersion, getDetailLayoutFromDeviceVersion(latestVersion));
                    mainActivity.changeFragment(fragment);
                }
            }
        });
    }

    private static VersionDetailFragment.DetailLayoutType getDetailLayoutFromDeviceVersion(Version latestVersion)
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

        if (mVersionUpToDateCurrentVersionNameText != null)
        {
            mVersionUpToDateCurrentVersionNameText.setText(currentVersionName);
            mVersionUpToDateCurrentVersionNameText.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onEnableDevMode();
                }
            });
        }

        if (mUpdateAvailableCurrentVersionNameText != null)
        {
            mUpdateAvailableCurrentVersionNameText.setText(currentVersionName);
            mUpdateAvailableCurrentVersionNameText.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onEnableDevMode();
                }
            });
        }
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
        enableGappsGroup(getGappsInstalationButtonState());
    }

    @Override
    public void onPause()
    {
        super.onPause();

        unregisterBroadCastReceiver();
    }

    void setupBroadcastReceiver()
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
                    if (mainActivity.getCurrentUpdaterState() != UpdaterState.DOWNLOAD && mainActivity.getCurrentUpdaterState() != UpdaterState.PREINSTALL)
                    {
                        toogleUpdateAvailableGroup();
                        enableGappsGroup(getGappsInstalationButtonState());
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

	public void onEnableDevMode()
	{
		if (!FairphoneUpdater.DEV_MODE_ENABLED)
		{
			mIsDevModeCounter--;

			Log.d(TAG, "Developer mode in " + mIsDevModeCounter + " Clicks...");

			if (mIsDevModeCounter <= 0)
			{
				FairphoneUpdater.DEV_MODE_ENABLED = true;

				Toast.makeText(mainActivity.getApplicationContext(), getResources().getString(R.string.dev_mode_message), Toast.LENGTH_LONG).show();

				Log.d(TAG, "Developer mode enabled for this session");

				mDevModeUrlContainer.setVisibility(FairphoneUpdater.DEV_MODE_ENABLED ? View.VISIBLE : View.GONE);
				mainActivity.forceConfigDownload();
				//Utils.downloadConfigFile(this, true);
			}
		}
	}
}
