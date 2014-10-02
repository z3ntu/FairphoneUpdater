package com.fairphone.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fairphone.updater.fragments.MainFragment;

public class FairphoneUpdater2Activity extends FragmentActivity {

	private static final String TAG = FairphoneUpdater2Activity.class
			.getSimpleName();

	public static final String FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED = "FairphoneUpdater.NEW.VERSION.RECEIVED";

	public static final String PREFERENCE_CURRENT_UPDATER_STATE = "CurrentUpdaterState";

	private static final String PREFERENCE_DOWNLOAD_ID = "LatestUpdateDownloadId";

	public static final String FAIRPHONE_UPDATER_PREFERENCES = "FairphoneUpdaterPreferences";

	public static final String PREFERENCE_SELECTED_VERSION_NUMBER = "SelectedVersionNumber";

	public static final String PREFERENCE_SELECTED_VERSION_TYPE = "SelectedVersionImageType";

	protected static final String PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD = "SelectedVersionBeginDownload";

	public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED = "FairphoneUpdater.Config.File.Download.FAILED";

	public static final String FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK = "FairphoneUpdater.ConfigFile.Download.LINK";

	public static enum UpdaterState {
		NORMAL, DOWNLOAD, PREINSTALL
	};

	private Version mDeviceVersion;

	private Version mLatestVersion;

	private Version mSelectedVersion;

	private UpdaterState mCurrentState;

	private SharedPreferences mSharedPreferences;

	// private DownloadManager mDownloadManager;
	//
	// private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

	private long mLatestUpdateDownloadId;

	private BroadcastReceiver newVersionbroadcastReceiver;

	public static boolean DEV_MODE_ENABLED;
	private int mIsDevModeCounter;

	private TextView headerMainText;
	private TextView headerFairphoneText;
	private TextView headerAndroidText;

	public static enum HeaderType {
		MAIN, FAIRPHONE, ANDROID
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_updater);

		Crashlytics.start(this);

		DEV_MODE_ENABLED = false;
		mIsDevModeCounter = 10;
		
		isDeviceSupported();
		mSharedPreferences = getSharedPreferences(
				FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

		boolean isConfigLoaded = UpdaterService.readUpdaterData(this);

		// get system data
		mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

		mLatestVersion = isConfigLoaded ? UpdaterData.getInstance()
				.getLatestVersion(mDeviceVersion.getImageType())
				: new Version();

		getSelectedVersionFromSharedPreferences();

		// check current state
		mCurrentState = getCurrentUpdaterState();

		initHeaderViews();

		setupFragments(savedInstanceState);
	}
	
	private void isDeviceSupported() {

		Resources resources = getResources();
		String[] suportedDevices = resources.getString(
				R.string.supportedDevices).split(";");
		String modelWithoutSpaces = Build.MODEL.replaceAll("\\s", "");
		for (String device : suportedDevices) {
			if (modelWithoutSpaces.equalsIgnoreCase(device)) {
				return;
			}
		}
		Toast.makeText(this, R.string.deviceNotSupported, Toast.LENGTH_LONG)
				.show();
		finish();
	}

	protected void getSelectedVersionFromSharedPreferences() {
		String versionImageType = mSharedPreferences.getString(
				PREFERENCE_SELECTED_VERSION_TYPE, "");
		int versionNumber = mSharedPreferences.getInt(
				PREFERENCE_SELECTED_VERSION_NUMBER, 0);
		mSelectedVersion = UpdaterData.getInstance().getVersion(
				versionImageType, versionNumber);
	}

	private UpdaterState getCurrentUpdaterState() {

		String currentState = getStringPreference(PREFERENCE_CURRENT_UPDATER_STATE);

		if (currentState == null || currentState.isEmpty()) {
			currentState = UpdaterState.NORMAL.name();

			Editor editor = mSharedPreferences.edit();

			editor.putString(currentState, currentState);

			editor.commit();
		}

		return UpdaterState.valueOf(currentState);
	}

	public String getStringPreference(String key) {
		return mSharedPreferences.getString(key, null);
	}

	public long getLongPreference(String key) {
		return mSharedPreferences.getLong(key, 0);
	}

	public boolean getBooleanPreference(String key) {
		return mSharedPreferences.getBoolean(key, false);
	}

	public void savePreference(String key, String value) {
		Editor editor = mSharedPreferences.edit();

		editor.putString(key, value);

		editor.commit();
	}

	public void savePreference(String key, boolean value) {
		Editor editor = mSharedPreferences.edit();

		editor.putBoolean(key, value);

		editor.commit();
	}

	public void savePreference(String key, long value) {
		Editor editor = mSharedPreferences.edit();

		editor.putLong(key, value);

		editor.commit();
	}

	private void initHeaderViews() {
		headerMainText = (TextView) findViewById(R.id.header_main_text);
		headerFairphoneText = (TextView) findViewById(R.id.header_fairphone_text);
		headerAndroidText = (TextView) findViewById(R.id.header_android_text);
	}

	public void updateHeader(HeaderType type, String headerText) {

		switch (type) {
		case FAIRPHONE:
			headerMainText.setVisibility(View.GONE);
			headerFairphoneText.setVisibility(View.VISIBLE);
			headerAndroidText.setVisibility(View.GONE);

			headerFairphoneText.setText(headerText);
			break;

		case ANDROID:
			headerMainText.setVisibility(View.GONE);
			headerFairphoneText.setVisibility(View.GONE);
			headerAndroidText.setVisibility(View.VISIBLE);

			headerAndroidText.setText(headerText);
			break;

		case MAIN:
		default:
			headerMainText.setVisibility(View.VISIBLE);
			headerFairphoneText.setVisibility(View.GONE);
			headerAndroidText.setVisibility(View.GONE);
			break;
		}
	}

	private void setupFragments(Bundle savedInstanceState) {
		// Check that the activity is using the layout version with
		// the fragment_container FrameLayout
		if (findViewById(R.id.fragment_holder) != null) {

			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}

			// Create a new Fragment to be placed in the activity layout
			Fragment firstFragment;

			switch (mCurrentState) {
			case DOWNLOAD:
				// TODO Add download fragment
				firstFragment = new Fragment();
				break;
			case PREINSTALL:
				// TODO Add pre-install fragment
				firstFragment = new Fragment();
				break;
			case NORMAL:
			default:
				firstFragment = new MainFragment();
				break;
			}

			// In case this activity was started with special instructions from
			// an
			// Intent, pass the Intent's extras to the fragment as arguments
			firstFragment.setArguments(getIntent().getExtras());

			// Add the fragment to the 'fragment_container' FrameLayout
			getSupportFragmentManager().beginTransaction()
					.add(R.id.fragment_holder, firstFragment).commit();
		}
	}

	public void changeFragment(Fragment newFragment) {

		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.fragment_holder, newFragment);
		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();
	}

	public void onEnableDevMode() {
		if (!DEV_MODE_ENABLED) {
			mIsDevModeCounter--;

			Log.d(TAG, "Developer mode in " + mIsDevModeCounter + " Clicks...");

			if (mIsDevModeCounter <= 0) {
				DEV_MODE_ENABLED = true;

				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.dev_mode_toast),
						Toast.LENGTH_LONG).show();

				Log.d(TAG, "Developer mode enabled for this session");

				FairphoneUpdater2Activity.downloadConfigFile(this);
			}
		}
	}

	private static void downloadConfigFile(Context context) {
		Intent i = new Intent(
				UpdaterService.ACTION_FAIRPHONE_UPDATER_CONFIG_FILE_DOWNLOAD);
		context.sendBroadcast(i);
	}

	public boolean isUpdateAvailable() {
		return mLatestVersion.isNewerVersionThan(mDeviceVersion);
	}

	private String getVersionName(Version version) {
		String versionName = "";
		if (version != null) {
			versionName = version.getImageTypeDescription(getResources()) + " "
					+ version.getName() + " " + version.getBuildNumber();
		}
		return versionName;
	}

	public String getDeviceVersionName() {
		return getVersionName(mDeviceVersion);
	}

	public String getLatestVersionName() {
		return getVersionName(mLatestVersion);
	}

	public String getSelectedVersionName() {
		return getVersionName(mSelectedVersion);
	}
}
