package com.fairphone.updater;

import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fairphone.updater.fragments.DownloadAndRestartFragment;
import com.fairphone.updater.fragments.MainFragment;
import com.fairphone.updater.tools.Utils;

public class FairphoneUpdater2Activity extends FragmentActivity
{

    private static final String TAG = FairphoneUpdater2Activity.class.getSimpleName();

    public static final String FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED = "FairphoneUpdater.NEW.VERSION.RECEIVED";

    public static final String PREFERENCE_CURRENT_UPDATER_STATE = "CurrentUpdaterState";

    private static final String PREFERENCE_DOWNLOAD_ID = "LatestUpdateDownloadId";

    public static final String FAIRPHONE_UPDATER_PREFERENCES = "FairphoneUpdaterPreferences";

    public static final String PREFERENCE_SELECTED_VERSION_NUMBER = "SelectedVersionNumber";

    public static final String PREFERENCE_SELECTED_VERSION_TYPE = "SelectedVersionImageType";

    protected static final String PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD = "SelectedVersionBeginDownload";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED = "FairphoneUpdater.Config.File.Download.FAILED";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK = "FairphoneUpdater.ConfigFile.Download.LINK";

    public static enum UpdaterState
    {
        NORMAL, DOWNLOAD, PREINSTALL
    };

    private Version mDeviceVersion;
    private Version mLatestVersion;
    private Version mSelectedVersion;

    private UpdaterState mCurrentState;
    private SharedPreferences mSharedPreferences;

    private long mLatestUpdateDownloadId;

    public static boolean DEV_MODE_ENABLED;
    private int mIsDevModeCounter;

    private TextView headerMainFairphoneText;
    private TextView headerMainAndroidText;
    private TextView headerFairphoneText;
    private TextView headerAndroidText;
    private TextView headerOtherOSText;

    public static enum HeaderType
    {
        MAIN_FAIRPHONE, MAIN_ANDROID, FAIRPHONE, ANDROID, OTHER_OS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updater);

        Crashlytics.start(this);

        DEV_MODE_ENABLED = false;
        mIsDevModeCounter = 10;

        isDeviceSupported();
        mSharedPreferences = getSharedPreferences(FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);

        // get system data
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

        mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : new Version();

        getSelectedVersionFromSharedPreferences();

        // check current state
        mCurrentState = getCurrentUpdaterState();

        initHeaderViews();

        setupFragments(savedInstanceState);
    }

    private void isDeviceSupported()
    {

        Resources resources = getResources();
        String[] suportedDevices = resources.getString(R.string.supportedDevices).split(";");
        String modelWithoutSpaces = Build.MODEL.replaceAll("\\s", "");
        for (String device : suportedDevices)
        {
            if (modelWithoutSpaces.equalsIgnoreCase(device))
            {
                return;
            }
        }
        Toast.makeText(this, R.string.deviceNotSupported, Toast.LENGTH_LONG).show();
        finish();
    }

    protected void getSelectedVersionFromSharedPreferences()
    {
        String versionImageType = mSharedPreferences.getString(PREFERENCE_SELECTED_VERSION_TYPE, "");
        int versionNumber = mSharedPreferences.getInt(PREFERENCE_SELECTED_VERSION_NUMBER, 0);
        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    public UpdaterState getCurrentUpdaterState()
    {

        String currentState = getStringPreference(PREFERENCE_CURRENT_UPDATER_STATE);

        if (currentState == null || currentState.isEmpty())
        {
            currentState = UpdaterState.NORMAL.name();

            Editor editor = mSharedPreferences.edit();

            editor.putString(currentState, currentState);

            editor.commit();
        }

        return UpdaterState.valueOf(currentState);
    }

    public String getStringPreference(String key)
    {
        return mSharedPreferences.getString(key, null);
    }

    public long getLongPreference(String key)
    {
        return mSharedPreferences.getLong(key, 0);
    }

    public boolean getBooleanPreference(String key)
    {
        return mSharedPreferences.getBoolean(key, false);
    }

    public void savePreference(String key, String value)
    {
        Editor editor = mSharedPreferences.edit();

        editor.putString(key, value);

        editor.commit();
    }

    public void savePreference(String key, boolean value)
    {
        Editor editor = mSharedPreferences.edit();

        editor.putBoolean(key, value);

        editor.commit();
    }

    public void savePreference(String key, long value)
    {
        Editor editor = mSharedPreferences.edit();

        editor.putLong(key, value);

        editor.commit();
    }

    private void initHeaderViews()
    {
        headerMainFairphoneText = (TextView) findViewById(R.id.header_main_fairphone_text);
        headerMainAndroidText = (TextView) findViewById(R.id.header_main_android_text);
        headerFairphoneText = (TextView) findViewById(R.id.header_fairphone_text);
        headerAndroidText = (TextView) findViewById(R.id.header_android_text);
        headerOtherOSText = (TextView) findViewById(R.id.header_other_os_text);

        OnClickListener headerBackPressListener = new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        };

        headerFairphoneText.setOnClickListener(headerBackPressListener);
        headerAndroidText.setOnClickListener(headerBackPressListener);
        headerOtherOSText.setOnClickListener(headerBackPressListener);
    }

    public void updateHeader(HeaderType type, String headerText)
    {

        switch (type)
        {
            case FAIRPHONE:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.VISIBLE);
                headerAndroidText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerFairphoneText.setText(headerText);
                break;

            case ANDROID:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.VISIBLE);
                headerOtherOSText.setVisibility(View.GONE);

                headerAndroidText.setText(headerText);
                break;

            case OTHER_OS:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.VISIBLE);

                headerOtherOSText.setText(headerText);
                break;

            case MAIN_ANDROID:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.VISIBLE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);
                break;

            case MAIN_FAIRPHONE:
            default:
                headerMainFairphoneText.setVisibility(View.VISIBLE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);
                break;
        }
    }

    private void setupFragments(Bundle savedInstanceState)
    {
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_holder) != null)
        {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null)
            {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            Fragment firstFragment = getFragmentFromState();

            // In case this activity was started with special instructions from
            // an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_holder, firstFragment).commit();
        }
    }

    public Fragment getFragmentFromState()
    {
        Fragment firstFragment;
        switch (mCurrentState)
        {
            case PREINSTALL:
            case DOWNLOAD:
                firstFragment = new DownloadAndRestartFragment();
                break;
            case NORMAL:
            default:
                firstFragment = new MainFragment();
                break;
        }
        return firstFragment;
    }

    public void changeFragment(Fragment newFragment)
    {

        Fragment topFragment = getTopFragment();
        if (topFragment == null || (topFragment != null && !newFragment.getClass().equals(topFragment.getClass())))
        {

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            // Replace whatever is in the fragment_container view with this
            // fragment,
            // and add the transaction to the back stack so the user can
            // navigate
            // back
            //            transaction.setCustomAnimations(R.animator.fade_in_fragment, R.animator.fade_out_fragment, R.animator.fade_in_fragment,
            //                    R.animator.fade_out_fragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.fragment_holder, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
    }

    public void removeLastFragment()
    {
        getSupportFragmentManager().popBackStackImmediate();
    }

    public Fragment getTopFragment()
    {
        List<Fragment> allFragments = getSupportFragmentManager().getFragments();
        Fragment topFragment = null;
        if (allFragments != null)
        {
            topFragment = allFragments.get(allFragments.size() - 1);
        }

        return topFragment;
    }

    public void onEnableDevMode()
    {
        if (!DEV_MODE_ENABLED)
        {
            mIsDevModeCounter--;

            Log.d(TAG, "Developer mode in " + mIsDevModeCounter + " Clicks...");

            if (mIsDevModeCounter <= 0)
            {
                DEV_MODE_ENABLED = true;

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dev_mode_toast), Toast.LENGTH_LONG).show();

                Log.d(TAG, "Developer mode enabled for this session");

                Utils.downloadConfigFile(this);
            }
        }
    }

    public boolean isUpdateAvailable()
    {
        return mLatestVersion.isNewerVersionThan(mDeviceVersion);
    }

    public String getVersionName(Version version)
    {
        String versionName = "";
        if (version != null)
        {
            versionName = version.getImageTypeDescription(getResources()) + " " + version.getName() + " " + version.getBuildNumber();
        }
        return versionName;
    }

    public String getDeviceVersionName()
    {
        return getVersionName(mDeviceVersion);
    }

    public String getLatestVersionName()
    {
        return getVersionName(mLatestVersion);
    }

    public String getSelectedVersionName()
    {
        return getVersionName(mSelectedVersion);
    }

    public Version getDeviceVersion()
    {
        return mDeviceVersion;
    }

    public Version getLatestVersion()
    {
        return mLatestVersion;
    }

    public Version getSelectedVersion()
    {
        return mSelectedVersion;
    }

    public HeaderType getHeaderTypeFromImageType(String imageType)
    {
        HeaderType type = HeaderType.MAIN_FAIRPHONE;
        if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(imageType))
        {
            type = HeaderType.ANDROID;
        }
        else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(imageType))
        {
            type = HeaderType.FAIRPHONE;
        }
        return type;
    }

    public void setSelectedVersion(Version selectedVersion)
    {
        int versionNumber = selectedVersion != null ? selectedVersion.getNumber() : 0;
        String versionImageType = selectedVersion != null ? selectedVersion.getImageType() : "";

        Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_SELECTED_VERSION_NUMBER, versionNumber);
        editor.putString(PREFERENCE_SELECTED_VERSION_TYPE, versionImageType);
        editor.commit();

        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    public void resetLastUpdateDownloadId()
    {
        mLatestUpdateDownloadId = 0;
        savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);
        setSelectedVersion(null);
    }

    private Version getLatestVersionFromConfig()
    {
        Version latest = UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType());
        return latest;
    }

    public void updateLatestVersionFromConfig()
    {
        mLatestVersion = getLatestVersionFromConfig();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // check current state
        mCurrentState = getCurrentUpdaterState();

        if (mCurrentState == UpdaterState.NORMAL)
        {
            Utils.startUpdaterService(this, true);
        }

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);
        mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : new Version();

        getSelectedVersionFromSharedPreferences();

        // if (mSharedPreferences
        // .getBoolean(
        // FairphoneUpdater2Activity.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD,
        // false)) {
        // Editor editor = mSharedPreferences.edit();
        // editor.putBoolean(
        // FairphoneUpdater2Activity.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD,
        // false);
        // editor.commit();
        // startUpdateDownload();
        // } else {
        // changeFragment(getFragmentFromState());
        // }

        changeFragment(getFragmentFromState());
    }

    public void changeState(UpdaterState newState)
    {
        updateStatePreference(newState);
        changeFragment(getFragmentFromState());
    }

    public void updateStatePreference(UpdaterState newState)
    {
        mCurrentState = newState;

        Editor editor = mSharedPreferences.edit();

        editor.putString(PREFERENCE_CURRENT_UPDATER_STATE, mCurrentState.name());

        editor.commit();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

    }

    public long getLatestDownloadId()
    {
        return mLatestUpdateDownloadId;
    }

    public long getLatestUpdateDownloadIdFromSharedPreference()
    {
        if (mLatestUpdateDownloadId == 0)
        {
            mLatestUpdateDownloadId = getLongPreference(PREFERENCE_DOWNLOAD_ID);
        }
        return mLatestUpdateDownloadId;
    }

    public void saveLatestUpdateDownloadId(long latestUpdateDownloadId)
    {
        mLatestUpdateDownloadId = latestUpdateDownloadId;
        savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);
    }
}
