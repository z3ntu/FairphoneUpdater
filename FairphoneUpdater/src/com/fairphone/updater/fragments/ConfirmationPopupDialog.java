package com.fairphone.updater.fragments;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.fairphone.updater.R;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;

public class ConfirmationPopupDialog extends DialogFragment implements OnEditorActionListener
{
    public interface ConfirmationPopupDialogListener
    {
        void onFinishPopUpDialog(boolean result);
    }

    private String mVersion;
    private ConfirmationPopupDialogListener mCallback;
    private DetailLayoutType mLayoutType;
    private Button mOkButton;
    private Button mCancelButton;
    private CheckBox mConfirmationCheckbox;
    private boolean mIsOSChange;
    private boolean mIsOlderVersion;

    public ConfirmationPopupDialog(String version, boolean isOSChange, boolean isOlderVersion, DetailLayoutType layoutType,
            ConfirmationPopupDialogListener callback)
    {
        // Empty constructor required for DialogFragment
        super();

        mVersion = version;
        mCallback = callback;
        mLayoutType = layoutType;
        mIsOSChange = isOSChange;
        mIsOlderVersion = isOlderVersion;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = null;
        TextView versionNameText = null;

        switch (mLayoutType)
        {
            case UPDATE_ANDROID:
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_download_android_confirmation_popup, container);
                break;
            case UPDATE_FAIRPHONE:
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_download_fairphone_confirmation_popup, container);
                break;
        }

        versionNameText = (TextView) view.findViewById(R.id.installing_version);
        versionNameText.setText(mVersion);

        TextView versionTypeText = (TextView) view.findViewById(R.id.version_type_text);

        if (mIsOSChange)
        {
            versionTypeText.setText(R.string.a_different_os_from_the_current);
        }
        else if (mIsOlderVersion)
        {
            versionTypeText.setText(R.string.an_older_version_of_os);
        }

        mOkButton = (Button) view.findViewById(R.id.confirmation_yes_button);
        mOkButton.setEnabled(false);

        mOkButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ConfirmationPopupDialog.this.dismiss();
                mCallback.onFinishPopUpDialog(true);
            }
        });

        mCancelButton = (Button) view.findViewById(R.id.confirmation_no_button);

        mCancelButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                ConfirmationPopupDialog.this.dismiss();
                mCallback.onFinishPopUpDialog(false);
            }
        });

        mConfirmationCheckbox = (CheckBox) view.findViewById(R.id.confirmation_checkbox);
        mConfirmationCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                mOkButton.setEnabled(isChecked);
            }
        });

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (EditorInfo.IME_ACTION_DONE == actionId)
        {
            // Return input text to activity
            this.dismiss();
            return true;
        }
        return false;
    }

}
