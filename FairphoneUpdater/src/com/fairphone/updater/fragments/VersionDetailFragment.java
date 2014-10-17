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
import com.fairphone.updater.data.Version;
import com.fairphone.updater.data.VersionParserHelper;
import com.fairphone.updater.fragments.ConfirmationPopupDialog.ConfirmationPopupDialogListener;
import com.fairphone.updater.tools.Utils;

public class VersionDetailFragment extends BaseFragment
{

    private static final String TAG = VersionDetailFragment.class.getSimpleName();

    public static enum DetailLayoutType
    {
        UPDATE_FAIRPHONE, UPDATE_ANDROID, FAIRPHONE, ANDROID
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
    private TextView mDownload_and_update_button_version_text;
    private boolean mIsOSChange;
    private boolean mIsOlderVersion;

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
            case UPDATE_FAIRPHONE:
                view = inflater.inflate(R.layout.fragment_version_detail_update_fairphone, container, false);
                break;
            case UPDATE_ANDROID:
                view = inflater.inflate(R.layout.fragment_version_detail_update_android, container, false);
                break;
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_version_detail_android, container, false);
                break;
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
        mDownload_and_update_button_version_text = (TextView) view.findViewById(R.id.download_and_update_button_version_text);
    }

    private void setupDownloadAndUpdateButton()
    {
        setDownloadAndUpdateButtonText();

        // This is only not null when coming from other OS options fragment
        if (mDownload_and_update_button_version_text != null)
        {
            mDownload_and_update_button_version_text.setText(mainActivity.getVersionName(mSelectedVersion));
        }

        mDownload_and_update_button.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                startVersionDownload();
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
            case FAIRPHONE:
            case ANDROID:
            default:
                mDownload_and_update_button.setText(R.string.install);
                break;
        }
    }

    private void updateReleaseNotesText()
    {
        if (mSelectedVersion != null)
        {
            mVersion_release_notes_text.setText(mSelectedVersion.getReleaseNotes(Locale.getDefault().getLanguage()));
        }
    }

    private void updateVersionName()
    {
        mVersion_details_title_text.setText(mVersionDetailsTitle);
        mVersion_details_name_text.setText(mainActivity.getVersionName(mSelectedVersion));
    }

    public void setupFragment(Version selectedVersion, DetailLayoutType detailType)
    {
        mSelectedVersion = selectedVersion;

        mDetailLayoutType = detailType;
    }

    private void setHeaderAndVersionDetailsTitles()
    {

        mHeaderType = mainActivity.getHeaderTypeFromImageType(mSelectedVersion.getImageType());
        Resources resources = mainActivity.getResources();
        Version deviceversion = mainActivity.getDeviceVersion();

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
                mHeaderText = mainActivity.getVersionName(mSelectedVersion);
                mVersionDetailsTitle = resources.getString(R.string.new_os);
                mIsOSChange = deviceversion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_FAIRPHONE);
                mIsOlderVersion =
                        (deviceversion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP) && deviceversion.isNewerVersionThan(mSelectedVersion));
                break;

            case FAIRPHONE:
            default:
                mHeaderText = mainActivity.getVersionName(mSelectedVersion);
                mVersionDetailsTitle = resources.getString(R.string.older_version);
                mIsOSChange = deviceversion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_AOSP);
                mIsOlderVersion =
                        (deviceversion.getImageType().equalsIgnoreCase(Version.IMAGE_TYPE_FAIRPHONE) && deviceversion.isNewerVersionThan(mSelectedVersion));
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

        // use only on WiFi
        if (isWiFiEnabled())
        {
            // set the download for the latest version on the download manager
            String fileName = VersionParserHelper.getNameFromVersion(mSelectedVersion);
            String downloadTitle = mSelectedVersion.getName() + " " + mSelectedVersion.getImageTypeDescription(getResources());
            Request request = createDownloadRequest(mSelectedVersion.getDownloadLink() + Utils.getModelAndOS(mainActivity), fileName, downloadTitle);
            if (request != null)
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
        else
        {
            Resources resources = this.getResources();

            AlertDialog.Builder disclaimerDialog = new AlertDialog.Builder(mainActivity);

            disclaimerDialog.setTitle(resources.getString(R.string.wifi_disabled));

            // Setting Dialog Message
            disclaimerDialog.setMessage(resources.getString(R.string.wifi_discaimer_message));
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

    @Override
    public void onResume()
    {
        super.onResume();

        mDownloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);

        setHeaderAndVersionDetailsTitles();
        mainActivity.updateHeader(mHeaderType, mHeaderText);
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

    public void startVersionDownload()
    {

        if (!Utils.areGappsInstalling(mainActivity))
        {
            if (mIsOSChange || mIsOlderVersion)
            {
                showPopupDialog(mSelectedVersion.getName(), mSelectedVersion.hasEraseAllPartitionWarning(), new ConfirmationPopupDialogListener()
                {

                    @Override
                    public void onFinishPopUpDialog(boolean isOk)
                    {
                        if (isOk)
                        {
                            mainActivity.setSelectedVersion(mSelectedVersion != null ? mSelectedVersion : mainActivity.getLatestVersion());
                            showEraseAllDataWarning(true);
                        }
                    }
                });
            }
            else
            {
                mainActivity.setSelectedVersion(mSelectedVersion != null ? mSelectedVersion : mainActivity.getLatestVersion());
                showEraseAllDataWarning(false);
            }
        }
        else
        {
            showGappsInstalingWarning();
        }
    }

    private void showEraseAllDataWarning(boolean bypassEraseAllWarning)
    {

        final UpdaterState currentState = mainActivity.getCurrentUpdaterState();

        if (mSelectedVersion.hasEraseAllPartitionWarning() && !bypassEraseAllWarning)
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

    private void showGappsInstalingWarning()
    {
        new AlertDialog.Builder(mainActivity).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.updater_google_apps_installing_description)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // close dialog
                    }
                }).show();
    }
}
