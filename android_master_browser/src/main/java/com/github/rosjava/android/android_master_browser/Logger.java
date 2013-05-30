package com.github.rosjava.android.master_browser;

import java.lang.String;
import com.github.rosjava.jmdns.ZeroconfLogger;

public class Logger implements ZeroconfLogger {

	public void println(String msg) {
		android.util.Log.i("zeroconf", msg);
	}
}