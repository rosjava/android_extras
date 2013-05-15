package org.ros.android.zeroconf.master_browser;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.widget.TextView;
import org.ros.zeroconf.jmdns.ZeroconfDiscoveryHandler;

/**
 * This class is the callback handler for services being listened for
 * by the jmdns zeroconf class. 
 * 
 * Usually we should do a bit of checking to make sure that any 
 * service isn't getting repeated on another interface, but for
 * now we can assume your android has only the one interface so that
 * we handle each added/resolved/removed as a unique entry.
 */
public class DiscoveryHandler implements ZeroconfDiscoveryHandler {

	/*********************
	 * Tasks
	 ********************/
	private class ServiceAddedTask extends AsyncTask<zeroconf_msgs.DiscoveredService, String, Void> {
		
	    protected Void doInBackground(zeroconf_msgs.DiscoveredService... services) {
	        if ( services.length == 1 ) {
	            zeroconf_msgs.DiscoveredService service = services[0];
				String result = "[+] Service added: " + service.getName() + "." + service.getType() + "." + service.getDomain() + ".";
				publishProgress(result);
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }

	    protected void onProgressUpdate(String... progress) {
	    	uiLog(progress);
		}
	}

	private class ServiceResolvedTask extends AsyncTask<zeroconf_msgs.DiscoveredService, String, zeroconf_msgs.DiscoveredService> {
		
	    protected zeroconf_msgs.DiscoveredService doInBackground(zeroconf_msgs.DiscoveredService... services) {
	        if ( services.length == 1 ) {
	            zeroconf_msgs.DiscoveredService discovered_service = services[0];
		    	String result = "[=] Service resolved: " + discovered_service.getName() + "." + discovered_service.getType() + "." + discovered_service.getDomain() + ".\n";
		    	result += "    Port: " + discovered_service.getPort();
		    	for ( String address : discovered_service.getIpv4Addresses() ) {
		    		result += "\n    Address: " + address;
		    	}
		    	for ( String address : discovered_service.getIpv6Addresses() ) {
		    		result += "\n    Address: " + address;
		    	}
		    	publishProgress(result);
		    	return discovered_service;
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }

	    protected void onProgressUpdate(String... progress) {
	    	uiLog(progress);
		}
	    
	    protected void onPostExecute(zeroconf_msgs.DiscoveredService discovered_service) {
	    	// add to the content and notify the list view if its a new service
	    	if ( discovered_service != null ) {
				int index = 0;
				for ( zeroconf_msgs.DiscoveredService s : discovered_services ) {
					if ( s.getName().equals(discovered_service.getName()) ) {
						break;
					} else {
						++index;
					}
				}
				if ( index == discovered_services.size() ) {
					discovered_services.add(discovered_service);
					discovery_adapter.notifyDataSetChanged();
				} else {
					android.util.Log.i("zeroconf", "Tried to add an existing service (fix this)");
				}
	    	}
	    }
	}
	
	private class ServiceRemovedTask extends AsyncTask<zeroconf_msgs.DiscoveredService, String, zeroconf_msgs.DiscoveredService> {
		
	    protected zeroconf_msgs.DiscoveredService doInBackground(zeroconf_msgs.DiscoveredService... services) {
	        if ( services.length == 1 ) {
	            zeroconf_msgs.DiscoveredService discovered_service = services[0];
	            String result = "[-] Service removed: " + discovered_service.getName() + "." + discovered_service.getType() + "." + discovered_service.getDomain() + ".\n";
	            result += "    Port: " + discovered_service.getPort();
		    	for ( String address : discovered_service.getIpv4Addresses() ) {
		    		result += "\n    Address: " + address;
		    	}
		    	for ( String address : discovered_service.getIpv6Addresses() ) {
		    		result += "\n    Address: " + address;
		    	}
		    	publishProgress(result);
		    	return discovered_service;
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }

	    protected void onProgressUpdate(String... progress) {
	    	uiLog(progress);
		}
	    
	    protected void onPostExecute(zeroconf_msgs.DiscoveredService discovered_service) {
	    	// remove service from storage and notify list view
	    	if ( discovered_service != null ) {
				int index = 0;
				for ( zeroconf_msgs.DiscoveredService s : discovered_services ) {
					if ( s.getName().equals(discovered_service.getName()) ) {
						break;
					} else {
						++index;
					}
				}
				if ( index != discovered_services.size() ) {
					discovered_services.remove(index);
					discovery_adapter.notifyDataSetChanged();
				} else {
					android.util.Log.i("zeroconf", "Tried to remove a non-existant service");
				}
	    	}
	    }
	}

	/*********************
	 * Variables
	 ********************/
	private ArrayList<zeroconf_msgs.DiscoveredService> discovered_services;
    private DiscoveryAdapter discovery_adapter;
	private TextView text_view;

	/*********************
	 * Constructors
	 ********************/
	public DiscoveryHandler(TextView tv, DiscoveryAdapter discovery_adapter, ArrayList<zeroconf_msgs.DiscoveredService> discovered_services) {
		this.text_view = tv;
		this.discovery_adapter = discovery_adapter;
		this.discovered_services = discovered_services;
	}

	/*********************
	 * Callbacks
	 ********************/
	public void serviceAdded(zeroconf_msgs.DiscoveredService service) {
		new ServiceAddedTask().execute(service);
	}
	
	public void serviceRemoved(zeroconf_msgs.DiscoveredService service) {
		new ServiceRemovedTask().execute(service);
	}
	
	public void serviceResolved(zeroconf_msgs.DiscoveredService service) {
		new ServiceResolvedTask().execute(service);
	}

	/*********************
	 * Utility
	 ********************/
	private void uiLog(String... messages) {
        for (String msg : messages ) {
            android.util.Log.i("zeroconf", msg);
            text_view.append(msg + "\n");
    	}
    	int line_count = text_view.getLineCount(); 
    	int view_height = text_view.getHeight();
    	int pixels_per_line = text_view.getLineHeight();
    	int pixels_difference = line_count*pixels_per_line - view_height;
    	if ( pixels_difference > 0 ) {
    		text_view.scrollTo(0, pixels_difference);
    	}
	}
}