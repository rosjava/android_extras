package org.ros.android.zeroconf.master_browser;

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.ros.message.zeroconf_msgs.DiscoveredService;

public class DiscoveryAdapter extends ArrayAdapter<DiscoveredService> {

	private final Context context;
	private ArrayList<DiscoveredService> discovered_services;

    public DiscoveryAdapter(Context context, ArrayList<DiscoveredService> discovered_services) {
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
        DiscoveredService discovered_service = discovered_services.get(position);
        if (discovered_service != null) {
                TextView tt = (TextView) v.findViewById(R.id.service_name);
                TextView bt = (TextView) v.findViewById(R.id.service_detail);
                if (tt != null) {
                    tt.setText(discovered_service.name);                            
                }
                if( bt != null ) {
                	String result = "";
                	for ( String address : discovered_service.ipv4_addresses ) {
                		if ( result.equals("") ) {
                			result += address + ":" + discovered_service.port;
                		} else { 
                			result += "\n" + address + ":" + discovered_service.port;
                		}
                	}
                	for ( String address : discovered_service.ipv6_addresses ) {
                		if ( result.equals("") ) {
                			result += address + ":" + discovered_service.port;
                		} else { 
                			result += "\n" + address + ":" + discovered_service.port;
                		}
                	}
                    bt.setText(result);
                }
                ImageView im = (ImageView) v.findViewById(R.id.icon);
                if ( im != null ) {
                	if ( discovered_service.type.equals("_ros-master._tcp" ) ||
                		 discovered_service.type.equals("_ros-master._udp" ) ) {
                    	im.setImageDrawable(context.getResources().getDrawable(R.drawable.turtle));
                	} else if ( 
                		 discovered_service.type.equals("_app-manager._tcp" ) ||
                   		 discovered_service.type.equals("_app-manager._udp" ) ) {
                    	im.setImageDrawable(context.getResources().getDrawable(R.drawable.app_manager));
                	} else {
                    	im.setImageDrawable(context.getResources().getDrawable(R.drawable.conductor));
                	}
                }
        }
        return v;
    }
}