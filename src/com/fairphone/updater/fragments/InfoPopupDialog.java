package com.fairphone.updater.fragments;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.fairphone.updater.R;
import com.fairphone.updater.fragments.VersionDetailFragment.DetailLayoutType;

@SuppressLint("ValidFragment")
public class InfoPopupDialog extends DialogFragment implements OnEditorActionListener
{
    private final DetailLayoutType mLayoutType;

	public InfoPopupDialog(DetailLayoutType layoutType)
    {
        // Empty constructor required for DialogFragment
        super();

        mLayoutType = layoutType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view;

        switch (mLayoutType)
        {
            case UPDATE_ANDROID:
            case ANDROID:
                view = inflater.inflate(R.layout.fragment_info_android_popup, container);
                break;
            case UPDATE_FAIRPHONE:
            case FAIRPHONE:
            default:
                view = inflater.inflate(R.layout.fragment_info_fairphone_popup, container);
                break;
        }

	    Button mOkButton = (Button) view.findViewById(R.id.confirmation_yes_button);

        mOkButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
		        InfoPopupDialog.this.dismiss();
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
