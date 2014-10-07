package com.fairphone.updater.fragments;

import java.util.Locale;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fairphone.updater.FairphoneUpdater2Activity.HeaderType;
import com.fairphone.updater.R;
import com.fairphone.updater.Version;

public class VersionDetailFragment extends BaseFragment {
	private HeaderType mHeaderType;
	private String mHeaderText;
	private TextView mVersion_details_title_text;
	private TextView mVersion_release_notes_text;
	private LinearLayout mVersion_warnings_group;
	private TextView mVersion_warnings_text;
	private Button mDownload_and_update_button;
	private TextView mVersion_details_name_text;
	private String mVersionDetailsTitle;
	private Version mSelectedVersion;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_update_version_detail, container, false);
		
		setupLayout(view);
		
		mainActivity.updateHeader(mHeaderType, mHeaderText);
		updateVersionName();
		updateReleaseNotesText();
		updateVersionWarningsGroup();
		setupDownloadAndUpdateButton();
		
        return view;
    }

	private void setupLayout(View view) {
		mVersion_details_title_text = (TextView) view.findViewById(R.id.version_details_title_text);
		mVersion_details_name_text = (TextView) view.findViewById(R.id.version_details_name_text);
		
		mVersion_release_notes_text= (TextView) view.findViewById(R.id.version_release_notes_text);
		
		//Version warnings group
		mVersion_warnings_group= (LinearLayout) view.findViewById(R.id.version_warnings_group);
		mVersion_warnings_text= (TextView) view.findViewById(R.id.version_warnings_text);
		
		mDownload_and_update_button = (Button) view.findViewById(R.id.download_and_update_button);
	}

	private void setupDownloadAndUpdateButton() {
		mDownload_and_update_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Launch version download fragment
				mainActivity.setSelectedVersion(mSelectedVersion);
				mainActivity.startVersionDownload();
			}
		});
	}
	
	private void updateVersionWarningsGroup()
    {
        if (mSelectedVersion != null)
        {
        	String warnings = mSelectedVersion.getWarningNotes(Locale.getDefault().getLanguage());
        	if(!TextUtils.isEmpty(warnings)){
        		mVersion_warnings_group.setVisibility(View.VISIBLE);
        		mVersion_warnings_text.setText(warnings);
        	}else{
        		mVersion_warnings_group.setVisibility(View.GONE);
        	}
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
        mVersion_details_name_text.setText(mainActivity.getSelectedVersionName());
    }
	
	public void setHeaderType(HeaderType type, String headerText, String versionDetailsTitle){
		mHeaderType = type;
		mHeaderText = headerText;
		mVersionDetailsTitle = versionDetailsTitle;
	}
	
	public void setVersion(Version version){
		mSelectedVersion = version;
	}
}
