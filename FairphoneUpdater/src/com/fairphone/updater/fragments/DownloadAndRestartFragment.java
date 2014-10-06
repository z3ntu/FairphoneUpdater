package com.fairphone.updater.fragments;

import android.app.DownloadManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fairphone.updater.FairphoneUpdater2Activity.HeaderType;
import com.fairphone.updater.FairphoneUpdater2Activity.UpdaterState;
import com.fairphone.updater.R;
import com.fairphone.updater.Version;
import com.fairphone.updater.tools.Utils;

public class DownloadAndRestartFragment extends BaseFragment {

	protected static final String TAG = DownloadAndRestartFragment.class
			.getSimpleName();

	private TextView mDownloadVersionName;
	private LinearLayout mVersionDownloadingGroup;
	private ProgressBar mVersionDownloadProgressBar;
	private LinearLayout mVersionInstallGroup;
	private Button mRestartButton;
	private Button mCancelButton;
	private Version mSelectedVersion;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		mSelectedVersion = mainActivity.getSelectedVersion();
		View view = inflateViewByImageType(inflater, container);

		setupLayout(view);
		
		updateHeader();
		mDownloadVersionName.setText(mainActivity
				.getVersionName(mSelectedVersion));

		toggleDownloadProgressAndRestart();

		return view;
	}

	private void toggleDownloadProgressAndRestart() {
		switch (mainActivity.getCurrentUpdaterState()) {
		case DOWNLOAD:
			startDownloadProgressUpdateThread();

			mVersionInstallGroup.setVisibility(View.GONE);
			mVersionDownloadingGroup.setVisibility(View.VISIBLE);
			break;

		case PREINSTALL:
			mVersionDownloadingGroup.setVisibility(View.GONE);
			mVersionInstallGroup.setVisibility(View.VISIBLE);
			break;

		default:
			break;
		}
	}

	private void updateHeader() {
		if (mSelectedVersion != null) {
			if (Version.IMAGE_TYPE_FAIRPHONE.equalsIgnoreCase(mSelectedVersion
					.getImageType())) {
				mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
			} else if (Version.IMAGE_TYPE_AOSP
					.equalsIgnoreCase(mSelectedVersion.getImageType())) {
				mainActivity.updateHeader(HeaderType.MAIN_ANDROID, "");
			}
		} else {
			mainActivity.updateHeader(HeaderType.MAIN_FAIRPHONE, "");
		}
	}

	private void startDownloadProgressUpdateThread() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				boolean downloading = true;

				long latestUpdateDownloadId = mainActivity.getLatestDownloadId();
				while (latestUpdateDownloadId  != 0 && downloading) {

					DownloadManager.Query q = new DownloadManager.Query();
					q.setFilterById(latestUpdateDownloadId);

					Cursor cursor = mainActivity.getDownloadManger().query(q);
					if (cursor != null) {
						cursor.moveToFirst();
						try {
							int bytes_downloaded = cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
							int bytes_total = cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

							if ((bytes_total + 10000) > Utils
									.getAvailablePartitionSizeInBytes(Environment
											.getExternalStorageDirectory())) {
								downloading = false;
								Toast.makeText(
										mainActivity,
										getResources()
												.getString(
														R.string.noSpaceAvailableSdcard),
										Toast.LENGTH_LONG).show();
								mainActivity.changeState(UpdaterState.NORMAL);
							}

							if (cursor.getInt(cursor
									.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
								downloading = false;

								bytes_downloaded = 0;
								bytes_total = 0;
							}

							mVersionDownloadProgressBar
									.setProgress(bytes_downloaded);
							mVersionDownloadProgressBar
									.setMax(bytes_total);
						} catch (Exception e) {
							downloading = false;
							Log.e(TAG,
									"Error updating download progress: "
											+ e.getMessage());
						}

						cursor.close();
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	private View inflateViewByImageType(LayoutInflater inflater,
			ViewGroup container) {
		View view = inflater.inflate(R.layout.fragment_download_fairphone,
				container, false);
		if (mSelectedVersion != null) {
			if (Version.IMAGE_TYPE_AOSP.equalsIgnoreCase(mSelectedVersion
					.getImageType())) {
				view = inflater.inflate(R.layout.fragment_download_android,
						container, false);
			} else if (Version.IMAGE_TYPE_FAIRPHONE
					.equalsIgnoreCase(mSelectedVersion.getImageType())) {
				view = inflater.inflate(R.layout.fragment_download_fairphone,
						container, false);
			}
		}
		return view;
	}

	private void setupLayout(View view) {
		mDownloadVersionName = (TextView) view
				.findViewById(R.id.download_version_name_text);

		// download in progress group
		mVersionDownloadingGroup = (LinearLayout) view
				.findViewById(R.id.version_downloading_group);
		mVersionDownloadProgressBar = (ProgressBar) view
				.findViewById(R.id.version_download_progress_bar);

		// restart group
		mVersionInstallGroup = (LinearLayout) view
				.findViewById(R.id.version_install_group);
		mRestartButton = (Button) view.findViewById(R.id.restart_button);

		mCancelButton = (Button) view.findViewById(R.id.cancel_button);
	}

}
