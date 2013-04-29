package org.ros.android.zeroconf.master_browser;

import java.lang.String;
import org.ros.zeroconf.jmdns.ZeroconfLogger;

public class Logger implements ZeroconfLogger {

	public void println(String msg) {
		android.util.Log.i("zeroconf", msg);
	}
}