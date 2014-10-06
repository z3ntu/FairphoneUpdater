package com.fairphone.updater;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fairphone.updater.fragments.DownloadAndRestartFragment;
import com.fairphone.updater.fragments.MainFragment;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;
import com.fairphone.updater.tools.Utils;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

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

	private DownloadManager mDownloadManager;

	private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

	private long mLatestUpdateDownloadId;

	private BroadcastReceiver newVersionbroadcastReceiver;

	public static boolean DEV_MODE_ENABLED;
	private int mIsDevModeCounter;


	private TextView headerMainFairphoneText;
	private TextView headerMainAndroidText;
	private TextView headerFairphoneText;
	private TextView headerAndroidText;


	public static enum HeaderType {
		MAIN_FAIRPHONE, MAIN_ANDROID, FAIRPHONE, ANDROID
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

	public UpdaterState getCurrentUpdaterState() {

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
		headerMainFairphoneText = (TextView) findViewById(R.id.header_main_fairphone_text);
		headerFairphoneText = (TextView) findViewById(R.id.header_fairphone_text);
		headerMainAndroidText = (TextView) findViewById(R.id.header_main_android_text);
		headerAndroidText = (TextView) findViewById(R.id.header_android_text);
	}

	public void updateHeader(HeaderType type, String headerText) {

		switch (type) {
		case FAIRPHONE:
			headerMainFairphoneText.setVisibility(View.GONE);
			headerFairphoneText.setVisibility(View.VISIBLE);
			headerMainAndroidText.setVisibility(View.GONE);
			headerAndroidText.setVisibility(View.GONE);

			headerFairphoneText.setText(headerText);
			break;

		case ANDROID:
			headerMainFairphoneText.setVisibility(View.GONE);
			headerFairphoneText.setVisibility(View.GONE);
			headerMainAndroidText.setVisibility(View.GONE);
			headerAndroidText.setVisibility(View.VISIBLE);

			headerAndroidText.setText(headerText);
			break;

		case MAIN_ANDROID:
			headerMainFairphoneText.setVisibility(View.GONE);
			headerFairphoneText.setVisibility(View.GONE);
			headerMainAndroidText.setVisibility(View.VISIBLE);
			headerAndroidText.setVisibility(View.GONE);
			break;

		case MAIN_FAIRPHONE:
		default:
			headerMainFairphoneText.setVisibility(View.VISIBLE);
			headerFairphoneText.setVisibility(View.GONE);
			headerMainAndroidText.setVisibility(View.GONE);
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
			Fragment firstFragment = getFragmentFromState();

			

			// In case this activity was started with special instructions from
			// an
			// Intent, pass the Intent's extras to the fragment as arguments
			firstFragment.setArguments(getIntent().getExtras());

			// Add the fragment to the 'fragment_container' FrameLayout
			getSupportFragmentManager().beginTransaction()
					.add(R.id.fragment_holder, firstFragment).commit();
		}
	}

	public Fragment getFragmentFromState() {
		Fragment firstFragment;
		switch (mCurrentState) {
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
	
	public Fragment getTopFragment() {
		List<Fragment> allFragments = getSupportFragmentManager()
				.getFragments();
		Fragment topFragment = null;
		if (allFragments != null) {
			topFragment = allFragments.get(allFragments.size()-1);
		}
		
		return topFragment;
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

				Utils.downloadConfigFile(this);
			}
		}
	}

	public boolean isUpdateAvailable() {
		return mLatestVersion.isNewerVersionThan(mDeviceVersion);
	}

	public String getVersionName(Version version) {
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

	public Version getDeviceVersion() {
		return mDeviceVersion;
	}

	public Version getLatestVersion() {
		return mLatestVersion;
	}

	public Version getSelectedVersion() {
		return mSelectedVersion;
	}

	public HeaderType getHeaderTypeFromImageType(String imageType) {
		HeaderType type = HeaderType.MAIN_FAIRPHONE;
		if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(imageType)) {
			type = HeaderType.ANDROID;
		} else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(imageType)) {
			type = HeaderType.FAIRPHONE;
		}
		return type;
	}

	public void setSelectedVersion(Version selectedVersion) {
		int versionNumber = selectedVersion != null ? selectedVersion
				.getNumber() : 0;
		String versionImageType = selectedVersion != null ? selectedVersion
				.getImageType() : "";

		Editor editor = mSharedPreferences.edit();
		editor.putInt(PREFERENCE_SELECTED_VERSION_NUMBER, versionNumber);
		editor.putString(PREFERENCE_SELECTED_VERSION_TYPE, versionImageType);
		editor.commit();

		mSelectedVersion = UpdaterData.getInstance().getVersion(
				versionImageType, versionNumber);
	}

	public void startVersionDownload() {
		if (!Utils.areGappsInstalling(FairphoneUpdater2Activity.this)) {
			setSelectedVersion(mSelectedVersion != null ? mSelectedVersion
					: mLatestVersion);
			showEraseAllDataWarning();
		} else {
			showGappsInstalingWarning();
		}
	}

	private void showEraseAllDataWarning() {

		if (mSelectedVersion.hasEraseAllPartitionWarning()) {
			new AlertDialog.Builder(FairphoneUpdater2Activity.this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.eraseAllPartitionsWarning)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (mCurrentState == UpdaterState.NORMAL) {
										startUpdateDownload();
									} else if (mCurrentState == UpdaterState.PREINSTALL) {
										startPreInstall();
									}
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).setIcon(android.R.drawable.ic_dialog_alert)
					.show();
		} else {
			if (mCurrentState == UpdaterState.NORMAL) {
				startUpdateDownload();
			} else if (mCurrentState == UpdaterState.PREINSTALL) {
				startPreInstall();
			}
		}
	}

	private void showGappsInstalingWarning() {
		new AlertDialog.Builder(FairphoneUpdater2Activity.this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.updater_google_apps_installing_description)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// close dialog
							}
						})

				.setIcon(android.R.drawable.ic_dialog_alert).show();
	}

	private boolean isWiFiEnabled() {

		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();

		return isWifi;
	}

	public void startUpdateDownload() {
		
		// use only on WiFi
		if (isWiFiEnabled()) {
			// set the download for the latest version on the download manager
			String fileName = VersionParserHelper
					.getNameFromVersion(mSelectedVersion);
			String downloadTitle = mSelectedVersion.getName() + " "
					+ mSelectedVersion.getImageTypeDescription(getResources());
			Request request = createDownloadRequest(
					mSelectedVersion.getDownloadLink() + getModelAndOS(),
					fileName, downloadTitle);
			if (request != null) {
				mLatestUpdateDownloadId = mDownloadManager.enqueue(request);

				// save it on the shared preferences
				savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);

				// change state to download
				changeState(UpdaterState.DOWNLOAD);
			} else {
				Toast.makeText(
						this,
						getResources().getString(R.string.updateDownloadError)
								+ " " + downloadTitle, Toast.LENGTH_LONG)
						.show();
			}
		} else {
			Resources resources = this.getResources();

			AlertDialog.Builder disclaimerDialog = new AlertDialog.Builder(this);

			disclaimerDialog.setTitle(resources
					.getString(R.string.wifiDiscaimerTitle));

			// Setting Dialog Message
			disclaimerDialog.setMessage(resources
					.getString(R.string.wifiDiscaimerMessage));
			disclaimerDialog.setPositiveButton(
					resources.getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// do nothing, since the state is still the same
						}
					});
			disclaimerDialog.create();
			disclaimerDialog.show();
		}
	}

	private void removeLastUpdateDownload() {
		if (mLatestUpdateDownloadId != 0) {
			// residue download ID
			mDownloadManager.remove(mLatestUpdateDownloadId);

			mLatestUpdateDownloadId = 0;
			savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);
			setSelectedVersion(null);
		}
	}

	private void updateDownloadFile() {

		DownloadManager.Query query = new DownloadManager.Query();

		query.setFilterById(mLatestUpdateDownloadId);

		Cursor cursor = mDownloadManager.query(query);

		if (cursor.moveToFirst()) {
			int columnIndex = cursor
					.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = cursor.getInt(columnIndex);

			switch (status) {
			case DownloadManager.STATUS_SUCCESSFUL:
				changeState(UpdaterState.PREINSTALL);
				break;
			case DownloadManager.STATUS_FAILED:
				Resources resources = getResources();
				if (mSelectedVersion != null) {
					String downloadTitle = mSelectedVersion.getName()
							+ " "
							+ mSelectedVersion
									.getImageTypeDescription(resources);
					Toast.makeText(
							getApplicationContext(),
							resources.getString(R.string.updateDownloadError)
									+ " " + downloadTitle, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(getApplicationContext(),
							resources.getString(R.string.updateDownloadError),
							Toast.LENGTH_LONG).show();
				}
				changeState(UpdaterState.NORMAL);
				break;
			}
		}

		cursor.close();
	}

	protected void setupBroadcastReceiver() {
		newVersionbroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (FairphoneUpdater2Activity.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED
						.equals(action)) {
					mLatestVersion = getLatestVersionFromConfig();
					if (mCurrentState == UpdaterState.NORMAL) {
						Fragment topFragment = getTopFragment();
						if (topFragment != null
								&& topFragment instanceof MainFragment) {
							((MainFragment) topFragment)
									.toogleUpdateAvailableGroup();
						}
					}
				} else if (FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED
						.equals(action)) {
					String link = intent
							.getStringExtra(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK);
					Toast.makeText(
							context.getApplicationContext(),
							context.getResources().getString(
									R.string.configFileDownloadLinkError)
									+ " " + link, Toast.LENGTH_LONG).show();
				}
			}
		};
	}

	private Version getLatestVersionFromConfig() {
		Version latest = UpdaterData.getInstance().getLatestVersion(
				mDeviceVersion.getImageType());
		return latest;
	}

	@Override
	protected void onResume() {
		super.onResume();

		setupInstallationReceivers();

		setupBroadcastReceiver();

		registerBroadCastReceiver();
		// check current state
		mCurrentState = getCurrentUpdaterState();

		if (mCurrentState == UpdaterState.NORMAL) {
			Utils.startUpdaterService(this, true);
		}

		boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
		mDeviceVersion = VersionParserHelper.getDeviceVersion(this);
		mLatestVersion = isConfigLoaded ? UpdaterData.getInstance()
				.getLatestVersion(mDeviceVersion.getImageType())
				: new Version();

		getSelectedVersionFromSharedPreferences();

		if (mSharedPreferences
				.getBoolean(
						FairphoneUpdater2Activity.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD,
						false)) {
			Editor editor = mSharedPreferences.edit();
			editor.putBoolean(
					FairphoneUpdater2Activity.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD,
					false);
			editor.commit();
			startUpdateDownload();
		} else {
			changeFragment(getFragmentFromState());
		}
	}

	public void changeState(UpdaterState newState) {
		updateStatePreference(newState);
        changeFragment(getFragmentFromState());
	}

	private void updateStatePreference(UpdaterState newState) {
		mCurrentState = newState;

		Editor editor = mSharedPreferences.edit();

		editor.putString(PREFERENCE_CURRENT_UPDATER_STATE, mCurrentState.name());

		editor.commit();
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterBroadCastReceiver();
	}

	@Override
	protected void onStop() {
		super.onStop();

	}

	private void setupNormalState() {

		removeLastUpdateDownload();

	}

	private String getVersionDownloadPath(Version version) {
		Resources resources = getResources();
		return Environment.getExternalStorageDirectory()
				+ resources.getString(R.string.updaterFolder)
				+ VersionParserHelper.getNameFromVersion(version);
	}

	// ************************************************************************************
	// PRE INSTALL
	// ************************************************************************************

	private void setupPreInstallState() {

		Resources resources = getResources();
		// the latest version data must exist
		if (mSelectedVersion != null) {

			// check the md5 of the file
			File file = new File(getVersionDownloadPath(mSelectedVersion));

			if (file.exists()) {
				if (Utils.checkMD5(mSelectedVersion.getMd5Sum(), file)) {
					copyUpdateToCache(file);
					return;
				} else {
					Toast.makeText(
							this,
							resources
									.getString(R.string.invalidMD5DownloadMessage),
							Toast.LENGTH_LONG).show();
					removeLastUpdateDownload();
				}
			}
		}

		// remove the updater directory
		File fileDir = new File(Environment.getExternalStorageDirectory()
				+ resources.getString(R.string.updaterFolder));
		fileDir.delete();

		// else if the perfect case does not happen, reset the download
		changeState(UpdaterState.NORMAL);
	}

	private void copyUpdateToCache(File file) {
		if (canCopyToCache()) {
			CopyFileToCacheTask copyTask = new CopyFileToCacheTask();
			copyTask.execute(
					file.getPath(),
					Environment.getDownloadCacheDirectory()
							+ "/"
							+ VersionParserHelper
									.getNameFromVersion(mSelectedVersion));
		} else {
			Log.d(TAG, "No space on cache. Defaulting to Sdcard");
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.noSpaceAvailableCache),
					Toast.LENGTH_LONG).show();
		}
	}

	public boolean canCopyToCache() {
		Resources resources = getResources();
		double cacheSize = Utils.getPartitionSizeInMBytes(Environment
				.getDownloadCacheDirectory());
		return cacheSize > resources
				.getInteger(R.integer.FP1CachePartitionSizeMb)
				&& cacheSize > resources
						.getInteger(R.integer.minimalCachePartitionSizeMb);
	}

	private void clearCache() {
		File f = Environment.getDownloadCacheDirectory();
		File files[] = f.listFiles();
		if (files != null) {
			Log.d(TAG, "Size: " + files.length);
			for (int i = 0; i < files.length; i++) {
				String filename = files[i].getName();

				if (filename.endsWith(".zip")) {
					files[i].delete();
					Log.d(TAG, "Deleted file " + filename);
				}
			}
		}
	}

	private void startPreInstall() {

		if (RootTools.isAccessGiven()) {
			// set the command for the recovery
			Resources resources = getResources();
			// Process p;
			try {

				Shell.runRootCommand(new CommandCapture(0,
						"rm -f /cache/recovery/command"));

				Shell.runRootCommand(new CommandCapture(0,
						"rm -f /cache/recovery/extendedcommand"));

				Shell.runRootCommand(new CommandCapture(0,
						"echo '--wipe_cache' >> /cache/recovery/command"));

				if (canCopyToCache()) {
					Shell.runRootCommand(new CommandCapture(
							0,
							"echo '--update_package=/"
									+ resources
											.getString(R.string.recoveryCachePath)
									+ "/"
									+ VersionParserHelper
											.getNameFromVersion(mSelectedVersion)
									+ "' >> /cache/recovery/command"));
				} else {
					Shell.runRootCommand(new CommandCapture(
							0,
							"echo '--update_package=/"
									+ resources
											.getString(R.string.recoverySdCardPath)
									+ resources
											.getString(R.string.updaterFolder)
									+ VersionParserHelper
											.getNameFromVersion(mSelectedVersion)
									+ "' >> /cache/recovery/command"));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RootDeniedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// send broadcast intent
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(GappsInstallerHelper.GAPPS_REINSTALATION);
			this.sendBroadcast(broadcastIntent);

			if (canCopyToCache()) {
				removeLastUpdateDownload();
			}

			// remove the update files from data
			removeUpdateFilesFromData();
			// reboot the device into recovery
			// ((PowerManager)
			// getSystemService(POWER_SERVICE)).reboot("recovery");
			try {
				updateStatePreference(UpdaterState.NORMAL);
				Shell.runRootCommand(new CommandCapture(0, "reboot recovery"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RootDeniedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// TODO: show warning
		}

	}

	// ************************************************************************************
	// Update Removal
	// ************************************************************************************
	private void removeUpdateFilesFromData() {
		try {
			Shell.runRootCommand(new CommandCapture(
					0,
					getResources().getString(R.string.removePlayStoreCommand),
					getResources().getString(R.string.removeGooglePlusCommand),
					getResources().getString(R.string.removeSoundSearchCommand),
					getResources().getString(R.string.removeGmailCommand),
					getResources()
							.getString(R.string.removePlayServicesCommand),
					getResources().getString(R.string.removeQuicksearchCommand),
					getResources().getString(R.string.removeTalkbackCommand),
					getResources().getString(R.string.removeText2SpeechCommand)));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (RootDeniedException e) {
			e.printStackTrace();
		}
	}

	// ************************************************************************************
	// DOWNLOAD UPDATE
	// ************************************************************************************

	private String getModelAndOS() {
		StringBuilder sb = new StringBuilder();

		// attach the model and the os
		sb.append("?");
		sb.append("model=" + Build.MODEL.replaceAll("\\s", ""));
		Version currentVersion = VersionParserHelper.getDeviceVersion(this);

		if (currentVersion != null) {
			sb.append("&");
			sb.append("os=" + currentVersion.getAndroidVersion());
		}

		return sb.toString();
	}

	private Request createDownloadRequest(String url, String fileName,
			String downloadTitle) {

		Resources resources = getResources();
		Request request;
		try {
			request = new Request(Uri.parse(url));
			Environment.getExternalStoragePublicDirectory(
					Environment.getExternalStorageDirectory()
							+ resources.getString(R.string.updaterFolder))
					.mkdirs();

			request.setDestinationInExternalPublicDir(
					resources.getString(R.string.updaterFolder), fileName);
			request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
			request.setAllowedOverRoaming(false);

			request.setTitle(downloadTitle);
		} catch (Exception e) {
			request = null;
		}

		return request;
	}

	public void setupDownloadState() {
		// setup the download state views
		if (mSelectedVersion == null) {
			Resources resources = getResources();

			// we don't have the lastest.xml so get back to initial state
			File updateDir = new File(Environment.getExternalStorageDirectory()
					+ resources.getString(R.string.updaterFolder));

			updateDir.delete();

			changeState(UpdaterState.NORMAL);

			return;
		}

		// if there is a download ID on the shared preferences
		if (mLatestUpdateDownloadId == 0) {
			mLatestUpdateDownloadId = getLongPreference(PREFERENCE_DOWNLOAD_ID);

			// invalid download Id
			if (mLatestUpdateDownloadId == 0) {
				changeState(UpdaterState.NORMAL);
				return;
			}
		}

		updateDownloadFile();

	}

	

	private void setupInstallationReceivers() {
		mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

		mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();
	}

	private void registerBroadCastReceiver() {
		registerReceiver(mDownloadBroadCastReceiver, new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
		iFilter.addAction(FairphoneUpdater2Activity.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
		registerReceiver(newVersionbroadcastReceiver, iFilter);
	}

	private void unregisterBroadCastReceiver() {
		unregisterReceiver(mDownloadBroadCastReceiver);
		unregisterReceiver(newVersionbroadcastReceiver);
	}

	private class DownloadBroadCastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (mLatestUpdateDownloadId == 0) {
				mLatestUpdateDownloadId = getLongPreference(PREFERENCE_DOWNLOAD_ID);
			}

			updateDownloadFile();

		}
	}

	private class CopyFileToCacheTask extends
			AsyncTask<String, Integer, Integer> {

		ProgressDialog mProgress;

		@Override
		protected Integer doInBackground(String... params) {
			// check the correct number of
			if (params.length != 2) {
				return -1;
			}

			String originalFilePath = params[0];
			String destinyFilePath = params[1];

			if (RootTools.isAccessGiven()) {
				clearCache();

				File otaFilePath = new File(originalFilePath);
				File otaFileCache = new File(destinyFilePath);

				if (!otaFileCache.exists()) {
					RootTools.copyFile(otaFilePath.getPath(),
							otaFileCache.getPath(), false, false);
				}
			}

			return 1;
		}

		protected void onProgressUpdate(Integer... progress) {

		}

		protected void onPreExecute() {

			if (mProgress == null) {
				String title = "";
				String message = FairphoneUpdater2Activity.this.getResources()
						.getString(R.string.pleaseWait);
				mProgress = ProgressDialog.show(FairphoneUpdater2Activity.this,
						title, message, true, false);
			}
		}

		protected void onPostExecute(Integer result) {
			// disable the spinner
			if (mProgress != null) {
				mProgress.dismiss();
				mProgress = null;
			}
		}
	}

	public long getLatestDownloadId(){
		return mLatestUpdateDownloadId;
	}
	
	public DownloadManager getDownloadManger(){
		return mDownloadManager;
	}
}
