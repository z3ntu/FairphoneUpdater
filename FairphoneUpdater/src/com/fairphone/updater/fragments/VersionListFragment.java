package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fairphone.updater.R;

public class VersionListFragment extends BaseFragment {

	private static final String TAG = VersionListFragment.class.getSimpleName();

	public static enum ListLayoutType {
		FAIRPHONE, ANDROID
	}

	private ListLayoutType mListLayoutType;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = setupLayout(inflater, container);

		return view;
	}

	private View setupLayout(LayoutInflater inflater, ViewGroup container) {
		View view;
		switch (mListLayoutType) {
		case ANDROID:
			view = inflater.inflate(
					R.layout.fragment_other_os_options_android_list, container,
					false);
			break;
		case FAIRPHONE:
		default:
			view = inflater.inflate(
					R.layout.fragment_other_os_options_fairphone_list,
					container, false);
			break;
		}
		return view;
	}

	public void setupFragment(ListLayoutType listType) {
		mListLayoutType = listType;
	}
}