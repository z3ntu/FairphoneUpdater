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
package org.fairphone.launcher.edgeswipe.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.fairphone.launcher.ApplicationInfo;
import org.fairphone.launcher.LauncherModel;

import android.content.ComponentName;

public class EdgeSwipeAppDiscoverer {
	private static EdgeSwipeAppDiscoverer _instance = new EdgeSwipeAppDiscoverer();
	
	private Map<ComponentName, ApplicationInfo> _allApps;

	public static EdgeSwipeAppDiscoverer getInstance() {
		return _instance;
	}

	private EdgeSwipeAppDiscoverer() {
		_allApps = new HashMap<ComponentName, ApplicationInfo>(); 
	}

	public void loadAllApps(ArrayList<ApplicationInfo> allApps ) {
		for (ApplicationInfo applicationInfo : allApps) {
			_allApps.put(applicationInfo.componentName, applicationInfo);
		}
	}

	public ArrayList<ApplicationInfo> getPackages(){
        ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();
        
        for (ApplicationInfo appInfo : _allApps.values()) {
            appList.add(appInfo);
        }
        Collections.sort(appList, LauncherModel.getAppNameComparator());
        
        return appList;
	}

	public ApplicationInfo getApplicationFromComponentName(ComponentName componentName) {
		
        if(_allApps.containsKey(componentName)){
            return _allApps.get(componentName);
		}
		
		return null;
	}
	
	public void removeUninstalledApp(ComponentName componentName) {
        if(_allApps.containsKey(componentName)){
            _allApps.remove(componentName);
		}
	}
}
