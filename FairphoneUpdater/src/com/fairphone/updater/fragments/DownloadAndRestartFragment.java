package com.fairphone.updater.fragments;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.fairphone.updater.gappsinstaller.GappsInstallerHelper;
import com.fairphone.updater.tools.Utils;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class DownloadAndRestartFragment extends BaseFragment
{

    protected static final String TAG = DownloadAndRestartFragment.class.getSimpleName();

    private TextView mDownloadVersionName;
    private LinearLayout mVersionDownloadingGroup;
    private ProgressBar mVersionDownloadProgressBar;
    private LinearLayout mVersionInstallGroup;
    private Button mRestartButton;
    private Button mCancelButton;
    private Version mSelectedVersion;

    private DownloadManager mDownloadManager;

    private DownloadBroadCastReceiver mDownloadBroadCastReceiver;

    private long mLatestUpdateDownloadId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        mSelectedVersion = mainActivity.getSelectedVersion();
        View view = inflateViewByImageType(inflater, container);

        setupLayout(view);
        
        // Setup monitoring for future connectivity status changes
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                	abortUpdateProccess();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);        
        mainActivity.registerReceiver(networkStateReceiver, filter);

        return view;
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
                        showEraseAllDataWarning();
                    }
                });

                break;

            default:
                Log.w(TAG, "Wrong State: " + state + "\nOnly DOWNLOAD and PREINSTALL are supported");
                mainActivity.removeLastFragment(true);
                return;

        }

        mCancelButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                abortUpdateProccess();
            }
        });
    }

    private void showEraseAllDataWarning()
    {
        if (mSelectedVersion.hasEraseAllPartitionWarning())
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
        if (mSelectedVersion != null)
        {
            if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
            }
            else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mSelectedVersion.getImageType()))
            {
                mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "");
            }
        }
        else
        {
            mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
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
                while (latestUpdateDownloadId != 0 && downloading)
                {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(latestUpdateDownloadId);

                    Cursor cursor = mDownloadManager.query(q);
                    if (cursor != null)
                    {
                        cursor.moveToFirst();
                        try
                        {
                            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if ((bytes_total + 10000) > Utils.getAvailablePartitionSizeInBytes(Environment.getExternalStorageDirectory()))
                            {
                                downloading = false;
                                Toast.makeText(mainActivity, getResources().getString(R.string.no_space_available_sd_card_message), Toast.LENGTH_LONG).show();
                                abortUpdateProccess();
                            }

                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL)
                            {
                                downloading = false;

                                bytes_downloaded = 0;
                                bytes_total = 0;
                            }

                            mVersionDownloadProgressBar.setProgress(bytes_downloaded);
                            mVersionDownloadProgressBar.setMax(bytes_total);
                        } catch (Exception e)
                        {
                            downloading = false;
                            Log.e(TAG, "Error updating download progress: " + e.getMessage());
                        }

                        cursor.close();
                        try
                        {
                            // TODO WHYYYYYY???????
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

        updateHeader();
        mDownloadVersionName.setText(mainActivity.getVersionName(mSelectedVersion));
        toggleDownloadProgressAndRestart();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        unregisterBroadCastReceiver();
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

            Cursor cursor = mDownloadManager.query(query);

            if (cursor.moveToFirst())
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
                        startDownloadProgressUpdateThread();
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Resources resources = getResources();
                        if (mSelectedVersion != null)
                        {
                            String downloadTitle = mSelectedVersion.getName() + " " + mSelectedVersion.getImageTypeDescription(resources);
                            Toast.makeText(mainActivity, resources.getString(R.string.error_downloading) + " " + downloadTitle, Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(mainActivity, resources.getString(R.string.error_downloading), Toast.LENGTH_LONG).show();
                        }
                        abortUpdateProccess();
                        break;
                }
            }
            else
            {
                abortUpdateProccess();
            }

            cursor.close();
        }
    }

    // ************************************************************************************
    // PRE INSTALL
    // ************************************************************************************

    private void setupPreInstallState()
    {

        Resources resources = mainActivity.getResources();
        // the latest version data must exist
        if (mSelectedVersion != null)
        {

            // check the md5 of the file
            File file = new File(getVersionDownloadPath(mSelectedVersion));

            if (file.exists())
            {
                if (Utils.checkMD5(mSelectedVersion.getMd5Sum(), file))
                {
                    copyUpdateToCache(file);
                    return;
                }
                else
                {
                    Toast.makeText(mainActivity, resources.getString(R.string.invalid_md5_download_message), Toast.LENGTH_LONG).show();
                    removeLastUpdateDownload();
                }
            }
        }

        // remove the updater directory
        File fileDir = new File(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));
        fileDir.delete();

        // else if the perfect case does not happen, reset the download
        abortUpdateProccess();
    }

    // ************************************************************************************
    // DOWNLOAD UPDATE
    // ************************************************************************************

    public void setupDownloadState()
    {
        // setup the download state views
        if (mSelectedVersion == null)
        {
            Resources resources = getResources();

            // we don't have the lastest.xml so get back to initial state
            File updateDir = new File(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));

            updateDir.delete();

            abortUpdateProccess();

            return;
        }

        // if there is a download ID on the shared preferences
        if (mLatestUpdateDownloadId == 0)
        {
            mLatestUpdateDownloadId = mainActivity.getLatestUpdateDownloadIdFromSharedPreference();

            // invalid download Id
            if (mLatestUpdateDownloadId == 0)
            {
                abortUpdateProccess();
                return;
            }
        }

        updateDownloadFile();
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

                if (canCopyToCache())
                {
                    Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=/" + resources.getString(R.string.recoveryCachePath) + "/"
                            + VersionParserHelper.getNameFromVersion(mSelectedVersion) + "' >> /cache/recovery/command"));
                }
                else
                {
                    Shell.runRootCommand(new CommandCapture(0, "echo '--update_package=/" + resources.getString(R.string.recoverySdCardPath)
                            + resources.getString(R.string.updaterFolder) + VersionParserHelper.getNameFromVersion(mSelectedVersion)
                            + "' >> /cache/recovery/command"));
                }
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
            broadcastIntent.setAction(GappsInstallerHelper.GAPPS_REINSTALATION);
            mainActivity.sendBroadcast(broadcastIntent);

            if (canCopyToCache())
            {
                removeLastUpdateDownload();
            }

            // remove the update files from data
            removeUpdateFilesFromData();

            // reboot the device into recovery
            try
            {
                mainActivity.updateStatePreference(UpdaterState.NORMAL);
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
            abortUpdateProccess();
        }
    }

    private void copyUpdateToCache(File file)
    {
        if (canCopyToCache())
        {
            CopyFileToCacheTask copyTask = new CopyFileToCacheTask();
            copyTask.execute(file.getPath(), Environment.getDownloadCacheDirectory() + "/" + VersionParserHelper.getNameFromVersion(mSelectedVersion));
        }
        else
        {
            Log.d(TAG, "No space on cache. Defaulting to Sdcard");
            Toast.makeText(mainActivity, getResources().getString(R.string.no_space_available_cache_message), Toast.LENGTH_LONG).show();
        }
    }

    public boolean canCopyToCache()
    {
        Resources resources = getResources();
        double cacheSize = Utils.getPartitionSizeInMBytes(Environment.getDownloadCacheDirectory());
        return cacheSize > resources.getInteger(R.integer.FP1CachePartitionSizeMb) && cacheSize > resources.getInteger(R.integer.minimalCachePartitionSizeMb);
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
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (TimeoutException e)
        {
            e.printStackTrace();
        } catch (RootDeniedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean removeLastUpdateDownload()
    {
        long latestUpdateDownloadId = mainActivity.getLatestUpdateDownloadIdFromSharedPreference();
        if (latestUpdateDownloadId != 0)
        {
            // residue download ID
            mDownloadManager.remove(latestUpdateDownloadId);

            mainActivity.resetLastUpdateDownloadId();
        }
        return latestUpdateDownloadId != 0; // report if something was canceled
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
                clearCache();

                File otaFilePath = new File(originalFilePath);
                File otaFileCache = new File(destinyFilePath);

                if (!otaFileCache.exists())
                {
                    RootTools.copyFile(otaFilePath.getPath(), otaFileCache.getPath(), false, false);
                }
            }
            else
            {
                abortUpdateProccess();
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

    private String getVersionDownloadPath(Version version)
    {
        Resources resources = mainActivity.getResources();
        return Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder) + VersionParserHelper.getNameFromVersion(version);
    }

    public void abortUpdateProccess()
    {
        if (removeLastUpdateDownload())
        {

	        if (mainActivity.getFragmentCount() == 1 && mainActivity.getBackStackSize() == 0)
	        {
	            mainActivity.removeLastFragment(false);
	            mainActivity.changeState(UpdaterState.NORMAL);
	        }
	        else
	        {
	            mainActivity.removeLastFragment(false);
	            mainActivity.updateStatePreference(UpdaterState.NORMAL);
	        }
        }
    }

}
