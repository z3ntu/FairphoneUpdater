package com.fairphone.updater.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater;
import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.FairphoneUpdater.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.ConfirmationPopupDialog.ConfirmationPopupDialogListener;
import com.fairphone.updater.tools.Utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

@SuppressLint("ValidFragment")
public class VersionDetailFragment extends BaseFragment
{

    private static final String TAG = VersionDetailFragment.class.getSimpleName();

    public static enum DetailLayoutType
    {
        UPDATE_FAIRPHONE, UPDATE_ANDROID, LATEST_FAIRPHONE, FAIRPHONE, ANDROID, APP_STORE
    }

    private HeaderType mHeaderType;
    private String mHeaderText;
    private TextView mVersion_details_title_text;
    private TextView mVersion_release_notes_text;
    private Button mDownload_and_update_button;
    private TextView mVersion_details_name_text;
    private String mVersionDetailsTitle;
    private Version mSelectedVersion;
    private DownloadManager mDownloadManager;
    private DetailLayoutType mDetailLayoutType;
    private boolean mIsOSChange;
    private Store mSelectedStore;
    private final boolean mIsVersion;

    public VersionDetailFragment(boolean isVersion)
    {
        super();

        mIsVersion = isVersion;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = setLayoutType(inflater, container);

        setupLayout(view);

        return view;
    }

    private View setLayoutType(LayoutInflater inflater, ViewGroup container)
    {
        View view;
        switch (mDetailLayoutType)
        {
            case UPDATE_ANDROID:
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_version_detail_android, container, false);
                break;
            case APP_STORE:
                view = inflater.inflate(R.layout.fragment_app_store_detail, container, false);
                break;
            case UPDATE_FAIRPHONE:
            case LATEST_FAIRPHONE:
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_version_detail_fairphone, container, false);
                break;
        }
        return view;
    }

    private void setupLayout(View view)
    {
        mVersion_details_title_text = (TextView) view.findViewById(R.id.version_details_title_text);
        mVersion_details_name_text = (TextView) view.findViewById(R.id.version_details_name_text);

        mVersion_release_notes_text = (TextView) view.findViewById(R.id.version_release_notes_text);

        mDownload_and_update_button = (Button) view.findViewById(R.id.download_and_update_button);
    }

    private void setupDownloadAndUpdateButton()
    {
        setDownloadAndUpdateButtonText();

        mDownload_and_update_button.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                boolean isWifiEnabled = Utils.isWiFiEnabled(mainActivity);
                boolean isBatteryLevelOk = Utils.isBatteryLevelOk(mainActivity);

                if (isWifiEnabled && isBatteryLevelOk)
                {
                    startDownload();
                }
                else
                {
                    if(!isWifiEnabled) {
                        AlertDialog.Builder wifiDialog = new AlertDialog.Builder(mainActivity);
                        wifiDialog.setIcon(R.drawable.ic_signal_wifi_4_bar_fpblue_24dp);
                        wifiDialog.setTitle(R.string.connect_to_wifi);
                        wifiDialog.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing, since the state is still the same
                            }
                        });
                        wifiDialog.create();
                        wifiDialog.show();
                    }

                    if(!isBatteryLevelOk) {
                        AlertDialog.Builder batteryDialog = new AlertDialog.Builder(mainActivity);
                        batteryDialog.setIcon(R.drawable.ic_battery_std_fpblue_24dp);
                        batteryDialog.setTitle(R.string.charge_battery);
                        batteryDialog.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing, since the state is still the same
                            }
                        });
                        batteryDialog.create();
                        batteryDialog.show();
                    }
                }
            }
        });
    }

    private void setDownloadAndUpdateButtonText()
    {
        switch (mDetailLayoutType)
        {
            case UPDATE_ANDROID:
            case UPDATE_FAIRPHONE:
                mDownload_and_update_button.setText(R.string.install_update);
                break;
            case APP_STORE:
            case LATEST_FAIRPHONE:
            case FAIRPHONE:
            case ANDROID:
            default:
                mDownload_and_update_button.setText(R.string.install);
                break;
        }
    }

    private void updateReleaseNotesText()
    {
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
        if (item != null)
        {
            mVersion_release_notes_text.setText(item.getReleaseNotes(Locale.getDefault().getLanguage()));
        }
    }

    private void updateVersionName()
    {
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;

        mVersion_details_title_text.setText(mVersionDetailsTitle);
        if (item != null)
        {
            mVersion_details_name_text.setText(mainActivity.getItemName(item, mIsVersion));
        }
    }

    public void setupFragment(Version selectedVersion, DetailLayoutType detailType)
    {
        mSelectedVersion = selectedVersion;

        mDetailLayoutType = detailType;
        mSelectedStore = null;
    }

    public void setupAppStoreFragment(Store selectedStore)
    {
        mSelectedStore = selectedStore;

        mDetailLayoutType = DetailLayoutType.APP_STORE;
        mSelectedVersion = null;
    }

    private void setHeaderAndVersionDetailsTitles()
    {

        Resources resources = mainActivity.getResources();
        Version deviceVersion = mainActivity.getDeviceVersion();

        if (mIsVersion && mSelectedVersion != null)
        {
            mHeaderType = FairphoneUpdater.getHeaderTypeFromImageType(mSelectedVersion.getImageType());
        }
        else if (mSelectedStore != null)
        {
            mHeaderType = HeaderType.APP_STORE;
        }
        else
        {
            mHeaderType = HeaderType.FAIRPHONE;
        }

        switch (mDetailLayoutType)
        {
            case UPDATE_FAIRPHONE:
            case UPDATE_ANDROID:
                mHeaderText = resources.getString(R.string.install_update);
                mVersionDetailsTitle = resources.getString(R.string.update_version);
                mIsOSChange = false;
                break;

            case ANDROID:
                mHeaderText = mSelectedVersion.getHumanReadableName();
                mVersionDetailsTitle = resources.getString(R.string.new_os);
                mIsOSChange = deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_FAIRPHONE);
                break;
            case APP_STORE:
                mHeaderText = FairphoneUpdater.getStoreName(mSelectedStore);
                mVersionDetailsTitle = resources.getString(R.string.install);
                mIsOSChange = false;
                break;
            case LATEST_FAIRPHONE:
                mHeaderText = mSelectedVersion.getHumanReadableName();
                mVersionDetailsTitle = resources.getString(R.string.latest_version);
                mIsOSChange = deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP);
                break;
            case FAIRPHONE:
            default:
                mHeaderText = mSelectedVersion.getHumanReadableName();
                mVersionDetailsTitle = resources.getString(R.string.additional_download);
                mIsOSChange = deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP);
                break;
        }
    }

    private Request createDownloadRequest(String url, String fileName, String downloadTitle)
    {

        Resources resources = getResources();
        Request request;
        try
        {
            request = new Request(Uri.parse(url));
	        final File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.getExternalStorageDirectory() + resources.getString(R.string.updaterFolder));
	        final boolean notMkDirs = !externalStoragePublicDirectory.mkdirs();
	        if(notMkDirs && !externalStoragePublicDirectory.exists()) {
		        throw new Exception("Couldn't create updater dir structures.");
	        }

	        request.setDestinationInExternalPublicDir(resources.getString(R.string.updaterFolder), fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setAllowedOverRoaming(false);

            request.setTitle(downloadTitle);
        } catch (Exception e)
        {
            Log.w(TAG, "Error setting the download request: " + e.getLocalizedMessage());
            request = null;
        }

        return request;
    }

    void startUpdateDownload()
    {
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;

        boolean isWifiEnabled = Utils.isWiFiEnabled(mainActivity);
        boolean isBatteryLevelOk = Utils.isBatteryLevelOk(mainActivity);

        if (isWifiEnabled && isBatteryLevelOk)
        {
            if (item != null)
            {
                // set the download for the latest version on the download
                // manager
                String fileName = Utils.getFilenameFromDownloadableItem(item, mIsVersion);
                String downloadTitle = Utils.getDownloadTitleFromDownloadableItem(getResources(), item, mIsVersion);
                String download_link =  item.getDownloadLink();
                if (!(download_link.startsWith("http://") || download_link.startsWith("https://")))
                {
                    // If the download URL is a relative path, make it an absolute
                    download_link = mainActivity.getPreferenceOtaDownloadUrl() + "/" + download_link;
                    // Sanitize URL - e.g. turn http://a.b//c/./d/../e to http://a.b/c/e
                    download_link = download_link.replaceAll("([^:])//", "/");
                    download_link = download_link.replaceAll("/([^/]+)/\\.\\./", "/");
                    download_link = download_link.replaceAll("/\\./", "/");
                    try {
                        download_link = new URL(download_link).toExternalForm();
                    } catch (MalformedURLException e) {
                        Log.w(TAG, "Potentially malformed download link " + download_link + ": " + e.getLocalizedMessage());
                    }
                }
                Request request = createDownloadRequest(download_link, fileName, downloadTitle);
                if (request != null && mDownloadManager != null)
                {
                    //Guarantee that only we have only one download
                    long oldDownloadId = mainActivity.getLatestUpdateDownloadIdFromSharedPreference();
                    if(oldDownloadId != 0){
                        mDownloadManager.remove(oldDownloadId);
                        mainActivity.saveLatestUpdateDownloadId(0);
                    }
                    
                    long mLatestUpdateDownloadId = mDownloadManager.enqueue(request);

                    // save it on the shared preferences
                    mainActivity.saveLatestUpdateDownloadId(mLatestUpdateDownloadId);

                    // change state to download
	                mainActivity.updateStatePreference(UpdaterState.DOWNLOAD);
	                mainActivity.changeFragment(mainActivity.getFragmentFromState());
                }
                else
                {
                    Toast.makeText(mainActivity, getResources().getString(R.string.error_downloading) + " " + downloadTitle, Toast.LENGTH_LONG).show();
                }
            }
        }
        else
        {
            Resources resources = this.getResources();

            if(!isWifiEnabled) {
                AlertDialog.Builder wifiDialog = new AlertDialog.Builder(mainActivity);
                wifiDialog.setIcon(R.drawable.ic_signal_wifi_4_bar_fpblue_24dp);
                wifiDialog.setTitle(resources.getString(R.string.connect_to_wifi));
                wifiDialog.setPositiveButton(resources.getString(R.string.got_it), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing, since the state is still the same
                    }
                });
                wifiDialog.create();
                wifiDialog.show();
            }

            if(!isBatteryLevelOk) {
                AlertDialog.Builder batteryDialog = new AlertDialog.Builder(mainActivity);
                batteryDialog.setIcon(R.drawable.ic_battery_std_fpblue_24dp);
                batteryDialog.setTitle(resources.getString(R.string.charge_battery));
                batteryDialog.setPositiveButton(resources.getString(R.string.got_it), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing, since the state is still the same
                    }
                });
                batteryDialog.create();
                batteryDialog.show();
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mDownloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);

        setHeaderAndVersionDetailsTitles();
        mainActivity.updateHeader(mHeaderType, mHeaderText, false);
        updateVersionName();
        updateReleaseNotesText();
        setupDownloadAndUpdateButton();
    }

    private void showPopupDialog(String version, boolean hasEraseAllDataWarning, ConfirmationPopupDialogListener listener)
    {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        ConfirmationPopupDialog popupDialog =
                new ConfirmationPopupDialog(version, mIsOSChange, hasEraseAllDataWarning, mDetailLayoutType, listener);
        popupDialog.show(fm, version);
    }

    void startDownload()
    {
        if (mIsVersion && mSelectedVersion != null)
        {
            if (mIsOSChange)
            {
                showPopupDialog(mSelectedVersion.getHumanReadableName(), mSelectedVersion.hasEraseAllPartitionWarning(),
                        new ConfirmationPopupDialogListener()
                        {

                            @Override
                            public void onFinishPopUpDialog(boolean isOk)
                            {
                                if (isOk)
                                {
                                    mainActivity.setSelectedVersion(mSelectedVersion);
                                    showEraseAllDataWarning(true);
                                }
                            }
                        });
            }
            else
            {
                mainActivity.setSelectedVersion(mSelectedVersion);
                showEraseAllDataWarning(false);
            }
        }
        else if (mSelectedStore != null)
        {
            mainActivity.setSelectedStore(mSelectedStore);
            showStoreDisclaimer();
        }
    }

    private void showEraseAllDataWarning(boolean bypassEraseAllWarning)
    {

        final UpdaterState currentState = mainActivity.getCurrentUpdaterState();

        if (mSelectedVersion != null && mSelectedVersion.hasEraseAllPartitionWarning() && !bypassEraseAllWarning)
        {
            new AlertDialog.Builder(mainActivity).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.erase_all_partitions_warning_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if (currentState == UpdaterState.NORMAL)
                            {
                                startUpdateDownload();
                            }
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mainActivity.setSelectedVersion(null);
                        }
                    }).show();
        }
        else
        {
            if (currentState == UpdaterState.NORMAL)
            {
                startUpdateDownload();
            }
            else
            {
                mainActivity.setSelectedVersion(null);
            }
        }
    }

    void showStoreDisclaimer()
    {
        final UpdaterState currentState = mainActivity.getCurrentUpdaterState();

        if (mSelectedStore != null && mSelectedStore.showDisclaimer())
        {
            new AlertDialog.Builder(mainActivity).setTitle(R.string.google_apps_disclaimer_title).setMessage(R.string.google_apps_disclaimer_description)
                    .setPositiveButton(R.string.google_apps_disclaimer_agree, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if (currentState == UpdaterState.NORMAL)
                            {
                                startUpdateDownload();
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mainActivity.setSelectedStore(null);
                        }
                    }).show();
        }
        else
        {
            if (currentState == UpdaterState.NORMAL)
            {
                startUpdateDownload();
            }
            else
            {
                mainActivity.setSelectedStore(null);
            }
        }
    }
}
