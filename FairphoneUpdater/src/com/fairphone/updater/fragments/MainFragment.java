package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater2Activity.HeaderType;
import com.fairphone.updater.R;
import com.fairphone.updater.Version;

public class MainFragment extends BaseFragment {

	private TextView mCurrentVersionNameText;
	private LinearLayout mVersionUpToDateGroup;
	// private Button mVersionUpToDateOlderOSVersionButton;
	private LinearLayout mUpdateAvailableGroup;
	// private Button mUpdateAvailableOlderOSVersionButton;
	private TextView mUpdateAvailableNameText;
	private Button mUpdateAvailableInstallButton;
	private Button mOtherOSOptionsButton;
	private Version mDeviceVersion;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_main, container, false);

		setupLayout(view);

		mDeviceVersion =  mainActivity.getDeviceVersion();
		updateHeader();
		updateCurrentVersionGroup();
		toogleUpdateAvailableGroup();
		updateOtherOSOptionsGroup();

		return view;
	}

	private void updateHeader() {
		if(Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mDeviceVersion.getImageType())){
			mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
		}else if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mDeviceVersion.getImageType())){
			mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "");
		}
	}

	private void setupLayout(View view) {
		// Current version group
		mCurrentVersionNameText = (TextView) view
				.findViewById(R.id.current_version_name_text);

		// Version up to date group
		mVersionUpToDateGroup = (LinearLayout) view
				.findViewById(R.id.version_up_to_date_group);
		// mVersionUpToDateOlderOSVersionButton = (Button) view
		// .findViewById(R.id.older_os_version_button);

		// Update available group
		mUpdateAvailableGroup = (LinearLayout) view
				.findViewById(R.id.update_available_group);
		mUpdateAvailableNameText = (TextView) view
				.findViewById(R.id.update_available_name_text);
		mUpdateAvailableInstallButton = (Button) view
				.findViewById(R.id.install_update_button);
		// mUpdateAvailableOlderOSVersionButton = (Button) view
		// .findViewById(R.id.other_os_version_button);

		// Other OS Options group
		mOtherOSOptionsButton = (Button) view
				.findViewById(R.id.other_os_options_button);
	}

	private void updateOtherOSOptionsGroup() {
		mOtherOSOptionsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(mainActivity, "Procced to other OS options",
						Toast.LENGTH_LONG).show();
			}
		});
	}

	public void toogleUpdateAvailableGroup() {
		if (mainActivity.isUpdateAvailable()) {
			mVersionUpToDateGroup.setVisibility(View.GONE);
			mUpdateAvailableGroup.setVisibility(View.VISIBLE);

			updateUpdateAvailableGroup();
		} else {
			mUpdateAvailableGroup.setVisibility(View.GONE);
			mVersionUpToDateGroup.setVisibility(View.VISIBLE);

			// updateOlderVersionsButton();

		}
	}

	// private void updateOlderVersionsButton() {
	// mVersionUpToDateOlderOSVersionButton
	// .setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// Toast.makeText(mainActivity,
	// "Procced to older OS Versions",
	// Toast.LENGTH_LONG).show();
	// }
	// });
	// }

	private void updateUpdateAvailableGroup() {
		mUpdateAvailableNameText.setText(mainActivity.getLatestVersionName());
		mUpdateAvailableInstallButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				VersionDetailFragment fragment = new VersionDetailFragment();

				Version latestVersion = mainActivity.getLatestVersion();
				if (latestVersion != null) {

					fragment.setHeaderType(mainActivity
							.getHeaderTypeFromImageType(latestVersion
									.getImageType()), "Install update PASS",
							"Update version PASS");
					fragment.setVersion(latestVersion);

				}

				mainActivity.changeFragment(fragment);
			}
		});

		// mUpdateAvailableOlderOSVersionButton
		// .setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// Toast.makeText(mainActivity,
		// "Procced to older OS Versions",
		// Toast.LENGTH_LONG).show();
		// }
		// });
	}

	private void updateCurrentVersionGroup() {
		mCurrentVersionNameText.setText(mainActivity.getDeviceVersionName());
		mCurrentVersionNameText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mainActivity.onEnableDevMode();
			}
		});
	}
}
