/*
 * Copyright (C) 2013 Fairphone Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fairphone.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class FairphoneUpdater extends Activity
{

    private static final String TAG = FairphoneUpdater.class.getSimpleName();

    public static final String FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED = "FairphoneUpdater.NEW.VERSION.RECEIVED";

    private static final String PREFERENCE_CURRENT_UPDATER_STATE = "CurrentUpdaterState";

    private static final String PREFERENCE_DOWNLOAD_ID = "LatestUpdateDownloadId";

    public static final String FAIRPHONE_UPDATER_PREFERENCES = "FairphoneUpdaterPreferences";

    private static final String GAPPS_REINSTALATION = "GAPPS_REINSTALATION_REQUEST";

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

    private UpdaterState mCurrentState;

    private SharedPreferences mSharedPreferences;

    // views
    private ImageView mCurrentVersionImage;

    private TextView mCurrentVersionNameText;

    private TextView mUpdateAvailableText;

    // private TextView mCurrentVersionReleaseDateText;

    private TextView mReleaseNotesText;

    private Button mFairphoneVersionsButton;

    private Button mAOSPVersionsButton;

    private DownloadManager mDownloadManager;

    private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

    private long mLatestUpdateDownloadId;

    private BroadcastReceiver newVersionbroadcastReceiver;

    private Button mMoreInfoText;

    private LinearLayout mOtherVersionsLayout;

    private LinearLayout mMoreInfoLayout;

    private Button mMoreInfoActionButton;

    private LinearLayout mCurrentVersionInfoLayout;

    private LinearLayout mUpdateDownloadInfoLayout;

    private ProgressBar mUpdateVersionDownloadProgressBar;

    private Button mMoreInfoCancelButton;

    private LinearLayout mMoreInfoButtonsLayout;

    private TextView mDownloadingVersionText;

    private Version mSelectedVersion;

    private TextView mReleaseNotesTitleText;

    private View mMoreInfoFairphoneLogo;

    private View mMoreInfoAndroidLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        
        setContentView(R.layout.activity_fairphone_updater);

        isDeviceSupported();
        mSharedPreferences = getSharedPreferences(FAIRPHONE_UPDATER_PREFERENCES, MODE_PRIVATE);

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);

        // get system data
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);

        mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : new Version();

        getSelectedVersionFromSharedPreferences();

        // check current state
        mCurrentState = getCurrentUpdaterState();

        setupLayout();

        setupInstallationReceivers();

        setupBroadcastReceiver();

        if (mCurrentState == UpdaterState.NORMAL)
        {
            startUpdaterService();
        }
    }

    private void isDeviceSupported()
    {

        Resources resources = getResources();
        String[] suportedDevices = resources.getString(R.string.supportedDevices).split(";");
        for (String device : suportedDevices)
        {
            if (Build.MODEL.equalsIgnoreCase(device))
            {
                return;
            }
        }
        Toast.makeText(this, R.string.deviceNotSupported, Toast.LENGTH_LONG).show();
        finish();
    }

    public void startUpdaterService()
    {
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (UpdaterService.class.getName().equals(service.service.getClassName()))
            {
                isRunning = true;
                break;
            }
        }

        if (!isRunning)
        {
            Log.e(TAG, "Starting Updater Service...");
            Intent i = new Intent(this, UpdaterService.class);
            startService(i);
        }
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
                    mLatestVersion = getLatestVersion();
                    if (mCurrentState == UpdaterState.NORMAL)
                    {
                        setupState(mCurrentState);
                    }
                }
                else if (FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED.equals(action))
                {
                    String link = intent.getStringExtra(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_LINK);
                    Toast.makeText(context.getApplicationContext(), context.getResources().getString(R.string.configFileDownloadError) + " " + link,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private Version getLatestVersion()
    {
        Version latest = UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType());
        return latest;
    }

    private void setupLayout()
    {

        // title bar
        // titleText

        setupCurrentVersionInfoLayout();
        setupUpdateDownloadInfoLayout();
        setupOtherVersionsLayout();
        setupMoreInfoLayout();
    }

    public void setupMoreInfoLayout()
    {
        mMoreInfoLayout = (LinearLayout) findViewById(R.id.moreInfoLayout);
        mMoreInfoFairphoneLogo = (ImageView) findViewById(R.id.updateLogoFairphone);
        mMoreInfoAndroidLogo = (ImageView) findViewById(R.id.updateLogoAndroid);
        mReleaseNotesTitleText = (TextView) findViewById(R.id.releaseNotesTitle);
        mReleaseNotesText = (TextView) findViewById(R.id.releaseNotesText);
        mMoreInfoButtonsLayout = (LinearLayout) findViewById(R.id.actionButtonsContainer);
        mMoreInfoCancelButton = (Button) findViewById(R.id.cancelButton);
        mMoreInfoActionButton = (Button) findViewById(R.id.actionButton);
    }

    public void setupOtherVersionsLayout()
    {
        mOtherVersionsLayout = (LinearLayout) findViewById(R.id.otherVersionsLayout);
        mFairphoneVersionsButton = (Button) findViewById(R.id.fairphoneVersionsButton);
        mAOSPVersionsButton = (Button) findViewById(R.id.aospVersionsButton);

        mFairphoneVersionsButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                startFairphoneVersionsActivity();
            }
        });

        mAOSPVersionsButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                startAOSPVersionActivity();
            }
        });
    }

    public void setupUpdateDownloadInfoLayout()
    {
        mUpdateDownloadInfoLayout = (LinearLayout) findViewById(R.id.updateDownloadInfo);
        mDownloadingVersionText = (TextView) findViewById(R.id.downloadingText);
        mUpdateVersionDownloadProgressBar = (ProgressBar) findViewById(R.id.updateDownloadProgressBar);
    }

    public void setupCurrentVersionInfoLayout()
    {
        // top layout part
        mCurrentVersionImage = (ImageView) findViewById(R.id.currentVersionImage);
        // currentVersionInfo layout
        mCurrentVersionInfoLayout = (LinearLayout) findViewById(R.id.currentVersionInfo);
        // youAreRunningText
        mCurrentVersionNameText = (TextView) findViewById(R.id.currentVersionNameText);
        mUpdateAvailableText = (TextView) findViewById(R.id.updateAvailableText);
        // mCurrentVersionReleaseDateText =
        // (TextView)findViewById(R.id.currentVersionReleaseDateText);
        mMoreInfoText = (Button) findViewById(R.id.moreInfoText);
    }

    public void updateMoreInfoLayout(boolean hasUpdate)
    {

        updateMoreInfoReleaseNotesText(hasUpdate);
        // mMoreInfoCancelButton
        // .setVisibility(hasUpdate && mCurrentState != UpdaterState.NORMAL ?
        // View.VISIBLE
        // : View.GONE);
        mMoreInfoActionButton.setVisibility(hasUpdate ? View.VISIBLE : View.GONE);

        mMoreInfoCancelButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                toggleReleaseInfoOtherVersions();
                if (mCurrentState == UpdaterState.DOWNLOAD || mCurrentState == UpdaterState.PREINSTALL)
                {

                    changeState(UpdaterState.NORMAL);
                }
            }
        });

        updateMoreInfoActionButton();
    }

    public void updateMoreInfoActionButton()
    {
        // set button text
        switch (mCurrentState)
        {
            case NORMAL:
                mMoreInfoActionButton.setText(R.string.downloadAndUpdateVersionBtn);
                mMoreInfoActionButton.setEnabled(true);
                mMoreInfoCancelButton.setText(R.string.lessInfo);
                break;
            case DOWNLOAD:
                toggleMoreInfoLogo(false);
                mMoreInfoActionButton.setText(R.string.installBtn);
                mMoreInfoActionButton.setEnabled(false);
                mMoreInfoCancelButton.setText(android.R.string.cancel);
                break;
            case PREINSTALL:
                toggleMoreInfoLogo(false);
                mMoreInfoActionButton.setText(R.string.installBtn);
                mMoreInfoActionButton.setEnabled(true);
                mMoreInfoCancelButton.setText(android.R.string.cancel);
                break;
        }

        mMoreInfoActionButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {

                setSelectedVersion(mSelectedVersion != null ? mSelectedVersion : mLatestVersion);

                if (mSelectedVersion != null)
                {
                    // Picasso.with(getApplicationContext()).load(mSelectedVersion.getThumbnailLink())
                    // .placeholder(R.drawable.logo_fairphone)
                    // .into(mCurrentVersionImage);
                    int logo =
                            Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()) ? R.drawable.logo_fairphone
                                    : R.drawable.logo_android;
                    mCurrentVersionImage.setImageResource(logo);
                }

                if (mCurrentState == UpdaterState.NORMAL)
                {
                    startUpdateDownload();
                }
                else if (mCurrentState == UpdaterState.PREINSTALL)
                {
                    startPreInstall();
                }
            }
        });
    }

    private void toggleMoreInfoLogo(boolean hideAll)
    {
        if (hideAll)
        {
            mMoreInfoFairphoneLogo.setVisibility(View.GONE);
            mMoreInfoAndroidLogo.setVisibility(View.GONE);
        }
        else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()))
        {
            mMoreInfoFairphoneLogo.setVisibility(View.VISIBLE);
            mMoreInfoAndroidLogo.setVisibility(View.GONE);
        }
        else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mSelectedVersion.getImageType()))
        {
            mMoreInfoFairphoneLogo.setVisibility(View.GONE);
            mMoreInfoAndroidLogo.setVisibility(View.VISIBLE);
        }
    }

    protected void setSelectedVersion(Version selectedVersion)
    {
        int versionNumber = selectedVersion != null ? selectedVersion.getNumber() : 0;
        String versionImageType = selectedVersion != null ? selectedVersion.getImageType() : "";

        Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_SELECTED_VERSION_NUMBER, versionNumber);
        editor.putString(PREFERENCE_SELECTED_VERSION_TYPE, versionImageType);
        editor.commit();

        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    protected void getSelectedVersionFromSharedPreferences()
    {
        String versionImageType = mSharedPreferences.getString(PREFERENCE_SELECTED_VERSION_TYPE, "");
        int versionNumber = mSharedPreferences.getInt(PREFERENCE_SELECTED_VERSION_NUMBER, 0);
        mSelectedVersion = UpdaterData.getInstance().getVersion(versionImageType, versionNumber);
    }

    private void updateMoreInfoReleaseNotesText(boolean hasUpdate)
    {
        Version version = mSelectedVersion != null ? mSelectedVersion : mLatestVersion;
        Resources resources = getResources();
        if (hasUpdate)
        {
            mReleaseNotesTitleText.setText(resources.getString(R.string.releaseNotes));
            mReleaseNotesText.setText(version.getReleaseNotes() + "\n" + version.getAndroidVersion(resources));
        }
        else
        {
            mReleaseNotesTitleText.setText(resources.getString(R.string.releaseNotes));
            mReleaseNotesText.setText(mDeviceVersion.getReleaseNotes() + "\n" + mDeviceVersion.getAndroidVersion(resources));
        }
    }

    public void toggleReleaseInfoOtherVersions()
    {
        if (mOtherVersionsLayout.getVisibility() == View.VISIBLE)
        {
            mOtherVersionsLayout.setVisibility(View.GONE);
            mMoreInfoLayout.setVisibility(View.VISIBLE);
            mMoreInfoText.setText(R.string.lessInfo);
            mMoreInfoText.setVisibility(View.GONE);

        }
        else
        {
            mOtherVersionsLayout.setVisibility(View.VISIBLE);
            mMoreInfoLayout.setVisibility(View.GONE);
            mMoreInfoText.setText(R.string.moreInfo);
            mMoreInfoText.setVisibility(View.VISIBLE);
            toggleMoreInfoLogo(true);
        }
    }

    protected void startAOSPVersionActivity()
    {
        Intent i = new Intent(this, VersionListActivity.class);
        i.putExtra(VersionListActivity.VERSION_LIST_TYPE, VersionListActivity.AOSP_VERSIONS);
        startActivity(i);
    }

    protected void startFairphoneVersionsActivity()
    {
        Intent i = new Intent(this, VersionListActivity.class);
        i.putExtra(VersionListActivity.VERSION_LIST_TYPE, VersionListActivity.FAIRPHONE_VERSIONS);
        startActivity(i);
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

    @Override
    protected void onResume()
    {
        super.onResume();

        registerBroadCastReceiver();
        // check current state
        mCurrentState = getCurrentUpdaterState();

        boolean isConfigLoaded = UpdaterService.readUpdaterData(this);
        mDeviceVersion = VersionParserHelper.getDeviceVersion(this);
        mLatestVersion = isConfigLoaded ? UpdaterData.getInstance().getLatestVersion(mDeviceVersion.getImageType()) : new Version();

        getSelectedVersionFromSharedPreferences();

        if (mSharedPreferences.getBoolean(FairphoneUpdater.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD, false))
        {
            Editor editor = mSharedPreferences.edit();
            editor.putBoolean(FairphoneUpdater.PREFERENCE_SELECTED_VERSION_BEGIN_DOWNLOAD, false);
            editor.commit();
            startUpdateDownload();
        }
        else
        {
            setupState(mCurrentState);
        }
    }

    private void setupState(UpdaterState state)
    {
        switch (state)
        {
            case NORMAL:
                setupNormalState();
                break;
            case DOWNLOAD:
                setupDownloadState();
                break;
            case PREINSTALL:
                setupPreInstallState();
                break;
        }
    }

    private void changeState(UpdaterState newState)
    {
        mCurrentState = newState;

        Editor editor = mSharedPreferences.edit();

        editor.putString(PREFERENCE_CURRENT_UPDATER_STATE, mCurrentState.name());

        editor.commit();

        setupState(mCurrentState);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        unregisterBroadCastReceiver();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

    }

    private void setupNormalState()
    {

        if (mLatestUpdateDownloadId != 0)
        {
            // residue download ID
            mDownloadManager.remove(mLatestUpdateDownloadId);

            mLatestUpdateDownloadId = 0;
            savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);
            setSelectedVersion(null);
        }

        mCurrentVersionInfoLayout.setVisibility(View.VISIBLE);
        mOtherVersionsLayout.setVisibility(View.VISIBLE);
        mUpdateDownloadInfoLayout.setVisibility(View.GONE);
        mMoreInfoLayout.setVisibility(View.GONE);

        setupCurrentVersionInfoLayout(UpdaterState.NORMAL);
        setupOtherVersionsLayout(UpdaterState.NORMAL);
    }

    private void setupOtherVersionsLayout(UpdaterState state)
    {
        switch (state)
        {
            case NORMAL:
                mFairphoneVersionsButton.setEnabled(!UpdaterData.getInstance().isFairphoneVersionListEmpty());
                mAOSPVersionsButton.setEnabled(!UpdaterData.getInstance().isAOSPVersionListEmpty());
                break;
            case DOWNLOAD:
            case PREINSTALL:
                break;
        }
    }

    private void setupCurrentVersionInfoLayout(UpdaterState state)
    {
        Resources resources = getResources();
        final boolean isUpdateAvailable = mLatestVersion.isNewerVersionThan(mDeviceVersion);
        switch (state)
        {
            case NORMAL:

                // Picasso.with(this).load(mDeviceVersion.getThumbnailLink())
                // .placeholder(R.drawable.logo_fairphone)
                // .into(mCurrentVersionImage);

                int logo = Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mDeviceVersion.getImageType()) ? R.drawable.logo_fairphone : R.drawable.logo_android;
                mCurrentVersionImage.setImageResource(logo);

                mCurrentVersionNameText.setText(mDeviceVersion.getImageTypeDescription(resources) + " " + mDeviceVersion.getName() + " "
                        + mDeviceVersion.getBuildNumber());

                setupUpdateAvailable(resources, isUpdateAvailable);

                mMoreInfoText.setText(mMoreInfoLayout.getVisibility() == View.VISIBLE ? R.string.lessInfo : R.string.moreInfo);
                mMoreInfoText.setVisibility(mMoreInfoLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

                mMoreInfoText.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
                        updateMoreInfoLayout(isUpdateAvailable);
                        toggleReleaseInfoOtherVersions();
                    }
                });

                break;
            case DOWNLOAD:
            case PREINSTALL:
                if (mSelectedVersion != null)
                {
                    // Picasso.with(getApplicationContext()).load(mSelectedVersion.getThumbnailLink())
                    // .placeholder(R.drawable.logo_fairphone)
                    // .into(mCurrentVersionImage);
                    logo = Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()) ? R.drawable.logo_fairphone : R.drawable.logo_android;
                    mCurrentVersionImage.setImageResource(logo);
                }
                break;
        }
    }

    public void setupUpdateAvailable(Resources resources, final boolean isUpdateAvailable)
    {
        if (isUpdateAvailable)
        {
            mUpdateAvailableText.setText(R.string.newVersionAvailable);
            // mCurrentVersionReleaseDateText.setVisibility(View.GONE);
        }
        else
        {
            mUpdateAvailableText.setText(R.string.noUpdatesAvailable);
            // mCurrentVersionReleaseDateText.setText(resources.getString(R.string.releasedIn)
            // + " "
            // + mDeviceVersion.getReleaseDate());
            // mCurrentVersionReleaseDateText.setVisibility(View.VISIBLE);
        }
    }

    private UpdaterState getCurrentUpdaterState()
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

    private String getVersionDownloadPath(Version version)
    {
        Resources resources = getResources();
        return Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + VersionParserHelper.getNameFromVersion(version);
    }

    // ************************************************************************************
    // PRE INSTALL
    // ************************************************************************************

    private void setupPreInstallState()
    {

        Resources resources = getResources();
        // the latest version data must exist
        if (mSelectedVersion != null)
        {

            mCurrentVersionInfoLayout.setVisibility(View.GONE);
            mOtherVersionsLayout.setVisibility(View.GONE);
            mUpdateDownloadInfoLayout.setVisibility(View.GONE);
            mMoreInfoLayout.setVisibility(View.VISIBLE);

            // check the md5 of the file
            File file = new File(getVersionDownloadPath(mSelectedVersion));

            if (file.exists())
            {
                if (FairphoneUpdater.checkMD5(mSelectedVersion.getMd5Sum(), file))
                {

                    clearCache();
                    copyUpdateToCache(file);
                    setupCurrentVersionInfoLayout(mCurrentState);
                    updateMoreInfoLayout(true);
                    return;
                }
                else
                {
                    mDownloadManager.remove(mLatestUpdateDownloadId);
                    mLatestUpdateDownloadId = 0;

                    savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);

                    Toast.makeText(this, resources.getString(R.string.invalidDownloadMessage), Toast.LENGTH_SHORT).show();
                }
            }
        }

        // remove the updater directory
        File fileDir = new File(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));
        fileDir.delete();

        // else if the perfect case does not happen, reset the download
        changeState(UpdaterState.NORMAL);
    }

    private void copyUpdateToCache(File file)
    {
        if (RootTools.isAccessGiven())
        {
            File OtaFileCache = new File(Environment.getDownloadCacheDirectory() + "/" + VersionParserHelper.getNameFromVersion(mSelectedVersion));
            if (!OtaFileCache.exists())
            {
                RootTools.copyFile(file.getPath(), OtaFileCache.getPath(), false, false);
            }
        }
    }

    private void clearCache()
    {
        File f = Environment.getDownloadCacheDirectory();
        File files[] = f.listFiles();
        if (files != null)
        {
            Log.d(TAG, "Size: " + files.length);
            for (int i = 0; i < files.length; i++)
            {
                String filename = files[i].getName();

                if (filename.endsWith(".zip"))
                {
                    files[i].delete();
                    Log.d(TAG, "Deleted file " + filename);
                }
            }
        }
    }

    private void startPreInstall()
    {

        if (RootTools.isAccessGiven())
        {
            // set the command for the recovery
            Resources resources = getResources();
            // Process p;
            try
            {

                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/command"));

                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/extendedcommand"));

                Shell.runRootCommand(new CommandCapture(0, "echo '--wipe_cache' >> /cache/recovery/command"));

                Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=/" + resources.getString(R.string.recoveryPath) + "/"
                        + VersionParserHelper.getNameFromVersion(mSelectedVersion) + "' >> /cache/recovery/command"));

                // p = Runtime.getRuntime().exec("su");
                //
                // DataOutputStream os = new
                // DataOutputStream(p.getOutputStream());
                // os.writeBytes("rm -f /cache/recovery/command\n");
                // os.writeBytes("rm -f /cache/recovery/extendedcommand\n");
                //
                // os.writeBytes("echo '--wipe_cache' >> /cache/recovery/command\n");
                //
                // os.writeBytes("echo '--update_package=/" +
                // resources.getString(R.string.recoveryPath) + "/"
                // + VersionParserHelper.getNameFromVersion(mSelectedVersion) +
                // "' >> /cache/recovery/command\n");
                //
                // os.writeBytes("sync\n");
                // os.writeBytes("exit\n");
                // os.flush();
                // p.waitFor();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RootDeniedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // send broadcast intent
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(FairphoneUpdater.GAPPS_REINSTALATION);
            this.sendBroadcast(broadcastIntent);

            setSelectedVersion(null);
            // reboot the device into recovery

            // ((PowerManager)
            // getSystemService(POWER_SERVICE)).reboot("recovery");
            try
            {
                Shell.runRootCommand(new CommandCapture(0, "reboot recovery"));
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RootDeniedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            // TODO: show warning
        }

    }

    // ************************************************************************************
    // DOWNLOAD UPDATE
    // ************************************************************************************

    private void startUpdateDownload()
    {
        // use only on WiFi
        if (isWiFiEnabled())
        {
            // set the download for the latest version on the download manager
            String fileName = VersionParserHelper.getNameFromVersion(mSelectedVersion);
            String downloadTitle = mSelectedVersion.getName() + " " + mSelectedVersion.getImageTypeDescription(getResources()) + " Update";
            Request request = createDownloadRequest(mSelectedVersion.getDownloadLink(), fileName, downloadTitle);
            if (request != null)
            {
                mLatestUpdateDownloadId = mDownloadManager.enqueue(request);

                // save it on the shared preferences
                savePreference(PREFERENCE_DOWNLOAD_ID, mLatestUpdateDownloadId);

                // change state to download
                changeState(UpdaterState.DOWNLOAD);
            }
            else
            {
                Toast.makeText(this, getResources().getString(R.string.updateDownloadError) + " " + downloadTitle, Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Resources resources = this.getResources();

            AlertDialog.Builder disclaimerDialog = new AlertDialog.Builder(this);

            disclaimerDialog.setTitle(resources.getString(R.string.wifiDiscaimerTitle));

            // Setting Dialog Message
            disclaimerDialog.setMessage(resources.getString(R.string.wifiDiscaimerMessage));
            disclaimerDialog.setPositiveButton(resources.getString(android.R.string.ok), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // do nothing, since the state is still the same
                }
            });
            disclaimerDialog.create();
            disclaimerDialog.show();
        }
    }

    private Request createDownloadRequest(String url, String fileName, String downloadTitle)
    {

        Resources resources = getResources();
        Request request;
        try
        {
            request = new Request(Uri.parse(url));
            Environment.getExternalStoragePublicDirectory(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder)).mkdirs();

            request.setDestinationInExternalPublicDir(resources.getString(R.string.updaterFolder), fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setAllowedOverRoaming(false);

            request.setTitle(downloadTitle);
        } catch (Exception e)
        {
            request = null;
        }

        return request;
    }

    private boolean isWiFiEnabled()
    {

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();

        return isWifi;
    }

    private void setupDownloadState()
    {
        // setup the download state views
        if (mSelectedVersion == null)
        {
            Resources resources = getResources();

            // we don't have the lastest.xml so get back to initial state
            File updateDir = new File(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));

            updateDir.delete();

            changeState(UpdaterState.NORMAL);

            return;
        }

        // if there is a download ID on the shared preferences
        if (mLatestUpdateDownloadId == 0)
        {
            mLatestUpdateDownloadId = getLongPreference(PREFERENCE_DOWNLOAD_ID);

            // invalid download Id
            if (mLatestUpdateDownloadId == 0)
            {
                changeState(UpdaterState.NORMAL);
                return;
            }
        }

        mCurrentVersionInfoLayout.setVisibility(View.GONE);
        mOtherVersionsLayout.setVisibility(View.GONE);
        mUpdateDownloadInfoLayout.setVisibility(View.VISIBLE);
        mMoreInfoLayout.setVisibility(View.VISIBLE);

        updateMoreInfoLayout(true);
        setupUpdateDownloadInfoLayout(UpdaterState.DOWNLOAD);
        setupCurrentVersionInfoLayout(UpdaterState.DOWNLOAD);

        updateDownloadFile();

    }

    private void setupUpdateDownloadInfoLayout(UpdaterState state)
    {

        Resources resources = getResources();
        switch (state)
        {
            case NORMAL:
                break;
            case DOWNLOAD:
                mDownloadingVersionText.setText(resources.getString(R.string.downloading) + " " + mSelectedVersion.getName() + " "
                        + mSelectedVersion.getBuildNumber() + ":");
                break;
            case PREINSTALL:
                break;
        }
    }

    private void updateDownloadFile()
    {

        DownloadManager.Query query = new DownloadManager.Query();

        query.setFilterById(mLatestUpdateDownloadId);

        Cursor cursor = mDownloadManager.query(query);

        if (cursor.moveToFirst())
        {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(columnIndex);

            switch (status)
            {
                case DownloadManager.STATUS_SUCCESSFUL:
                    changeState(UpdaterState.PREINSTALL);
                    break;
                case DownloadManager.STATUS_RUNNING:
                    startDownloadProgressUpdateThread();
                    break;
                case DownloadManager.STATUS_FAILED:
                    changeState(UpdaterState.NORMAL);
                    break;
            }
        }

        cursor.close();
    }

    private void startDownloadProgressUpdateThread()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {

                boolean downloading = true;

                while (mLatestUpdateDownloadId != 0 && downloading)
                {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(mLatestUpdateDownloadId);

                    Cursor cursor = mDownloadManager.query(q);
                    if (cursor != null)
                    {
                        cursor.moveToFirst();
                        try
                        {
                            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL)
                            {
                                downloading = false;

                                bytes_downloaded = 0;
                                bytes_total = 0;
                            }

                            mUpdateVersionDownloadProgressBar.setProgress(bytes_downloaded);
                            mUpdateVersionDownloadProgressBar.setMax(bytes_total);
                        } catch (Exception e)
                        {
                            downloading = false;
                            Log.e(TAG, "Error updating download progress: " + e.getMessage());
                        }

                        cursor.close();
                        try
                        {
                            Thread.sleep(3000);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void setupInstallationReceivers()
    {
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();
    }

    private void registerBroadCastReceiver()
    {
        registerReceiver(mDownloadBroadCastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(FairphoneUpdater.FAIRPHONE_UPDATER_NEW_VERSION_RECEIVED);
        iFilter.addAction(FairphoneUpdater.FAIRPHONE_UPDATER_CONFIG_DOWNLOAD_FAILED);
        registerReceiver(newVersionbroadcastReceiver, iFilter);
    }

    private void unregisterBroadCastReceiver()
    {
        unregisterReceiver(mDownloadBroadCastReceiver);
        unregisterReceiver(newVersionbroadcastReceiver);
    }

    private class DownloadBroadCastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {

            if (mLatestUpdateDownloadId == 0)
            {
                mLatestUpdateDownloadId = getLongPreference(PREFERENCE_DOWNLOAD_ID);
            }

            updateDownloadFile();

        }
    }

    // **************************************************************************************************************
    // HELPERS
    // **************************************************************************************************************

    public static boolean checkMD5(String md5, File updateFile)
    {

        if (!updateFile.exists())
        {
            return false;
        }

        if (md5 == null || md5.equals("") || updateFile == null)
        {
            Log.e(TAG, "MD5 String NULL or UpdateFile NULL");
            return false;
        }

        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null)
        {
            Log.e(TAG, "calculatedDigest NULL");
            return false;
        }

        return calculatedDigest.equalsIgnoreCase(md5);
    }

    public static String calculateMD5(File updateFile)
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "Exception while getting Digest", e);
            return null;
        }

        InputStream is;
        try
        {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e)
        {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try
        {
            while ((read = is.read(buffer)) > 0)
            {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e)
        {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally
        {
            try
            {
                is.close();
            } catch (IOException e)
            {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }
}
