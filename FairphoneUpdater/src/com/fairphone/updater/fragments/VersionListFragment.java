package com.fairphone.updater.fragments;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fairphone.updater.R;
import com.fairphone.updater.UpdaterData;
import com.fairphone.updater.Version;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = setupLayout(inflater, container);

        return view;
    }

    private View setupLayout(LayoutInflater inflater, ViewGroup container)
    {

        View view;
        switch (mListLayoutType)
        {
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_other_os_options_android_list, container, false);

                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                setupAndroidVersions(container);
                break;
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_other_os_options_fairphone_list, container, false);

                mVersionListContainer = (LinearLayout) view.findViewById(R.id.version_list_container);

                setupFairphoneVersions(container);
                break;
        }
        return view;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        //        mVersionListContainer.removeAllViews();
    }

    private void setupFairphoneVersions(ViewGroup root)
    {
        Button versionLayout;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mVersionList = UpdaterData.getInstance().getFairphoneVersionList();

        System.out.println("Values : " + mVersionList.size());
        for (Version version : mVersionList)
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

    private void setupAndroidVersions(ViewGroup root)
    {
        Button versionLayout;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mVersionList = UpdaterData.getInstance().getAOSPVersionList();

        System.out.println("Values : " + mVersionList.size());
        for (Version version : mVersionList)
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

    public void setupFragment(ListLayoutType listType)
    {
        mListLayoutType = listType;
    }
}