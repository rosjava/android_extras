package com.github.rosjava.android_extras.ros_master_browser;

import java.lang.String;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.ZeroconfLogger;

public class Logger implements ZeroconfLogger {

	public void println(String msg) {
		android.util.Log.i("zeroconf", msg);
	}
}
