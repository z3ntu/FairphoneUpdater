package com.fairphone.updater.tools;


import android.util.Log;

import java.io.File;
import java.io.IOException;

public final class PrivilegeChecker {
    private static final String TAG = PrivilegeChecker.class.getSimpleName();

	private static final boolean isPrivilegedApp;

	static {
		// If we have permissions to write instructions to the recovery, we are a privileged app.
		File f = new File("/cache/test.txt");
        boolean success = false;
        try {
            success = f.createNewFile() && f.delete();
        } catch (IOException ignored) {
            success = false;
        } finally {
            isPrivilegedApp = success;
        }
		Log.i(TAG, "App is " + (isPrivilegedApp ? "" : "not") + " privileged.");
	}

	public static boolean isPrivilegedApp(){
		return isPrivilegedApp;
	}

}
