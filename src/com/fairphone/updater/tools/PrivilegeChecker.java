package com.fairphone.updater.tools;


import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public final class PrivilegeChecker {
    private static final String TAG = PrivilegeChecker.class.getSimpleName();

	private static final boolean isPrivilegedApp;

	static {
		// If we have permissions to write instructions to the recovery, we are a privileged app.
		File f = new File("/cache/recovery/command");
		if ( f.exists() ) {
			isPrivilegedApp = f.canWrite();
		} else {
			boolean success = false;
			try {
				f.createNewFile();
				success = f.delete();
			} catch (IOException e) {
				success = false;
			} finally {
				isPrivilegedApp = success;
			}
		}
		Log.d(TAG, "App is "+(isPrivilegedApp ? "" : "not")+" privileged.");
	}

	public static boolean isPrivilegedApp(){
		return isPrivilegedApp;
	}

}
