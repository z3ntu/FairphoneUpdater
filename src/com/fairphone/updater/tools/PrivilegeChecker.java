package com.fairphone.updater.tools;


import android.util.Log;

import java.io.File;

public final class PrivilegeChecker {
    private static final String TAG = PrivilegeChecker.class.getSimpleName();

	private static final boolean isPrivilegedApp;

	static {
		// If we have permissions to write instructions to the recovery, we are a privileged app.
		File command = new File("/cache/recovery/command");
        File command2 = new File("/cache/command");
		isPrivilegedApp = command.canWrite();
        Log.wtf(TAG, "comand: " + command.canWrite() + "\ncommand2: "+ command2.canWrite());
	}

	public static boolean isPrivilegedApp(){
		return isPrivilegedApp;
	}

}
