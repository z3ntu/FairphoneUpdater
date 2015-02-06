package com.fairphone.updater.fragments;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.FairphoneUpdater.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.UpdaterService;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.fairphone.updater.tools.Utils;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

@SuppressLint("ValidFragment")
public class DownloadAndRestartFragment extends BaseFragment
{

    private static final int PROGRESS_BAR_UPDATE_FREQUENCY_IN_MILLIS = 1000;

    private static final String TAG = DownloadAndRestartFragment.class.getSimpleName();
    private static final int GET_LATEST_DOWNLOAD_ID_RETRIES = 12;

    private TextView mDownloadVersionName;
    private LinearLayout mVersionDownloadingGroup;
    private ProgressBar mVersionDownloadProgressBar;
    private LinearLayout mVersionInstallGroup;
    private Button mRestartButton;
    private Button mCancelButton;
    private Version mSelectedVersion;
    private Store mSelectedStore;

    private final boolean mIsVersion;

    private DownloadManager mDownloadManager;

    private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

    private BroadcastReceiver mNetworkStateReceiver;

    private long mLatestUpdateDownloadId;

    public DownloadAndRestartFragment(boolean isVersion)
    {
        super();

        mIsVersion = isVersion;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view;
        if (mIsVersion)
        {
            mSelectedVersion = mainActivity.getSelectedVersion();
            view = inflateViewByImageType(inflater, container);
            mSelectedStore = null;
        }
        else
        {
            mSelectedStore = mainActivity.getSelectedStore();
            view = inflateStoreView(inflater, container);
            mSelectedVersion = null;
        }

        setupLayout(view);

        return view;
    }

    private View inflateViewByImageType(LayoutInflater inflater, ViewGroup container)
    {
        View view = inflater.inflate(R.layout.fragment_download_fairphone, container, false);
        if (mSelectedVersion != null)
        {
            if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                view = inflater.inflate(R.layout.fragment_download_android, container, false);
            }
            else if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                view = inflater.inflate(R.layout.fragment_download_fairphone, container, false);
            }
        }
        return view;
    }

    private View inflateStoreView(LayoutInflater inflater, ViewGroup container)
    {

	    return inflater.inflate(R.layout.fragment_download_app_store, container, false);
    }

    private void toggleDownloadProgressAndRestart()
    {
        UpdaterState state = mainActivity.getCurrentUpdaterState();
        switch (state)
        {
            case DOWNLOAD:
                setupDownloadState();

                mVersionInstallGroup.setVisibility(View.GONE);
                mVersionDownloadingGroup.setVisibility(View.VISIBLE);
                break;

            case PREINSTALL:
                setupPreInstallState();

                mVersionDownloadingGroup.setVisibility(View.GONE);
                mVersionInstallGroup.setVisibility(View.VISIBLE);

                mRestartButton.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
                        if (mIsVersion)
                        {
                            showEraseAllDataWarning();
                        }
                        else if (mSelectedStore != null)
                        {
                            startPreInstall();
                        }
                    }
                });

                break;

            default:
                Log.w(TAG, "Wrong State: " + state + "\nOnly DOWNLOAD and PREINSTALL are supported");
                mainActivity.onBackPressed();
                return;

        }

        mCancelButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                abortUpdateProcess();
                mainActivity.onBackPressed();
            }
        });
    }

    private void showEraseAllDataWarning()
    {
        if (mSelectedVersion != null && mSelectedVersion.hasEraseAllPartitionWarning())
        {
            new AlertDialog.Builder(mainActivity).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.erase_all_partitions_warning_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            startPreInstall();
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // do nothing
                        }
                    }).show();
        }
        else
        {
            startPreInstall();
        }
    }

    private void updateHeader()
    {
        if (mIsVersion && mSelectedVersion != null)
        {
            if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "", false);
            }
            else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "", false);
            }
        }
        else if (mSelectedStore != null)
        {
            mainActivity.updateHeader(HeaderType.MAIN_APP_STORE, "", false);
        }
        else
        {
            mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "", false);
        }
    }

    private void startDownloadProgressUpdateThread()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                boolean downloading = true;

                long latestUpdateDownloadId = mainActivity.getLatestDownloadId();

                int count = GET_LATEST_DOWNLOAD_ID_RETRIES;

                // Wait a sensible amount of time to get a correct reference to the download
                while ((latestUpdateDownloadId <= 0) && count > 0)
                {
                    try
                    {
                        Thread.sleep(Utils.DELAY_HALF_SECOND);
                        count--;
                        latestUpdateDownloadId = mainActivity.getLatestDownloadId();
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                while (mDownloadManager != null && latestUpdateDownloadId != 0 && downloading)
                {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(latestUpdateDownloadId);

                    Cursor cursor = mDownloadManager != null ? mDownloadManager.query(q) : null;

                    if (cursor != null && cursor.moveToFirst())
                    {
                        try
                        {
                            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if ((bytes_total + Utils.BUFFER_SIZE_10_MBYTES) > Utils.getAvailablePartitionSizeInBytes(Environment.getExternalStorageDirectory()))
                            {
                                downloading = false;
                                Toast.makeText(mainActivity, getResources().getString(R.string.no_space_available_sd_card_message), Toast.LENGTH_LONG).show();
                                abortUpdateProcess();
                            }
                            else
                            {
                                switch (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
                                {
                                    case DownloadManager.STATUS_SUCCESSFUL:
                                    case DownloadManager.STATUS_FAILED:
                                        downloading = false;

                                        bytes_downloaded = 0;
                                        bytes_total = 0;
                                        break;
                                    default:
                                        break;
                                }

                                mVersionDownloadProgressBar.setProgress(bytes_downloaded);
                                mVersionDownloadProgressBar.setMax(bytes_total);
                            }
                        } catch (Exception e)
                        {
                            downloading = false;
                            Log.e(TAG, "Error updating download progress: " + e.getMessage());
                        }

                        cursor.close();
                        try
                        {
                            Thread.sleep(PROGRESS_BAR_UPDATE_FREQUENCY_IN_MILLIS);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        if (cursor != null)
                        {
                            downloading = false;
                            cursor.close();
                        }
                        if (mDownloadManager == null)
                        {
                            downloading = false;
                        }
                    }
                }
            }
        }).start();
    }

    private void setupLayout(View view)
    {
        mDownloadVersionName = (TextView) view.findViewById(R.id.download_version_name_text);

        // download in progress group
        mVersionDownloadingGroup = (LinearLayout) view.findViewById(R.id.version_downloading_group);
        mVersionDownloadProgressBar = (ProgressBar) view.findViewById(R.id.version_download_progress_bar);

        // restart group
        mVersionInstallGroup = (LinearLayout) view.findViewById(R.id.version_install_group);
        mRestartButton = (Button) view.findViewById(R.id.restart_button);

        mCancelButton = (Button) view.findViewById(R.id.cancel_button);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setupInstallationReceivers();
        registerDownloadBroadCastReceiver();

        registerNetworkStatusBoradcastReceiver();

        updateHeader();

        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
        if (item != null)
        {
            mDownloadVersionName.setText(mainActivity.getItemName(item, mIsVersion));
        }

        toggleDownloadProgressAndRestart();
    }

    private void registerNetworkStatusBoradcastReceiver()
    {
        // Setup monitoring for future connectivity status changes
        if (mNetworkStateReceiver != null)
        {
            mNetworkStateReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false))
                    {
                        abortUpdateProcess();
                    }
                }
            };
        }

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mainActivity.registerReceiver(mNetworkStateReceiver, filter);
    }

    private void unregisterNetworkStatusBoradcastReceiver()
    {
        if (mNetworkStateReceiver != null)
        {
            mainActivity.unregisterReceiver(mNetworkStateReceiver);

            mNetworkStateReceiver = null;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        unregisterBroadCastReceiver();

        unregisterNetworkStatusBoradcastReceiver();
    }

    private void setupInstallationReceivers()
    {
        mDownloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);

        mDownloadBroadCastReceiver = new DownloadBroadCastReceiver();
    }

    private void registerDownloadBroadCastReceiver()
    {
        mainActivity.registerReceiver(mDownloadBroadCastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void unregisterBroadCastReceiver()
    {
        mainActivity.unregisterReceiver(mDownloadBroadCastReceiver);
    }

    private class DownloadBroadCastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {

            mainActivity.getLatestUpdateDownloadIdFromSharedPreference();

            updateDownloadFile();
        }
    }

    private void updateDownloadFile()
    {

        long downloadId = mainActivity.getLatestDownloadId();

        if (downloadId != 0)
        {
            DownloadManager.Query query = new DownloadManager.Query();

            query.setFilterById(downloadId);

            Cursor cursor = mDownloadManager != null ? mDownloadManager.query(query) : null;

            if (cursor != null && cursor.moveToFirst())
            {
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);

                switch (status)
                {

                    case DownloadManager.STATUS_SUCCESSFUL:
                        mainActivity.updateStatePreference(UpdaterState.PREINSTALL);
                        toggleDownloadProgressAndRestart();
                        break;
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        startDownloadProgressUpdateThread();
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Resources resources = getResources();
                        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
                        if (item != null)
                        {
                            String downloadTitle = Utils.getDownloadTitleFromDownloadableItem(getResources(), item, mIsVersion);
                            Toast.makeText(mainActivity, resources.getString(R.string.error_downloading) + " " + downloadTitle, Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(mainActivity, resources.getString(R.string.error_downloading), Toast.LENGTH_LONG).show();
                        }
                        abortUpdateProcess();
                        break;
                    default:
                        break;
                }
            }
            else
            {
                abortUpdateProcess();
            }

            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    // ************************************************************************************
    // PRE INSTALL
    // ************************************************************************************

    private void setupPreInstallState()
    {
        Resources resources = mainActivity.getResources();
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
        if (item != null)
        {
            File file = new File(getDownloadPath(item));

            if (file.exists())
            {
                if (Utils.checkMD5(item.getMd5Sum(), file))
                {
                    copyUpdateToCache(file);
                }
                else
                {
                    Toast.makeText(mainActivity, resources.getString(R.string.invalid_md5_download_message), Toast.LENGTH_LONG).show();
                    removeLastUpdateDownload();
                }
            }
        }
    }

    // ************************************************************************************
    // DOWNLOAD UPDATE
    // ************************************************************************************

    void setupDownloadState()
    {
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;

        // setup the download state views
        if (item == null)
        {
            Resources resources = getResources();

            // we don't have the lastest.xml so get back to initial state
            File updateDir = new File(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));

	        final boolean notDeleted = !updateDir.delete();
	        if(notDeleted) {
		        Log.d(TAG, "Unable to delete "+updateDir.getAbsolutePath());
	        }

	        abortUpdateProcess();

            return;
        }

        // if there is a download ID on the shared preferences
        if (mLatestUpdateDownloadId == 0)
        {
            mLatestUpdateDownloadId = mainActivity.getLatestUpdateDownloadIdFromSharedPreference();

            // invalid download Id
            if (mLatestUpdateDownloadId == 0)
            {
                abortUpdateProcess();
                return;
            }
        }

        updateDownloadFile();
    }

    private void startPreInstall()
    {
        final Resources resources = getResources();
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;

        File f = new File("/" + resources.getString(R.string.recoveryCachePath) + "/" + Utils.getFilenameFromDownloadableItem(item, mIsVersion));
        if (!f.exists())
        {
            abortUpdateProcess();
        }
        else if (item != null && RootTools.isAccessGiven())
        {
            // set the command for the recovery
            try
            {

                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/command"));

                Shell.runRootCommand(new CommandCapture(0, "rm -f /cache/recovery/extendedcommand"));

                Shell.runRootCommand(new CommandCapture(0, "echo '--wipe_cache' >> /cache/recovery/command"));

                if (Utils.hasUnifiedPartition(resources))
                {
                    Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=/" + resources.getString(R.string.recoveryCachePath) + "/"
                            + Utils.getFilenameFromDownloadableItem(item, mIsVersion) + "' >> /cache/recovery/command"));
                }
                else
                {
                    Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=/" + resources.getString(R.string.recoverySdCardPath)
                            + resources.getString(R.string.updaterFolder) + Utils.getFilenameFromDownloadableItem(item, mIsVersion) + "' >> /cache/recovery/command"));
                }
            } catch (IOException | NotFoundException | TimeoutException | RootDeniedException e)
            {
                e.printStackTrace();
            }

            new Thread(new Runnable() {
                @SuppressLint("CommitPrefEdits")
                @Override
                public void run() {
                    Editor editor = mSharedPreferences.edit();
                    editor.remove(UpdaterService.PREFERENCE_REINSTALL_GAPPS);
                    editor.commit();

                    if (Utils.hasUnifiedPartition(resources))
                    {
                        removeLastUpdateDownload();
                    }

                    // remove the update files from data
                    removeUpdateFilesFromData();

                    // reboot the device into recovery
                    try
                    {
                        mainActivity.updateStatePreference(UpdaterState.NORMAL);
                        mainActivity.clearSelectedItems();
                        clearConfigFile();
                        editor = mSharedPreferences.edit();
                        editor.remove(UpdaterService.LAST_CONFIG_DOWNLOAD_IN_MS);
                        editor.remove(MainFragment.SHARED_PREFERENCES_ENABLE_GAPPS);
                        editor.commit();
                        Shell.runRootCommand(new CommandCapture(0, "reboot recovery"));
                    } catch (IOException | TimeoutException | RootDeniedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else
        {
            abortUpdateProcess();
        }
    }

    private void clearConfigFile()
    {
        long cfgFile = mainActivity.getConfigFileDownloadIdFromSharedPreference();
        if (cfgFile != 0 && mDownloadManager != null)
        {
            mDownloadManager.remove(cfgFile);
        }
        mainActivity.clearConfigFileDownloadId();
        VersionParserHelper.removeConfigFiles(mainActivity);
    }

    private void copyUpdateToCache(File file)
    {
        if (Utils.canCopyToCache(file))
        {
            DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
            CopyFileToCacheTask copyTask = new CopyFileToCacheTask();
            if (item != null)
            {
                copyTask.execute(file.getPath(), Environment.getDownloadCacheDirectory() + "/" + Utils.getFilenameFromDownloadableItem(item, mIsVersion));
            }
            else
            {
                Toast.makeText(mainActivity, getResources().getString(R.string.no_space_available_cache_message), Toast.LENGTH_LONG).show();
                abortUpdateProcess();
            }
        }
        else
        {
            if (Utils.hasUnifiedPartition(getResources()))
            {
                Log.d(TAG, "No space on cache. Defaulting to Sdcard");
                Toast.makeText(mainActivity, getResources().getString(R.string.no_space_available_cache_message), Toast.LENGTH_LONG).show();

                abortUpdateProcess();
            }
        }
    }

    // ************************************************************************************
    // Update Removal
    // ************************************************************************************
    private void removeUpdateFilesFromData()
    {
        try
        {
            Shell.runRootCommand(new CommandCapture(0, getResources().getString(R.string.removePlayStoreCommand), getResources().getString(
                    R.string.removeGooglePlusCommand), getResources().getString(R.string.removeSoundSearchCommand), getResources().getString(
                    R.string.removeGmailCommand), getResources().getString(R.string.removePlayServicesCommand), getResources().getString(
                    R.string.removeQuicksearchCommand), getResources().getString(R.string.removeTalkbackCommand), getResources().getString(
                    R.string.removeText2SpeechCommand)));
        } catch (IOException | TimeoutException | RootDeniedException e)
        {
            e.printStackTrace();
        }
    }

    void removeLastUpdateDownload()
    {
        long latestUpdateDownloadId = mainActivity.getLatestUpdateDownloadIdFromSharedPreference();
        if (latestUpdateDownloadId != 0 && mDownloadManager != null)
        {
            // residue download ID
            mDownloadManager.remove(latestUpdateDownloadId);

            mainActivity.resetLastUpdateDownloadId();
        }
    }

    private class CopyFileToCacheTask extends AsyncTask<String, Integer, Integer>
    {

        ProgressDialog mProgress;

        @Override
        protected Integer doInBackground(String... params)
        {
            // check the correct number of
            if (params.length != 2)
            {
                return -1;
            }

            String originalFilePath = params[0];
            String destinyFilePath = params[1];

            if (RootTools.isAccessGiven())
            {
                Utils.clearCache();

                File otaFilePath = new File(originalFilePath);
                File otaFileCache = new File(destinyFilePath);

                if (!otaFileCache.exists())
                {
                    RootTools.copyFile(otaFilePath.getPath(), otaFileCache.getPath(), false, false);
                }
            }
            else
            {
                abortUpdateProcess();
            }

            return 1;
        }

        protected void onProgressUpdate(Integer... progress)
        {

        }

        protected void onPreExecute()
        {

            if (mProgress == null)
            {
                String title = "";
                String message = mainActivity.getResources().getString(R.string.please_be_patient);
                mProgress = ProgressDialog.show(mainActivity, title, message, true, false);
            }
        }

        protected void onPostExecute(Integer result)
        {
            // disable the spinner
            if (mProgress != null)
            {
                mProgress.dismiss();
                mProgress = null;
            }
        }
    }

    private String getDownloadPath(DownloadableItem item)
    {
        Resources resources = mainActivity.getResources();
        return Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + Utils.getFilenameFromDownloadableItem(item, mIsVersion);
    }

    public void abortUpdateProcess()
    {
        removeLastUpdateDownload();

        mainActivity.clearSelectedItems();
        mainActivity.updateStatePreference(UpdaterState.NORMAL);
    }
}
