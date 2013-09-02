/*
 * Copyright (C) 2013 Yujin Robot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.rosjava.android_extras.ros_master_browser;

import java.io.IOException;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.Zeroconf;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.ZeroconfDiscoveryHandler;
import com.github.rosjava.android_extras.ros_master_browser.R;
import com.github.rosjava.android_extras.ros_master_browser.DiscoveryAdapter;
import com.github.rosjava.android_extras.ros_master_browser.DiscoveryHandler;
import com.github.rosjava.android_extras.ros_master_browser.DiscoverySetup;
import com.github.rosjava.android_extras.ros_master_browser.Logger;

// adb logcat System.out:I *:S
// adb logcat zeroconf:I *:S

/**
 * Master browser does discovery for the following services
 * (both xxx._tcp and xxx._udp):
 * 
 * - _ros-master  
 * 
 * Easiest way to test is to use avahi to publish/browse on the other end. In a linux shell:
 * 
 * @code
 * > avahi-publish -s DudeMaster _ros-master._tcp 8882
 * @endcode
 *
 * Then run this program on your android while it is connected to the lan.
 */
public class MasterBrowser extends Activity {
	
	/********************
	 * Variables
	 *******************/
	private Zeroconf zeroconf;
	private Logger logger;
	private TextView tv;
	private ListView lv;
	private ArrayList<DiscoveredService> discovered_services;
    private DiscoveryAdapter discovery_adapter;
	private DiscoveryHandler discovery_handler;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger = new Logger();
    	android.util.Log.i("zeroconf","*********** Zeroconf Create **************");
        discovered_services = new ArrayList<DiscoveredService>();
        setContentView(R.layout.main);
        lv = (ListView)findViewById(R.id.discovered_services_view);
        discovery_adapter = new DiscoveryAdapter(this, discovered_services);
        lv.setAdapter(discovery_adapter);
        tv = (TextView)findViewById(R.id.mytextview);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText("");

		zeroconf = new Zeroconf(logger);
		discovery_handler = new DiscoveryHandler(tv, discovery_adapter, discovered_services);
		zeroconf.setDefaultDiscoveryCallback(discovery_handler);

		new DiscoverySetup(this).execute(zeroconf);
    }
    
    @Override
    public void onPause() {
    	logger.println("*********** Zeroconf Pause **************");
		super.onPause();
    }

    @Override
    public void onStop() {
    	logger.println("*********** Zeroconf Stop **************");
		super.onStop();
    }
    
    
    @Override
    public void onDestroy() {
    	logger.println("*********** Zeroconf Destroy **************");
        zeroconf.removeListener("_ros-master._tcp","local");
	    zeroconf.removeListener("_ros-master._udp","local");
	    try {
	    	zeroconf.shutdown();
        } catch (IOException e) {
	        e.printStackTrace();
        }
		super.onDestroy();
    }
}
