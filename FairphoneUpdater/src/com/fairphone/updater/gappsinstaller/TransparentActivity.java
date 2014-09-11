package com.fairphone.updater.gappsinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;

import com.fairphone.updater.R;

public class TransparentActivity extends Activity {

	public static final String HIDE_GAPPS_PROGRESS_SPINNER = "HIDE_GAPPS_PROGRESS_SPINNER";
	public static final String SHOW_GAPPS_PROGRESS_SPINNER = "SHOW_GAPPS_PROGRESS_SPINNER";
	public static final String SHOW_GAPPS_WIFI_WARNING_DIALOG = "SHOW_GAPPS_WIFI_WARNING_DIALOG";
	public static final String SHOW_GAPPS_DISCLAIMER_DIALOG = "SHOW_GAPPS_DISCLAIMER_DIALOG";
	public static final String SHOW_GAPPS_REINSTALL_DIALOG = "SHOW_GAPPS_REINSTALL_DIALOG";
	
	public static final String ACTION_SET_GAPPS_REINSTALL_FLAG = "ACTION_SET_GAPPS_REINSTALL_FLAG";
	public static final String ACTION_CHANGE_GAPPS_STATE_TO_INITIAL = "ACTION_CHANGE_GAPPS_STATE_TO_INITIAL";
	
	private ProgressDialog mProgress = null;
	private BroadcastReceiver mBCastProgressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().hide();
		
		String action = getIntent().getAction();
		
		if(SHOW_GAPPS_REINSTALL_DIALOG.equals(action)){
			showReinstallAlert();
		}else if(SHOW_GAPPS_DISCLAIMER_DIALOG.equals(action)){
			showDisclaimer();
		}else if(SHOW_GAPPS_WIFI_WARNING_DIALOG.equals(action)){
			showWifiWarning();
		}else if(SHOW_GAPPS_PROGRESS_SPINNER.equals(action)){
			showProgressSpinner();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mBCastProgressBar = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				hideProgressSpinner();
			}
		};

		registerReceiver(mBCastProgressBar, new IntentFilter(
				HIDE_GAPPS_PROGRESS_SPINNER));
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		unregisterReceiver(mBCastProgressBar);
	}
	
	public void showReinstallAlert() {
        Resources resources = getResources();

        AlertDialog reinstallDialog = new AlertDialog.Builder(this)
                .create();

        reinstallDialog.setTitle(resources
                .getText(R.string.google_apps_reinstall_request_title));

        // Setting Dialog Message
        reinstallDialog.setMessage(resources
                .getText(R.string.google_apps_reinstall_description));

        reinstallDialog
                .setButton(
                        AlertDialog.BUTTON_POSITIVE,
                        resources
                                .getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                            	Intent i = new Intent(ACTION_SET_GAPPS_REINSTALL_FLAG);
                            	sendBroadcast(i);
                            	
                            	finish();
                            }
                        });

        reinstallDialog.show();
    }
	
	private void showDisclaimer() {
		Resources resources = getResources();

		AlertDialog disclaimerDialog = new AlertDialog.Builder(this)
				.create();

		disclaimerDialog.setTitle(resources
				.getText(R.string.google_apps_disclaimer_title));

		// Setting Dialog Message
		disclaimerDialog.setMessage(resources
				.getText(R.string.google_apps_disclaimer_description));

		disclaimerDialog
				.setButton(
						AlertDialog.BUTTON_POSITIVE,
						resources
								.getString(R.string.google_apps_disclaimer_agree),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								Intent startDownloadOkIntent = new Intent();
								startDownloadOkIntent
										.setAction(GappsInstallerHelper.GAPPS_ACTION_DOWNLOAD_CONFIGURATION_FILE);

								sendBroadcast(startDownloadOkIntent);
								
								TransparentActivity.this.finish();
							}
						});

		disclaimerDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
				resources.getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						
						changeGappsStateToInitial();
					}
				});

		disclaimerDialog.show();
	}

	private void showWifiWarning() {
		AlertDialog disclaimerDialog = new AlertDialog.Builder(
				this).create();

		Resources resources = getResources();

		disclaimerDialog.setTitle(resources
				.getText(R.string.google_apps_connection_title));

		// Setting Dialog Message
		disclaimerDialog
				.setMessage(resources
						.getText(R.string.google_apps_connection_description));

		disclaimerDialog.setButton(AlertDialog.BUTTON_POSITIVE,
				resources.getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						changeGappsStateToInitial();
					}
				});

		disclaimerDialog.show();
	}

	private void changeGappsStateToInitial() {
		Intent i = new Intent(ACTION_CHANGE_GAPPS_STATE_TO_INITIAL);
		sendBroadcast(i);
		
		finish();
	}
	
	private void showProgressSpinner() {            
        if(mProgress == null){
            String title = "";
            String message = getResources().getString(R.string.pleaseWait);
            mProgress = ProgressDialog.show(this, title, message, true, false);
        }
    }

	private void hideProgressSpinner() {
        // disable the spinner
        if(mProgress != null){
            mProgress.dismiss();
            mProgress = null;
        }
        
        finish();
    }
}
