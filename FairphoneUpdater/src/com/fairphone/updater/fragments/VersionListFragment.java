package com.fairphone.updater.fragments;

import java.util.List;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fairphone.updater.FairphoneUpdater.HeaderType;
import com.fairphone.updater.R;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;

public class VersionListFragment extends BaseFragment
{

    private static final String TAG = VersionListFragment.class.getSimpleName();

    public static enum ListLayoutType
    {
        FAIRPHONE, ANDROID
    }

    private ListLayoutType mListLayoutType;
    private List<Version> mVersionList;
    private LinearLayout mVersionListContainer;
    private TextView mLatestVersionText;
    private TextView mInstalledIndicator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = setupLayout(inflater, container);

        return view;
    }

    private View setupLayout(LayoutInflater inflater, ViewGroup container)
    {

        View view = null;
        Resources resources = mainActivity.getResources();
        switch (mListLayoutType)
        {

            case ANDROID:
                view = inflater.inflate(R.layout.fragment_other_os_options_android_list, container, false);
                mainActivity.updateHeader(HeaderType.ANDROID, resources.getString(R.string.android_os));

                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                mLatestVersionText = (TextView) view.findViewById(R.id.other_os_options_android_version_text);
                mInstalledIndicator = (TextView) view.findViewById(R.id.other_os_options_android_installed_indicator_text);

                setupAndroidLatestVersion();
                setupAndroidVersions(container);
                break;
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_other_os_options_fairphone_list, container, false);
                mainActivity.updateHeader(HeaderType.FAIRPHONE, resources.getString(R.string.fairphone_os));

                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                mLatestVersionText = (TextView) view.findViewById(R.id.other_os_options_fairphone_version_text);
                mInstalledIndicator = (TextView) view.findViewById(R.id.other_os_options_fairphone_installed_indicator_text);

                setupFairphoneLatestVersion();
                setupFairphoneVersions(container);
                break;
        }
        return view;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    private void setupFairphoneVersions(ViewGroup root)
    {
        Button versionLayout;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mVersionList = UpdaterData.getInstance().getFairphoneVersionList();
        Version latestFairphoneVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE);

        for (Version version : mVersionList)
        {
            if (version.compareTo(latestFairphoneVersion) != 0)
            {
                versionLayout = (Button) inflater.inflate(R.layout.fragment_other_os_options_fairphone_list_button, root, false);
                versionLayout.setTag(version);
                versionLayout.setClickable(true);

                mVersionListContainer.addView(versionLayout);

                versionLayout.setText(version.getName() + " " + version.getBuildNumber());

                versionLayout.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Version selectedVersion = (Version) v.getTag();

                        if (selectedVersion != null)
                        {
                            VersionDetailFragment versionDetail = new VersionDetailFragment();

                            versionDetail.setupFragment(selectedVersion, DetailLayoutType.FAIRPHONE);

                            mainActivity.changeFragment(versionDetail);
                        }
                    }
                });

            }
        }
    }

    private void setupFairphoneLatestVersion()
    {
        final Version latestFairphoneVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE);
        mLatestVersionText.setText(latestFairphoneVersion.getName() + " v" + latestFairphoneVersion.getBuildNumber());

        if (mainActivity.getDeviceVersion().compareTo(latestFairphoneVersion) == 0)
        {
            mInstalledIndicator.setVisibility(View.VISIBLE);
        }
        else
        {
            mInstalledIndicator.setVisibility(View.GONE);
        }

        mLatestVersionText.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (latestFairphoneVersion != null)
                {
                    VersionDetailFragment versionDetail = new VersionDetailFragment();

                    versionDetail.setupFragment(latestFairphoneVersion, DetailLayoutType.FAIRPHONE);

                    mainActivity.changeFragment(versionDetail);
                }
            }
        });
    }

    private void setupAndroidVersions(ViewGroup root)
    {
        Button versionLayout;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mVersionList = UpdaterData.getInstance().getAOSPVersionList();
        Version latestAOSPVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_AOSP);

        for (Version version : mVersionList)
        {
            if (version.compareTo(latestAOSPVersion) != 0)
            {
                versionLayout = (Button) inflater.inflate(R.layout.fragment_other_os_options_android_list_button, root, false);
                versionLayout.setTag(version);
                versionLayout.setClickable(true);

                mVersionListContainer.addView(versionLayout);

                versionLayout.setText(version.getName() + " " + version.getBuildNumber());

                versionLayout.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
                        Version selectedVersion = (Version) v.getTag();

                        if (selectedVersion != null)
                        {
                            VersionDetailFragment versionDetail = new VersionDetailFragment();

                            versionDetail.setupFragment(selectedVersion, DetailLayoutType.ANDROID);

                            mainActivity.changeFragment(versionDetail);
                        }
                    }
                });

            }
        }
    }

    private void setupAndroidLatestVersion()
    {
        final Version latestAOSPVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_AOSP);
        mLatestVersionText.setText(latestAOSPVersion.getName() + " v" + latestAOSPVersion.getBuildNumber());

        if (mainActivity.getDeviceVersion().compareTo(latestAOSPVersion) == 0)
        {
            mInstalledIndicator.setVisibility(View.VISIBLE);
        }
        else
        {
            mInstalledIndicator.setVisibility(View.GONE);
        }

        mLatestVersionText.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (latestAOSPVersion != null)
                {
                    VersionDetailFragment versionDetail = new VersionDetailFragment();

                    versionDetail.setupFragment(latestAOSPVersion, DetailLayoutType.ANDROID);

                    mainActivity.changeFragment(versionDetail);
                }
            }
        });
    }

    public void setupFragment(ListLayoutType listType)
    {
        mListLayoutType = listType;
    }
}