
package com.fairphone.updater;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.fairphone.updater.tools.Utils;

public class BetaEnabler extends Activity {

    private static final String FAIRPHONE_BETA_PROPERTY = "fairphone.ota.beta";
    private static final String BETA_DISABLED = "0";
    private static final String BETA_ENABLED = "1";
    
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
                    Utils.setprop(FAIRPHONE_BETA_PROPERTY, BETA_ENABLED);
                    if (isBetaEnabled()) {
                        Button b = (Button) findViewById(R.id.beta_activator);
                        b.setEnabled(false);
                        b.setText(R.string.beta_is_enabled);
                        b.setOnClickListener(null);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.beta_activation_failed, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    
    private boolean isBetaEnabled(){
        return Utils.getprop(FAIRPHONE_BETA_PROPERTY, BETA_DISABLED).equals(BETA_ENABLED);
    }
    
}
