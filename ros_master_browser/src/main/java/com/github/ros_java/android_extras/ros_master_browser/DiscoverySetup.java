package com.github.ros_java.android_extras.ros_master_browser;

import android.content.Context;
import com.github.ros_java.jmdns.Zeroconf;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * Configures the zeroconf class for discovery of services.
 */
public class DiscoverySetup extends AsyncTask<Zeroconf, String, Void> {

	private ProgressDialog commencing_dialog; 
	private final Context context;

	public DiscoverySetup(Context context) {
		this.context = context;
	}
	
    protected Void doInBackground(Zeroconf... zeroconfs) {
        if ( zeroconfs.length == 1 ) {
            Zeroconf zconf = zeroconfs[0];
            android.util.Log.i("zeroconf", "*********** Discovery Commencing **************");
            zconf.addListener("_ros-master._tcp","local");
            zconf.addListener("_ros-master._udp","local");
        } else {
        	android.util.Log.i("zeroconf", "Error - DiscoveryTask::doInBackground received #zeroconfs != 1");
        }
        return null;
    }

    protected void onPreExecute() {
		commencing_dialog = ProgressDialog.show(context,
				"Zeroconf Discovery", "Adding listeners...", true);
    }
    protected void onPostExecute(Void result) {
    	commencing_dialog.dismiss();
    }
}
