package org.ros.android.zeroconf.master_browser;

import android.content.Context;
import ros.zeroconf.jmdns.Zeroconf;
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
//			try {
//	    		Thread.sleep(2000L);
//		    } catch (InterruptedException e) {
//		        e.printStackTrace();
//		    }
            zconf.addListener("_ros-master._tcp","local");
            zconf.addListener("_ros-master._udp","local");
            zconf.addListener("_concert-master._tcp","local");
            zconf.addListener("_concert-master._udp","local");
            zconf.addListener("_app-manager._tcp","local");
            zconf.addListener("_app-manager._udp","local");
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