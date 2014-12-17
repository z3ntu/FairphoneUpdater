package com.fairphone.updater.fragments;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.FairphoneUpdater.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.data.DownloadableItem;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.ConfirmationPopupDialog.ConfirmationPopupDialogListener;
import com.fairphone.updater.tools.Utils;

public class VersionDetailFragment extends BaseFragment
{

    private static final String TAG = VersionDetailFragment.class.getSimpleName();

    public static enum DetailLayoutType
    {
        UPDATE_FAIRPHONE, UPDATE_ANDROID, FAIRPHONE, ANDROID, APP_STORE
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
    private boolean mIsOlderVersion;
    private Store mSelectedStore;
    private boolean mIsVersion;

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
        View view = null;
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
                startDownload();
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
            mVersion_details_name_text.setText(mainActivity.getItemName(item));
        }
    }

    public void setupFragment(Version selectedVersion, DetailLayoutType detailType)
    {
        mSelectedVersion = selectedVersion;

        mDetailLayoutType = detailType;
        mSelectedStore = null;
    }

    public void setupFragment(Store selectedStore, DetailLayoutType detailType)
    {
        mSelectedStore = selectedStore;

        mDetailLayoutType = detailType;
        mSelectedVersion = null;
    }

    private void setHeaderAndVersionDetailsTitles()
    {

        Resources resources = mainActivity.getResources();
        Version deviceVersion = mainActivity.getDeviceVersion();

        if (mIsVersion && mSelectedVersion != null)
        {
            mHeaderType = mainActivity.getHeaderTypeFromImageType(mSelectedVersion != null ? mSelectedVersion.getImageType() : "");
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
                mIsOlderVersion = false;
                break;

            case ANDROID:
                mHeaderText = mainActivity.getItemName(mSelectedVersion);
                mVersionDetailsTitle = resources.getString(R.string.new_os);
                mIsOSChange = deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_FAIRPHONE);
                mIsOlderVersion =
                        (deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP) && deviceVersion.isNewerVersionThan(mSelectedVersion));
                break;
            case APP_STORE:
                mHeaderText = mainActivity.getItemName(mSelectedStore);
                mVersionDetailsTitle = resources.getString(R.string.install);
                mIsOSChange = false;
                mIsOlderVersion = false;
                break;
            case FAIRPHONE:
            default:
                mHeaderText = mainActivity.getItemName(mSelectedVersion);
                mVersionDetailsTitle = resources.getString(R.string.older_version);
                mIsOSChange = deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP);
                mIsOlderVersion =
                        (deviceVersion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_FAIRPHONE) && deviceVersion.isNewerVersionThan(mSelectedVersion));
                break;
        }
    }

    private boolean isWiFiEnabled()
    {

        ConnectivityManager manager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();

        return isWifi;
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

    public void startUpdateDownload()
    {
        DownloadableItem item = mIsVersion ? mSelectedVersion : mSelectedStore;
        // use only on WiFi
        if (isWiFiEnabled())
        {
            if (item != null)
            {
                // set the download for the latest version on the download
                // manager
                String fileName = Utils.getFilenameFromDownloadableItem(item);
                String downloadTitle = Utils.getDownloadTitleFromDownloadableItem(getResources(), item);
                Request request = createDownloadRequest(item.getDownloadLink() + Utils.getModelAndOS(mainActivity), fileName, downloadTitle);
                if (request != null && mDownloadManager != null)
                {
                    long mLatestUpdateDownloadId = mDownloadManager.enqueue(request);

                    // save it on the shared preferences
                    mainActivity.saveLatestUpdateDownloadId(mLatestUpdateDownloadId);

                    // change state to download
                    mainActivity.changeState(UpdaterState.DOWNLOAD);
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

            AlertDialog.Builder wifiOffDialog = new AlertDialog.Builder(mainActivity);

            wifiOffDialog.setTitle(resources.getString(R.string.wifi_disabled));

            // Setting Dialog Message
            wifiOffDialog.setMessage(resources.getString(R.string.wifi_discaimer_message));
            wifiOffDialog.setPositiveButton(resources.getString(android.R.string.ok), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // do nothing, since the state is still the same
                }
            });
            wifiOffDialog.create();
            wifiOffDialog.show();
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
                new ConfirmationPopupDialog(version, mIsOSChange, mIsOlderVersion, hasEraseAllDataWarning, mDetailLayoutType, listener);
        popupDialog.show(fm, version);
    }

    public void startDownload()
    {
        if (mIsVersion && mSelectedVersion != null)
        {
            if (mIsOSChange || mIsOlderVersion)
            {
                showPopupDialog(mainActivity.getItemName(mSelectedVersion), mSelectedVersion.hasEraseAllPartitionWarning(),
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

    protected void showStoreDisclaimer()
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
