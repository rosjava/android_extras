package org.ros.android.zeroconf.master_browser;

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import javax.jmdns.ServiceInfo;
import java.net.Inet4Address;
import java.net.Inet6Address;

public class DiscoveryAdapter extends ArrayAdapter<ServiceInfo> {

	private final Context context;
	private ArrayList<ServiceInfo> discovered_services;

    public DiscoveryAdapter(Context context, ArrayList<ServiceInfo> discovered_services) {
        super(context, R.layout.row_layout,discovered_services); // pass the list to the super
        this.context = context;
        this.discovered_services = discovered_services;  // keep a pointer locally so we can play with it
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row_layout, null);
        }
        ServiceInfo discovered_service = discovered_services.get(position);
        if (discovered_service != null) {
                TextView tt = (TextView) v.findViewById(R.id.service_name);
                TextView bt = (TextView) v.findViewById(R.id.service_detail);
                if (tt != null) {
                    tt.setText(discovered_service.getName());                            
                }
                if( bt != null ) {
                	String result = "";
                	for ( Inet4Address address : discovered_service.getInet4Addresses() ) {
                		if ( result.equals("") ) {
                			result += address.getHostAddress() + ":" + discovered_service.getPort();
                		} else { 
                			result += "\n" + address.getHostAddress() + ":" + discovered_service.getPort();
                		}
                	}
                	for ( Inet6Address address : discovered_service.getInet6Addresses() ) {
                		if ( result.equals("") ) {
                			result += address.getHostAddress() + ":" + discovered_service.getPort();
                		} else { 
                			result += "\n" + address.getHostAddress() + ":" + discovered_service.getPort();
                		}
                	}
                    bt.setText(result);
                }
                ImageView im = (ImageView) v.findViewById(R.id.icon);
                if ( im != null ) {
            	    String type = discovered_service.getType();
                	// getType() usually returns name, protocol and domain in jmdns, e.g.
                	// _ros-master._tcp.local. Retrieve the name and protocol only for our purposes
                	String[] everything = discovered_service.getType().split("\\.");
                	if (everything.length > 1) {
                	    type = everything[0] + "." + everything[1];
	                	if ( type.equals("_ros-master._tcp" ) ||
	                		 type.equals("_ros-master._udp" ) ) {
	                    	im.setImageDrawable(context.getResources().getDrawable(R.drawable.turtle));
	                	}
                	} else {
                		// unknown
                    	im.setImageDrawable(context.getResources().getDrawable(R.drawable.conductor));
                	}
                }
        }
        return v;
    }
}