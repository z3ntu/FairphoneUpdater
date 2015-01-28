package com.fairphone.updater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.fairphone.updater.fragments.DownloadAndRestartFragment;
import com.fairphone.updater.fragments.InfoPopupDialog;
import com.fairphone.updater.fragments.MainFragment;
import com.fairphone.updater.fragments.VersionDetailFragment;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;
import com.fairphone.updater.tools.Utils;

import java.util.List;

public class FairphoneUpdater extends FragmentActivity
{

    private static final String TAG = FairphoneUpdater.class.getSimpleName();

    public static final String FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED = "FairphoneUpdater.NEW.VERSION.RECEIVED";

    public static final String PREFERENCE_FIRST_TIME_ANDROID = "FirstTimeAndroid";

    public static final String PREFERENCE_FIRST_TIME_FAIRPHONE = "FirstTimeFairphone";

    public static final String PREFERENCE_FIRST_TIME_APP_STORE = "FirstTimeAppStore";

    public static final String PREFERENCE_CURRENT_UPDATER_STATE = "CurrentUpdaterState";

    private static final String PREFERENCE_DOWNLOAD_ID = "LatestUpdateDownloadId";

    public static final String FAIRPHONE_UPDATER_PREFERENCES = "FairphoneUpdaterPreferences";

    public static final String PREFERENCE_SELECTED_VERSION_NUMBER = "SelectedVersionNumber";

    public static final String PREFERENCE_SELECTED_VERSION_TYPE = "SelectedVersionImageType";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED = "FairphoneUpdater.Config.File.Download.FAILED";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK = "FairphoneUpdater.ConfigFile.Download.LINK";

    public static final String PREFERENCE_SELECTED_STORE_NUMBER = "SelectedStoreNumber";
    
    public static final String PREFERENCE_OTA_DOWNLOAD_URL = "OtaDownloadUrl";


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
    
    public String OTA_DOWNLOAD_URL;

    private TextView headerMainFairphoneText;
    private TextView headerMainAndroidText;
    private TextView headerFairphoneText;
    private TextView headerAndroidText;
    private TextView headerAppStoreText;
    private TextView headerOtherOSText;
    private ImageButton headerFairphoneInfoButton;
    private ImageButton headerAndroidInfoButton;

    private boolean mIsFirstTimeAndroid;
    private boolean mIsFirstTimeFairphone;
    private boolean mIsFirstTimeAppStore;

    private Store mSelectedStore;

    private TextView headerMainAppStoreText;
    public static enum HeaderType
    {
        MAIN_FAIRPHONE, MAIN_ANDROID, MAIN_APP_STORE, FAIRPHONE, ANDROID, OTHER_OS, APP_STORE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updater);

        Crashlytics.start(this);

        DEV_MODE_ENABLED = false;
        mIsDevModeCounter = 10;

        mSharedPreferences = getSharedPreferences(FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

        // update first times
        mIsFirstTimeAndroid = mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_ANDROID, true);

        mIsFirstTimeFairphone = mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_FAIRPHONE, true);

        mIsFirstTimeAppStore = false;//mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_APP_STORE, true);
        
        OTA_DOWNLOAD_URL = mSharedPreferences.getString(PREFERENCE_OTA_DOWNLOAD_URL, getResources().getString(R.string.downloadUrl));

        // get system data
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

        isDeviceSupported();
        
        // check current state
        mCurrentState = getCurrentUpdaterState();

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
        if (!isConfigLoaded)
            mSharedPreferences.edit().remove(UpdaterService.LAST_CONFIG_DOWNLOAD_IN_MS).commit();

        startService();

        if (mDeviceVersion != null)
        {
            mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : null;
        }
        else
        {
            mLatestVersion = null;
        }

        getSelectedVersionFromSharedPreferences();
        getSelectedStoreFromSharedPreferences();

        initHeaderViews();

        setupFragments(savedInstanceState);
    }

    private void isDeviceSupported()
    {
        if(!Utils.isDeviceSupported(this))
        {
            Toast.makeText(this, R.string.device_not_supported_message, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected void getSelectedVersionFromSharedPreferences()
    {
        String versionImageType = mSharedPreferences.getString(PREFERENCE_SELECTED_VERSION_TYPE, "");
        int versionNumber = mSharedPreferences.getInt(PREFERENCE_SELECTED_VERSION_NUMBER, 0);
        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    protected void getSelectedStoreFromSharedPreferences()
    {
        int storeNumber = mSharedPreferences.getInt(PREFERENCE_SELECTED_STORE_NUMBER, 0);
        mSelectedStore = UpdaterData.getInstance().getStore(storeNumber);
    }

    public UpdaterState getCurrentUpdaterState()
    {

        String currentState = getStringPreference(PREFERENCE_CURRENT_UPDATER_STATE);

        if (TextUtils.isEmpty(currentState))
        {
            currentState = UpdaterState.NORMAL.name();

            updateStatePreference(UpdaterState.NORMAL);
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
        headerMainAppStoreText = (TextView) findViewById(R.id.header_main_app_store_text);
        headerFairphoneText = (TextView) findViewById(R.id.header_fairphone_text);
        headerAndroidText = (TextView) findViewById(R.id.header_android_text);
        headerOtherOSText = (TextView) findViewById(R.id.header_other_os_text);
        headerAppStoreText = (TextView) findViewById(R.id.header_app_store_text);

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
        headerAppStoreText.setOnClickListener(headerBackPressListener);
        headerOtherOSText.setOnClickListener(headerBackPressListener);

        headerFairphoneInfoButton = (ImageButton) findViewById(R.id.header_fairphone_info_button);
        headerAndroidInfoButton = (ImageButton) findViewById(R.id.header_android_info_button);

        headerFairphoneInfoButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                showInfoPopupDialog(DetailLayoutType.FAIRPHONE);
            }
        });

        headerAndroidInfoButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                showInfoPopupDialog(DetailLayoutType.ANDROID);
            }
        });
    }

    private void showInfoPopupDialog(DetailLayoutType layoutType)
    {
        FragmentManager fm = getSupportFragmentManager();
        InfoPopupDialog popupDialog = new InfoPopupDialog(layoutType);
        popupDialog.show(fm, layoutType.name());
    }

    @Override
    public void onBackPressed()
    {
        Fragment fragment = getTopFragment();
        if (fragment != null && fragment instanceof DownloadAndRestartFragment && !getCurrentUpdaterState().equals(UpdaterState.NORMAL))
        {
            ((DownloadAndRestartFragment) fragment).abortUpdateProcess();
        }
        else if (fragment != null && fragment instanceof MainFragment)
        {
            clearSelectedItems();
            clearBackStack();
            finish();
        }
        else
        {
            super.onBackPressed();
        }
    }

    public void updateHeader(HeaderType type, String headerText, boolean showInfo)
    {

        switch (type)
        {
            case FAIRPHONE:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerMainAppStoreText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.VISIBLE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerFairphoneText.setText(headerText);

                if (showInfo && mIsFirstTimeFairphone)
                {
                    showInfoPopupDialog(DetailLayoutType.UPDATE_FAIRPHONE);
                    Editor editor = mSharedPreferences.edit();

                    mIsFirstTimeFairphone = false;

                    editor.putBoolean(PREFERENCE_FIRST_TIME_FAIRPHONE, mIsFirstTimeFairphone);
                    editor.commit();
                }

                headerFairphoneInfoButton.setVisibility(showInfo ? View.VISIBLE : View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
                break;

            case ANDROID:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerMainAppStoreText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.VISIBLE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerAndroidText.setText(headerText);

                if (showInfo && mIsFirstTimeAndroid)
                {
                    showInfoPopupDialog(DetailLayoutType.UPDATE_ANDROID);
                    Editor editor = mSharedPreferences.edit();

                    mIsFirstTimeAndroid = false;

                    editor.putBoolean(PREFERENCE_FIRST_TIME_ANDROID, mIsFirstTimeAndroid);
                    editor.commit();
                }

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(showInfo ? View.VISIBLE : View.GONE);
                break;

            case APP_STORE:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerMainAppStoreText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.VISIBLE);
                headerOtherOSText.setVisibility(View.GONE);
                headerAppStoreText.setText(headerText);

                if (showInfo && mIsFirstTimeAppStore)
                {
                    showInfoPopupDialog(DetailLayoutType.APP_STORE);
                    Editor editor = mSharedPreferences.edit();

                    mIsFirstTimeAppStore = false;

                    editor.putBoolean(PREFERENCE_FIRST_TIME_APP_STORE, mIsFirstTimeAppStore);
                    editor.commit();
                }

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
                break;

            case OTHER_OS:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerMainAppStoreText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.VISIBLE);

                headerOtherOSText.setText(headerText);

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
                break;

            case MAIN_ANDROID:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.VISIBLE);
                headerMainAppStoreText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
                break;
                
            case MAIN_APP_STORE:
                headerMainFairphoneText.setVisibility(View.GONE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerMainAppStoreText.setVisibility(View.VISIBLE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
                break;

            case MAIN_FAIRPHONE:
            default:
                headerMainFairphoneText.setVisibility(View.VISIBLE);
                headerMainAndroidText.setVisibility(View.GONE);
                headerFairphoneText.setVisibility(View.GONE);
                headerAndroidText.setVisibility(View.GONE);
                headerAppStoreText.setVisibility(View.GONE);
                headerOtherOSText.setVisibility(View.GONE);

                headerFairphoneInfoButton.setVisibility(View.GONE);
                headerAndroidInfoButton.setVisibility(View.GONE);
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
            Intent intent = getIntent();
            if (firstFragment != null && intent != null)
            {
                Bundle bundle = intent.getExtras();
                if (bundle != null)
                {
                    firstFragment.setArguments(bundle);
                }
            }

            // Add the fragment to the 'fragment_container' FrameLayout
            FragmentManager fragManager = getSupportFragmentManager();
            if (firstFragment != null && fragManager != null)
            {
                fragManager.beginTransaction().add(R.id.fragment_holder, firstFragment).commit();
            }
            else
            {
                Log.e(TAG, "setupFragments - Couldn't get FragmentManager");
            }
        }
    }

    public Fragment getFragmentFromState()
    {
        Fragment firstFragment = null;
        switch (mCurrentState)
        {
            case PREINSTALL:
            case DOWNLOAD:
                if (mSelectedVersion != null)
                {
                    firstFragment = new DownloadAndRestartFragment(true);
                }
                else if (mSelectedStore != null)
                {
                    firstFragment = new DownloadAndRestartFragment(false);
                } else {
                    firstFragment = new MainFragment();
                    updateStatePreference(UpdaterState.NORMAL);
                }
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
        if ( newFragment != null && ( topFragment == null || !newFragment.getClass().equals(topFragment.getClass())) )
        {
            FragmentManager fragManager = getSupportFragmentManager();
            if (fragManager != null)
            {
                FragmentTransaction transaction = fragManager.beginTransaction();
                // Replace whatever is in the fragment_container view with this
                // fragment,
                // and add the transaction to the back stack so the user can
                // navigate
                // back
                // transaction.setCustomAnimations(R.animator.fade_in_fragment,
                // R.animator.fade_out_fragment, R.animator.fade_in_fragment,
                // R.animator.fade_out_fragment);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.fragment_holder, newFragment);
                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
            }
            else
            {
                Log.e(TAG, "changeFragment - Couldn't get FragmentManager");
            }
        }
    }

    public void removeLastFragment(final boolean forceFinish)
    {
 
                FragmentManager fragManager = getSupportFragmentManager();
                if (fragManager != null)
                {
                    fragManager.executePendingTransactions();
                    boolean popSuccess = fragManager.popBackStackImmediate();
                    if (forceFinish && !popSuccess)
                    {
                        finish();
                    }
                }
                else
                {
                    Log.e(TAG, "removeLastFragment - Couldn't get FragmentManager");
                }

    }

    public int getFragmentCount()
    {
        int listSize = 0;
        FragmentManager fragManager = getSupportFragmentManager();
        if (fragManager != null)
        {
            List<Fragment> allFragments = fragManager.getFragments();
            if (allFragments != null)
            {
                listSize = allFragments.size();
            }
        }
        else
        {
            Log.e(TAG, "getFragmentCount - Couldn't get FragmentManager");
        }

        Log.d(TAG, "Fragment list size: " + listSize);
        return listSize;
    }

    public int getBackStackSize()
    {
        int backStackSize = 0;
        FragmentManager fragManager = getSupportFragmentManager();
        if (fragManager != null)
        {
            backStackSize = fragManager.getBackStackEntryCount();
        }
        else
        {
            Log.e(TAG, "getBackStackSize - Couldn't get FragmentManager");
        }

        Log.d(TAG, "Back stack size: " + backStackSize);
        return backStackSize;
    }

    public void clearBackStack()
    {
        FragmentManager fragManager = getSupportFragmentManager();
        if (fragManager != null)
        {
            int backStackSize = fragManager.getBackStackEntryCount();

            for (int i = 0; i < backStackSize; i++)
            {
                removeLastFragment(false);
            }
        }
        else
        {
            Log.e(TAG, "clearBackStack - Couldn't get FragmentManager");
        }
    }

    public Fragment getTopFragment()
    {
        Fragment topFragment = null;
        FragmentManager fragManager = getSupportFragmentManager();
        if (fragManager != null)
        {
            return fragManager.findFragmentByTag(null);
        }
        else
        {
            Log.e(TAG, "getTopFragment - Couldn't get FragmentManager");
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

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dev_mode_message), Toast.LENGTH_LONG).show();

                Log.d(TAG, "Developer mode enabled for this session");

                Utils.downloadConfigFile(this, true);
            }
        }
    }

    public boolean isUpdateAvailable()
    {
        boolean update = false;
        if (mLatestVersion != null)
        {
            update = mLatestVersion.isNewerVersionThan(mDeviceVersion);
        }
        return update;
    }

    public String getItemName(DownloadableItem item)
    {
        String itemName = "";
        if (item != null)
        {
            if (item instanceof Version)
            {
                Version version = (Version) item;
                itemName = version.getImageTypeDescription(getResources()) + " " + version.getName() + " " + version.getBuildNumber();
            }
            else if (item instanceof Store)
            {

                Store store = (Store) item;
                itemName = store.getName();
            }
        }
        return itemName;
    }

    public String getDeviceVersionName()
    {
        return getItemName(mDeviceVersion);
    }

    public String getLatestVersionName()
    {
        return getItemName(mLatestVersion);
    }

    public String getSelectedVersionName()
    {
        return getItemName(mSelectedVersion);
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

    public Store getSelectedStore()
    {
        return mSelectedStore;
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

        clearSelectedVersion(versionNumber, versionImageType);

        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
        clearSelectedStore(0);
    }

    public void clearSelectedItems()
    {
        clearSelectedVersion(0, "");
        clearSelectedStore(0);
    }

    private void clearSelectedVersion(int versionNumber, String versionImageType)
    {
        Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_SELECTED_VERSION_NUMBER, versionNumber);
        editor.putString(PREFERENCE_SELECTED_VERSION_TYPE, versionImageType);
        editor.commit();

        mSelectedVersion = null;
    }

    public void setSelectedStore(Store selectedStore)
    {
        int storeNumber = selectedStore != null ? selectedStore.getNumber() : 0;

        clearSelectedStore(storeNumber);

        mSelectedStore = UpdaterData.getInstance().getStore(storeNumber);
        clearSelectedVersion(0, "");
    }

    private void clearSelectedStore(int storeNumber)
    {
        Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_SELECTED_STORE_NUMBER, storeNumber);
        editor.commit();

        mSelectedStore = null;
    }

    public void resetLastUpdateDownloadId()
    {
        mLatestUpdateDownloadId = 0;
        savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);
        setSelectedVersion(null);
    }

    private Version getLatestVersionFromConfig()
    {
        Version latest = null;
        if (mDeviceVersion != null)
        {
            latest = UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType());
        }
        return latest;
    }

    public void updateLatestVersionFromConfig()
    {
        mLatestVersion = getLatestVersionFromConfig();
    }

    boolean mLaunchGapps = false;

    @Override
    protected void onNewIntent(Intent intent)
    {
        // super.onNewIntent(intent);
        mLaunchGapps = false;

        if (intent.getAction().equals(GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL))
        {
            mLaunchGapps = true;
        }
        else
        {
            intent = getParentActivityIntent();
            if (intent != null && intent.getAction().equals(GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL))
            {
                mLaunchGapps = true;
            }
        }

        if (mLaunchGapps)
        {
            getSelectedStoreFromSharedPreferences();
            startGappsInstall();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Intent i = getIntent();
        //
        // // check current state
        mCurrentState = getCurrentUpdaterState();

        startService();
        //
        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

        if (mDeviceVersion != null)
        {
            mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : null;
        }
        else
        {
            mLatestVersion = null;
        }

        getSelectedVersionFromSharedPreferences();
        getSelectedStoreFromSharedPreferences();
        //
        //
        // if (i != null &&
        // i.getBooleanExtra(GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL,
        // false))
        // {
        // startGappsInstall();
        // }
        // else
        // {
        if (mLaunchGapps)
        {
            startGappsInstall();
        }
//        else
//        {
//            changeFragment(getFragmentFromState());
//        }
    }

    public void startGappsInstall()
    {
        getSelectedStoreFromSharedPreferences();

        VersionDetailFragment fragment = new VersionDetailFragment(false);

        if (mSelectedStore != null)
        {
            fragment.setupFragment(mSelectedStore, DetailLayoutType.APP_STORE);
            changeFragment(fragment);
        }
    }

    private void startService()
    {
        if (mCurrentState == UpdaterState.NORMAL)
        {
            Utils.startUpdaterService(this, true);
        }
        else
        {
            Utils.startUpdaterService(this, false);
        }
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
    
    public long getConfigFileDownloadIdFromSharedPreference()
    {
        return getLongPreference(UpdaterService.PREFERENCE_LAST_CONFIG_DOWNLOAD_ID);
    }
    
    public void saveConfigFileDownloadId(long latestUpdateDownloadId)
    {
        savePreference(UpdaterService.PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, latestUpdateDownloadId);
    }
}
