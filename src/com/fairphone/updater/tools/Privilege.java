package com.fairphone.updater.tools;


import java.io.File;

public final class Privilege {
	private static boolean isPrivilegedApp;

	static {
		// If we have permissions to write instructions to the recovery, we are a privileged app.
		File command = new File("/cache/recovery/command");
		isPrivilegedApp = command.canWrite();
	}

	public static boolean isPrivilegedApp(){
		return isPrivilegedApp;
	}

}
