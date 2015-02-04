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
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;

public class VersionListFragment extends BaseFragment
{

    private static final String TAG = VersionListFragment.class.getSimpleName();

    public static enum ListLayoutType
    {
        FAIRPHONE, ANDROID, APP_STORE
    }

    private ListLayoutType mListLayoutType;
    private List<Version> mVersionList;
	private LinearLayout mVersionListContainer;
    private Button mLatestVersionDetailsButton;
    private TextView mLatestVersionInstalledIndicator;
    private LinearLayout mOlderVersionsGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment

	    return setupLayout(inflater, container);
    }

    private View setupLayout(LayoutInflater inflater, ViewGroup container)
    {

        View view;
        Resources resources = mainActivity.getResources();
        switch (mListLayoutType)
        {
            case APP_STORE:
                view = inflater.inflate(R.layout.fragment_app_store_options_list, container, false);
                mainActivity.updateHeader(HeaderType.APP_STORE, resources.getString(R.string.app_store), true);

                mOlderVersionsGroup = (LinearLayout) view.findViewById(R.id.older_versions_group);
                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                setupAppStoreVersions(container);
                break;
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_other_os_options_android_list, container, false);
                mainActivity.updateHeader(HeaderType.ANDROID, resources.getString(R.string.android_os), true);

                mOlderVersionsGroup = (LinearLayout) view.findViewById(R.id.older_versions_group);
                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                mLatestVersionDetailsButton = (Button) view.findViewById(R.id.other_os_options_android_latest_version_button);
                mLatestVersionInstalledIndicator = (TextView) view.findViewById(R.id.other_os_options_android_version_installed_indicator_text);

                setupAndroidLatestVersion();
                setupAndroidVersions(container);
                break;
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_other_os_options_fairphone_list, container, false);
                mainActivity.updateHeader(HeaderType.FAIRPHONE, resources.getString(R.string.fairphone_os), true);

                mOlderVersionsGroup = (LinearLayout) view.findViewById(R.id.older_versions_group);
                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                mLatestVersionDetailsButton = (Button) view.findViewById(R.id.other_os_options_fairphone_latest_version_button);
                mLatestVersionInstalledIndicator = (TextView) view.findViewById(R.id.other_os_options_fairphone_version_installed_indicator_text);

                setupFairphoneLatestVersion();
                setupFairphoneVersions(container);
                break;
        }
        return view;
    }

    private void setupAppStoreVersions(ViewGroup root)
    {
        Button storeLayout;
        LayoutInflater inflater = getActivity().getLayoutInflater();

	    List<Store> mStoreList = UpdaterData.getInstance().getAppStoreList();

        for (Store store : mStoreList)
        {
            storeLayout = (Button) inflater.inflate(R.layout.fragment_app_store_options_list_button, root, false);
            storeLayout.setTag(store);
            storeLayout.setClickable(true);

            mVersionListContainer.addView(storeLayout);

            storeLayout.setText(store.getName());

            storeLayout.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Store selectedStore = (Store) v.getTag();

                    if (selectedStore != null)
                    {
                        VersionDetailFragment versionDetail = new VersionDetailFragment(false);

                        versionDetail.setupAppStoreFragment(selectedStore);

                        mainActivity.changeFragment(versionDetail);
                    }
                }
            });
        }

        if (mStoreList.size() >= 1)
        {
            mOlderVersionsGroup.setVisibility(View.VISIBLE);
        }
        else
        {
            mOlderVersionsGroup.setVisibility(View.GONE);
        }
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

                versionLayout.setText(mainActivity.getItemName(version));

                versionLayout.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Version selectedVersion = (Version) v.getTag();

                        if (selectedVersion != null)
                        {
                            VersionDetailFragment versionDetail = new VersionDetailFragment(true);

                            versionDetail.setupFragment(selectedVersion, DetailLayoutType.FAIRPHONE);

                            mainActivity.changeFragment(versionDetail);
                        }
                    }
                });
            }
        }
                
        if (mVersionList.size() <= 1)
        {
            mOlderVersionsGroup.setVisibility(View.GONE);
        }
        else
        {
            mOlderVersionsGroup.setVisibility(View.VISIBLE);
        }
    }

    private void setupFairphoneLatestVersion()
    {
        final Version latestFairphoneVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE);
        mLatestVersionDetailsButton.setText(mainActivity.getItemName(latestFairphoneVersion));

        if (mainActivity.getDeviceVersion().compareTo(latestFairphoneVersion) == 0)
        {
            mLatestVersionInstalledIndicator.setVisibility(View.VISIBLE);
        }
        else
        {
            mLatestVersionInstalledIndicator.setVisibility(View.GONE);
        }

        mLatestVersionDetailsButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (latestFairphoneVersion != null)
                {
                    VersionDetailFragment versionDetail = new VersionDetailFragment(true);

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

                versionLayout.setText(mainActivity.getItemName(version));

                versionLayout.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v)
                    {
                        Version selectedVersion = (Version) v.getTag();

                        if (selectedVersion != null)
                        {
                            VersionDetailFragment versionDetail = new VersionDetailFragment(true);

                            versionDetail.setupFragment(selectedVersion, DetailLayoutType.ANDROID);

                            mainActivity.changeFragment(versionDetail);
                        }
                    }
                });
            }
        }
        
        if (mVersionList.size() <= 1)
        {
            mOlderVersionsGroup.setVisibility(View.GONE);
        }
        else
        {
            mOlderVersionsGroup.setVisibility(View.VISIBLE);
        }
    }

    private void setupAndroidLatestVersion()
    {
        final Version latestAOSPVersion = UpdaterData.getInstance().getLatestVersion(Version.IMAGE_TYPE_AOSP);
        mLatestVersionDetailsButton.setText(mainActivity.getItemName(latestAOSPVersion));

        if (mainActivity.getDeviceVersion().compareTo(latestAOSPVersion) == 0)
        {
            mLatestVersionInstalledIndicator.setVisibility(View.VISIBLE);
        }
        else
        {
            mLatestVersionInstalledIndicator.setVisibility(View.GONE);
        }

        mLatestVersionDetailsButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (latestAOSPVersion != null)
                {
                    VersionDetailFragment versionDetail = new VersionDetailFragment(true);

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