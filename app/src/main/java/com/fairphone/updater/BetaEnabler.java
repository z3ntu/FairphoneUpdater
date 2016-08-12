
package com.fairphone.updater;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.fairphone.updater.tools.Utils;

public class BetaEnabler extends Activity {

    public static final String BETA_DISABLED = "0";
    public static final String BETA_ENABLED = "1";
    
    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_beta_enabler);
        
        Button b = (Button) findViewById(R.id.beta_activator);
        
        if(!isBetaEnabled()){
            b.setEnabled(true);
            b.setText(R.string.enable_beta);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.enableBeta(getApplicationContext());
                    if (isBetaEnabled()) {
                        Button b = (Button) findViewById(R.id.beta_activator);
                        b.setEnabled(false);
                        b.setText(R.string.beta_is_enabled);
                        b.setOnClickListener(null);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Utils.restartUpdater(BetaEnabler.this);
                            }
                        }, 1000);

                    } else {
                        Toast.makeText(getApplicationContext(), R.string.beta_activation_failed, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private boolean isBetaEnabled(){
        SharedPreferences settings = getSharedPreferences(FairphoneUpdater.FAIRPHONE_UPDATER_PREFERENCES, Context.MODE_PRIVATE);
        return settings.getBoolean(FairphoneUpdater.PREFERENCE_BETA_MODE, getResources().getBoolean(R.bool.defaultBetaStatus));
    }
    
}
