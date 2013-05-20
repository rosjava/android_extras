package org.ros.android.zeroconf.master_browser;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import android.os.AsyncTask;
import android.widget.TextView;
import org.ros.zeroconf.jmdns.ZeroconfDiscoveryHandler;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.ServiceInfoImpl;

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
	private class ServiceAddedTask extends AsyncTask<ServiceInfo, String, Void> {
		
	    protected Void doInBackground(ServiceInfo... services) {
	        if ( services.length == 1 ) {
	            ServiceInfo service = services[0];
            	// getType() usually returns name, protocol and domain in jmdns, e.g.
            	// _ros-master._tcp.local. Retrieve the name and protocol only for our purposes
	            String type = service.getType();
            	String[] everything = service.getType().split("\\.");
            	if (everything.length > 1) {
            	    type = everything[0] + "." + everything[1];
            	}
				String result = "[+] Service added: " + service.getName() + "." + type + "." + service.getDomain() + ".";
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

	private class ServiceResolvedTask extends AsyncTask<ServiceInfo, String, ServiceInfo> {
		
	    protected ServiceInfo doInBackground(ServiceInfo... services) {
	        if ( services.length == 1 ) {
	            ServiceInfo discovered_service = services[0];
            	// getType() usually returns name, protocol and domain in jmdns, e.g.
            	// _ros-master._tcp.local. Retrieve the name and protocol only for our purposes
	            String type = discovered_service.getType();
            	String[] everything = discovered_service.getType().split("\\.");
            	if (everything.length > 1) {
            	    type = everything[0] + "." + everything[1];
            	}
		    	String result = "[=] Service resolved: " + discovered_service.getName() + "." + type + "." + discovered_service.getDomain() + ".\n";
		    	result += "    Port: " + discovered_service.getPort();
		    	for ( Inet4Address address : discovered_service.getInet4Addresses() ) {
		    		result += "\n    Address: " + address.getHostAddress();
		    	}
		    	for ( Inet6Address address : discovered_service.getInet6Addresses() ) {
		    		result += "\n    Address: " + address.getHostAddress();
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
	    
	    protected void onPostExecute(ServiceInfo discovered_service) {
	    	// add to the content and notify the list view if its a new service
	    	// this is a bit horrible - ServiceInfo objects can't add extra addresses,
	    	// so we just discard one if it doesn't have a complete list of ip's
	    	// (quite often we get just an ipv4 address come in, or just an ipv6 address
	    	// but also a service info with both).
	    	if ( discovered_service != null ) {
				Iterator<ServiceInfo> it = discovered_services.iterator();
				Boolean exists = false;
				Boolean must_replace = false;
				while ( it.hasNext() )
				{
					ServiceInfo s = it.next();
					if ( s.getName().equals(discovered_service.getName()) ) {
				    	for ( Inet4Address address : discovered_service.getInet4Addresses() ) {
				    		if ( !Arrays.asList(s.getInet4Addresses()).contains(address) ) {
				    			it.remove();
				    			must_replace = true;
				    			break;
				    		}
				    	}
				    	for ( Inet6Address address : discovered_service.getInet6Addresses() ) {
				    		if ( !Arrays.asList(s.getInet6Addresses()).contains(address) ) {
				    			it.remove();
				    			must_replace = true;
				    			break;
				    		}
				    	}
				    	exists = true;
						break;
					}
				}
				if ( exists ) {
					if ( must_replace ) {
						discovered_services.add(discovered_service);
					} else {
						android.util.Log.i("zeroconf", "Tried to add an existing service (fix this)");
					}
				} else {
					discovered_services.add(discovered_service);
					discovery_adapter.notifyDataSetChanged();
				}
	    	}
	    }
	}
	
	private class ServiceRemovedTask extends AsyncTask<ServiceInfo, String, ServiceInfo> {
		
	    protected ServiceInfo doInBackground(ServiceInfo... services) {
	        if ( services.length == 1 ) {
	            ServiceInfo discovered_service = services[0];
            	// getType() usually returns name, protocol and domain in jmdns, e.g.
            	// _ros-master._tcp.local. Retrieve the name and protocol only for our purposes
	            String type = discovered_service.getType();
            	String[] everything = discovered_service.getType().split("\\.");
            	if (everything.length > 1) {
            	    type = everything[0] + "." + everything[1];
            	}
	            String result = "[-] Service removed: " + discovered_service.getName() + "." + type + "." + discovered_service.getDomain() + ".";
	            // Address and port information is not usually transferred here, you'd have
	            // to look it up on stored objects here in this program if you wanted to work out
	            // which was removed.
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
	    
	    protected void onPostExecute(ServiceInfo discovered_service) {
	    	// remove service from storage and notify list view
	    	if ( discovered_service != null ) {
				int index = 0;
				for ( ServiceInfo s : discovered_services ) {
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
	private ArrayList<ServiceInfo> discovered_services;
    private DiscoveryAdapter discovery_adapter;
	private TextView text_view;

	/*********************
	 * Constructors
	 ********************/
	public DiscoveryHandler(TextView tv, DiscoveryAdapter discovery_adapter, ArrayList<ServiceInfo> discovered_services) {
		this.text_view = tv;
		this.discovery_adapter = discovery_adapter;
		this.discovered_services = discovered_services;
	}

	/*********************
	 * Callbacks
	 ********************/
	public void serviceAdded(ServiceInfo service) {
		new ServiceAddedTask().execute(service);
	}
	
	public void serviceRemoved(ServiceInfo service) {
		new ServiceRemovedTask().execute(service);
	}
	
	public void serviceResolved(ServiceInfo service) {
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