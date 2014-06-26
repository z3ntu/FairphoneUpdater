/*
 * Copyright (C) 2013 Fairphone Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fairphone.launcher.util;

import java.io.File;

import android.util.Log;

public class Utils {
	
	private static final String TAG = Utils.class.getSimpleName();

	public static double getPartitionSizeInGBytes(File path) {
		double availableBlocks = getPartitionSizeInBytes(path);
		double sizeInGB = (((double) availableBlocks / 1024d) / 1024d) / 1024d;
		Log.d(TAG, path.getPath() + " size(GB): " + sizeInGB);
		return sizeInGB;
	}

	public static double getPartitionSizeInMBytes(File path) {
		double availableBlocks = getPartitionSizeInBytes(path);
		double sizeInMB = (((double) availableBlocks / 1024d)) / 1024d;
		Log.d(TAG, path.getPath() + " size(MB): " + sizeInMB);
		return sizeInMB;
	}

	public static long getPartitionSizeInBytes(File path) {
		android.os.StatFs stat = new android.os.StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getBlockCount() * blockSize;
		return availableBlocks;
	}
}
