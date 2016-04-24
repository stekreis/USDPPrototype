package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;

import java.util.ArrayList;

import de.tu_darmstadt.seemoo.usdpprototype.TwoLineArrayAdapter;
import de.tu_darmstadt.seemoo.usdpprototype.misc.ListDevice;

/**
 * Created by kenny on 03.02.16.
 */
public class DeviceArrayAdapter extends TwoLineArrayAdapter<ListDevice> {
    public DeviceArrayAdapter(Context context, ArrayList<ListDevice> employees) {
        super(context, employees);
    }

    @Override
    public int mapStateToColor(ListDevice listDevice) {
        switch (listDevice.getState()) {
            case WifiP2pDevice.AVAILABLE:
                return Color.BLUE;
            case WifiP2pDevice.INVITED:
                return Color.rgb(0,200,200);
            case WifiP2pDevice.CONNECTED:
                return Color.rgb(0,200,0);
            default:
                return Color.GRAY;
        }
    }

    @Override
    public String lineOneText(ListDevice e) {
        String ret = e.getName();
        if(e.isGroupOwner()){
            ret+=" (GO)";
        }
        return ret;
    }

    @Override
    public String lineTwoText(ListDevice e) {
        return e.getAddress();
    }

}