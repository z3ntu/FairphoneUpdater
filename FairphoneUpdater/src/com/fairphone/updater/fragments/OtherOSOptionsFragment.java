package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.fairphone.updater.R;
import com.fairphone.updater.fragments.VersionListFragment.ListLayoutType;

public class OtherOSOptionsFragment extends BaseFragment {

	private static final String TAG = OtherOSOptionsFragment.class
			.getSimpleName();
	private Button olderFairphoneOSButton;
	private Button androidOSButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_other_os_options,
				container, false);

		olderFairphoneOSButton = (Button)view.findViewById(R.id.older_fairphone_os_button);
		androidOSButton = (Button)view.findViewById(R.id.android_os_button);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		olderFairphoneOSButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VersionListFragment newFragment = new VersionListFragment();
				newFragment.setupFragment(ListLayoutType.FAIRPHONE);
				mainActivity.changeFragment(newFragment);
			}
		});
		
		androidOSButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VersionListFragment newFragment = new VersionListFragment();
				newFragment.setupFragment(ListLayoutType.ANDROID);
				mainActivity.changeFragment(newFragment);
			}
		});
	}
}