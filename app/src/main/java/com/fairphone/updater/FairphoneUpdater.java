package com.fairphone.updater;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
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

public class FairphoneUpdater extends FragmentActivity
{

    private static final String TAG = FairphoneUpdater.class.getSimpleName();

	private static final String CRASHLYTICS_OPT_IN = "crashlytics_opt_in"; // IMPORTANT: keep synced with Settings.Global.CRASHLYTICS_OPT_IN

    public static final String FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED = "FairphoneUpdater.NEW.VERSION.RECEIVED";

    private static final String PREFERENCE_FIRST_TIME_ANDROID = "FirstTimeAndroid";

    private static final String PREFERENCE_FIRST_TIME_FAIRPHONE = "FirstTimeFairphone";

    private static final String PREFERENCE_FIRST_TIME_APP_STORE = "FirstTimeAppStore";

    public static final String PREFERENCE_CURRENT_UPDATER_STATE = "CurrentUpdaterState";

    private static final String PREFERENCE_DOWNLOAD_ID = "LatestUpdateDownloadId";

    public static final String FAIRPHONE_UPDATER_PREFERENCES = "FairphoneUpdaterPreferences";

    public static final String PREFERENCE_SELECTED_VERSION_NUMBER = "SelectedVersionNumber";

    public static final String PREFERENCE_SELECTED_VERSION_TYPE = "SelectedVersionImageType";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED = "FairphoneUpdater.Config.File.Download.FAILED";

    public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK = "FairphoneUpdater.ConfigFile.Download.LINK";

    public static final String PREFERENCE_SELECTED_STORE_NUMBER = "SelectedStoreNumber";
    
    public static final String PREFERENCE_OTA_DOWNLOAD_URL = "OtaDownloadUrl";

    public static final String PREFERENCE_BETA_MODE = "BetaMode";
    
    private static final String TAG_FIRST_FRAGMENT = "FIRST_FRAGMENT";
    private String mZipPath;
    private AlertDialog internetOffDialog;


	public static enum UpdaterState
    {
        NORMAL, DOWNLOAD, PREINSTALL, ZIP_INSTALL
    }

    private Version mDeviceVersion;
    private Version mLatestVersion;
    private Version mSelectedVersion;

    private UpdaterState mCurrentState;
    private SharedPreferences mSharedPreferences;

    private long mLatestUpdateDownloadId;

    public static boolean DEV_MODE_ENABLED;
    public static boolean BETA_MODE_ENABLED;
    public static String otaDevDownloadUrl = "";

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
    public enum HeaderType
    {
        MAIN_FAIRPHONE, MAIN_ANDROID, MAIN_APP_STORE, FAIRPHONE, ANDROID, OTHER_OS, APP_STORE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mSharedPreferences = getSharedPreferences(FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

        onNewIntent(getIntent());
        
        setContentView(R.layout.activity_updater);

	    if (Settings.Global.getInt(getContentResolver(), CRASHLYTICS_OPT_IN, 0) == 1)
        {
            Log.d(TAG, "Crash reports active.");
            try {
                Crashlytics.start(this);
            } catch(Exception e) {
                Log.w(TAG, "Crashlytics failed to start");
            }
        }

        DEV_MODE_ENABLED = false;

        otaDevDownloadUrl = "";

        // update first times
        mIsFirstTimeAndroid = mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_ANDROID, true);

        mIsFirstTimeFairphone = mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_FAIRPHONE, true);

        mIsFirstTimeAppStore = false;//mSharedPreferences.getBoolean(PREFERENCE_FIRST_TIME_APP_STORE, true);

        String otaDownloadUrl = getResources().getString(R.string.downloadUrl);

        // reset download URL
        Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_OTA_DOWNLOAD_URL, otaDownloadUrl);
        editor.apply();

        // get system data
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

        isDeviceSupported();
        
        setupBetaStatus();
        
        // check current state
        mCurrentState = getCurrentUpdaterState();

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
        if (!isConfigLoaded) {
            mSharedPreferences.edit().remove(UpdaterService.LAST_CONFIG_DOWNLOAD_IN_MS).apply();
        }

        if (mDeviceVersion != null)
        {
            mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : null;
        }
        else
        {
            mLatestVersion = null;
        }

        initHeaderViews();

        setupHeader();

        setupFragments(savedInstanceState);

        startService();
    }


    
    void setupBetaStatus()
    {
        BETA_MODE_ENABLED = mSharedPreferences.getBoolean(PREFERENCE_BETA_MODE, getResources().getBoolean(R.bool.defaultBetaStatus));
    }

    private void isDeviceSupported()
    {
        if(Utils.isDeviceUnsupported(this))
        {
            Toast.makeText(this, R.string.device_not_supported_message, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void getSelectedVersionFromSharedPreferences()
    {
        String versionImageType = mSharedPreferences.getString(PREFERENCE_SELECTED_VERSION_TYPE, "");
        String versionNumber = "0";
        try {
            versionNumber = mSharedPreferences.getString(PREFERENCE_SELECTED_VERSION_NUMBER, "0");
        } catch (ClassCastException e) {
            versionNumber = Integer.toString(mSharedPreferences.getInt(PREFERENCE_SELECTED_VERSION_NUMBER, 0));
        }
        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    void getSelectedStoreFromSharedPreferences()
    {
        String storeNumber = "-1";
        try {
            storeNumber = mSharedPreferences.getString(PREFERENCE_SELECTED_STORE_NUMBER, "-1");
        } catch (ClassCastException e) {
            storeNumber = Integer.toString(mSharedPreferences.getInt(PREFERENCE_SELECTED_STORE_NUMBER, -1));
        }
        mSelectedStore = UpdaterData.getInstance().getStore(storeNumber);
    }

    public UpdaterState getCurrentUpdaterState()
    {

        String currentState = mSharedPreferences.getString(PREFERENCE_CURRENT_UPDATER_STATE, null);

        if (TextUtils.isEmpty(currentState))
        {
            currentState = UpdaterState.NORMAL.name();

            updateStatePreference(UpdaterState.NORMAL);
        }

        return UpdaterState.valueOf(currentState);
    }

    long getLongPreference(String key)
    {
        return mSharedPreferences.getLong(key, 0);
    }

    void savePreference(String key, long value)
    {
        Editor editor = mSharedPreferences.edit();

        editor.putLong(key, value);

        editor.commit();
    }

    public void changeOTADownloadURL(String newUrl){
        otaDevDownloadUrl = TextUtils.isEmpty(newUrl) ? getResources().getString(R.string.downloadUrl) : newUrl;

        Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_OTA_DOWNLOAD_URL, otaDevDownloadUrl);
        editor.commit();
    }

    public void forceConfigDownload(){
        Utils.startUpdaterService(getApplicationContext(), true);
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

    private void setupHeader() {
        if(BETA_MODE_ENABLED) {
            String input = "<br /><small><font color=" + getResources().getColor(R.color.design_pink) + ">" + getResources().getString(R.string.beta_mode) + "</font></small>";
            headerMainFairphoneText.append(Html.fromHtml(input));
            headerMainAndroidText.append(Html.fromHtml(input));
            headerMainAppStoreText.append(Html.fromHtml(input));
        }
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
        if (fragment != null && fragment instanceof DownloadAndRestartFragment && getCurrentUpdaterState() != UpdaterState.NORMAL)
        {
            ((DownloadAndRestartFragment) fragment).abortUpdateProcess("");
        }

        if (fragment != null && TAG_FIRST_FRAGMENT.equals(fragment.getTag()))
        {
            clearSelectedItems();
            finish();
        }
        
        super.onBackPressed();
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

                    editor.putBoolean(PREFERENCE_FIRST_TIME_FAIRPHONE, false);
                    editor.apply();
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

                    editor.putBoolean(PREFERENCE_FIRST_TIME_ANDROID, false);
                    editor.apply();
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

                    editor.putBoolean(PREFERENCE_FIRST_TIME_APP_STORE, false);
                    editor.apply();
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
                headerMainAppStoreText.setVisibility(View.GONE);
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
            Fragment firstFragment = new MainFragment();

            // In case this activity was started with special instructions from
            // an
            // Intent, pass the Intent's extras to the fragment as arguments
            Intent intent = getIntent();
            if (intent != null)
            {
                Bundle bundle = intent.getExtras();
                if (bundle != null)
                {
                    firstFragment.setArguments(bundle);
                }
            }

            // Add the fragment to the 'fragment_container' FrameLayout
            FragmentManager fragManager = getSupportFragmentManager();
            if (fragManager != null)
            {
                FragmentTransaction transation = fragManager.beginTransaction();
                transation.add(R.id.fragment_holder, firstFragment, TAG_FIRST_FRAGMENT);
                transation.addToBackStack(TAG_FIRST_FRAGMENT);
                transation.commit();
            }
            else
            {
                Log.e(TAG, "setupFragments - Couldn't get FragmentManager");
            }
        }
    }

    public Fragment getFragmentFromState()
    {
        Fragment firstFragment;
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
                } else 
                {
                    firstFragment = new MainFragment();
                    updateStatePreference(UpdaterState.NORMAL);
                }
                break;
            case NORMAL:
                if(mLaunchGapps)
                {
                    firstFragment = startGappsInstall();  
                }
                else if (getTopFragment() != null)
                {
                    firstFragment = getTopFragment();
                }
                else
                {
                    firstFragment = new MainFragment();
                }
                break;
            case ZIP_INSTALL:
                firstFragment = new DownloadAndRestartFragment(true);
                break;
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
            if(topFragment != null && newFragment instanceof MainFragment)
            {
                if(TAG_FIRST_FRAGMENT.equals(topFragment.getTag()))
                {
                    return;
                }
            }
            
            FragmentManager fragManager = getSupportFragmentManager();
            if (fragManager != null)
            {
                FragmentTransaction transaction = fragManager.beginTransaction();
                // Replace whatever is in the fragment_container view with this
                // fragment,
                // and add the transaction to the back stack so the user can
                // navigate
                // back
				transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
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

    Fragment getTopFragment()
    {
        Fragment topFragment = null;
        FragmentManager fragManager = getSupportFragmentManager();
        if (fragManager != null)
        {
            topFragment = fragManager.findFragmentById(R.id.fragment_holder);
        }
        else
        {
            Log.e(TAG, "getTopFragment - Couldn't get FragmentManager");
        }
        return topFragment;
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

    public String getItemName(DownloadableItem item, boolean isVersion)
    {
        String itemName = "";
        if (item != null)
        {
            if(isVersion)
            {
                itemName = ((Version) item).getHumanReadableName();
            }
            else
            {
                itemName = getStoreName((Store) item);
            }
        }
        return itemName;
    }



    public static String getStoreName(Store store)
    {
        String itemName = "";
        if (store != null)
        {
            itemName = store.getName();
        }
        return itemName;
    }

    public String getDeviceVersionName()
    {
        return mDeviceVersion.getHumanReadableName();
    }

    public String getLatestVersionName()
    {
        return mLatestVersion.getHumanReadableName();
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

    public static HeaderType getHeaderTypeFromImageType(String imageType)
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
        String versionNumber = selectedVersion != null ? selectedVersion.getId() : "0";
        String versionImageType = selectedVersion != null ? selectedVersion.getImageType() : "";

        clearSelectedVersion(versionNumber, versionImageType);

        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
        clearSelectedStore("-1");
    }

    public void clearSelectedItems()
    {
        clearSelectedVersion("0", "");
        clearSelectedStore("-1");
    }

    private void clearSelectedVersion(String versionNumber, String versionImageType)
    {
        Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_SELECTED_VERSION_NUMBER, versionNumber);
        editor.putString(PREFERENCE_SELECTED_VERSION_TYPE, versionImageType);
        editor.commit();

        mSelectedVersion = null;
    }

    public void setSelectedStore(Store selectedStore)
    {
        String storeNumber = selectedStore != null ? selectedStore.getId() : "-1";

        clearSelectedStore(storeNumber);

        mSelectedStore = UpdaterData.getInstance().getStore(storeNumber);
        clearSelectedVersion("0", "");
    }

    private void clearSelectedStore(String storeNumber)
    {
        Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_SELECTED_STORE_NUMBER, storeNumber);
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

    private boolean mLaunchGapps = false;

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);
        mLaunchGapps = false;

        if(intent != null) {
            String action =  TextUtils.isEmpty(intent.getAction()) ? "" : intent.getAction();
            switch (action)
            {
                case GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL:
                    if (checkStartGappsInstall(intent)) {
                        mLaunchGapps = true;
                    } else {
                        Intent parentIntent = getParentActivityIntent();
                        if (checkStartGappsInstall(parentIntent)) {
                            mLaunchGapps = true;
                        }
                    }
                    break;
                case Intent.ACTION_VIEW:
                    Uri data = intent.getData();
                    if(data != null)
                    {

                        String zipPath = Utils.getPath(this, data);
                        if(!TextUtils.isEmpty(zipPath)) {
                            mZipPath = zipPath;
                            updateStatePreference(UpdaterState.ZIP_INSTALL);
                        }
                        else
                        {
                            mZipPath = "";
                        }
                    }
                    break;
                default:
                    //no action
                    break;
            }
        }
    }

    private static boolean checkStartGappsInstall(Intent intent)
    {
        return intent != null && GappsInstallerHelper.EXTRA_START_GAPPS_INSTALL.equals(intent.getAction());
    }

	@Override
	protected void onPause() {
		super.onPause();
		if(internetOffDialog != null) {
            internetOffDialog.cancel();
            internetOffDialog = null;
		}
	}

	@Override
    protected void onResume()
    {
        super.onResume();

        // check current state
        mCurrentState = getCurrentUpdaterState();
        if(mCurrentState == UpdaterState.ZIP_INSTALL)
        {
            updateStatePreference(TextUtils.isEmpty(mZipPath) ? UpdaterState.NORMAL : UpdaterState.ZIP_INSTALL);
        }

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
        
        changeFragment(getFragmentFromState());

        // Show internet disable dialog if in a blank state and no internet is available
        if(     internetOffDialog == null &&
                mCurrentState == UpdaterState.NORMAL &&
                !Utils.isInternetEnabled(this) &&
                UpdaterData.getInstance().isAppStoreListEmpty() &&
			    !UpdaterData.getInstance().isAOSPVersionListNotEmpty() &&
			    !UpdaterData.getInstance().isFairphoneVersionListNotEmpty())
        {
            Resources resources = getResources();

            AlertDialog.Builder internetOffDialogBuilder = new AlertDialog.Builder(this);

            internetOffDialogBuilder.setTitle(R.string.connect_to_internet);
            internetOffDialogBuilder.setIcon(resources.getDrawable(R.drawable.ic_import_export_fpblue_24dp));

            internetOffDialogBuilder.setPositiveButton(resources.getString(R.string.got_it), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				    // do nothing, since the state is still the same
			    }
		    });
            internetOffDialog = internetOffDialogBuilder.create();
            internetOffDialog.show();
        }
    }

    public Fragment startGappsInstall()
    {
        mSelectedStore = Utils.getGappsStore();

        VersionDetailFragment fragment = new VersionDetailFragment(false);

        if (mSelectedStore != null)
        {
            fragment.setupAppStoreFragment(mSelectedStore);
        }
        else
        {
            fragment = null;
        }
        
        return fragment;
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

    public void updateStatePreference(UpdaterState newState)
    {
        mCurrentState = newState;

        Editor editor = mSharedPreferences.edit();

        editor.putString(PREFERENCE_CURRENT_UPDATER_STATE, mCurrentState.name());

        editor.commit();
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
    
    public void clearConfigFileDownloadId()
    {
        savePreference(UpdaterService.PREFERENCE_LAST_CONFIG_DOWNLOAD_ID, 0L);
    }

    public String getZipFilePath()
    {
        return mZipPath;
    }

    public String getPreferenceOtaDownloadUrl()
    {
        return mSharedPreferences.getString(FairphoneUpdater.PREFERENCE_OTA_DOWNLOAD_URL, getResources().getString(R.string.downloadUrl));
    }
}
