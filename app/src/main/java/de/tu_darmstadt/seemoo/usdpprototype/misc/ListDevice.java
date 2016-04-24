package de.tu_darmstadt.seemoo.usdpprototype.misc;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by kenny on 12.04.16.
 */
public class ListDevice {
    private String address = "";
    private String name = "";
    private boolean isGroupOwner;
    private int state = WifiP2pDevice.AVAILABLE;

    public ListDevice(String address, String name, int state, boolean isGroupOwner) {
        this.address = address;
        this.name = name;
        this.state = state;
        this.isGroupOwner = isGroupOwner;
    }

    public boolean isGroupOwner() {
        return isGroupOwner;
    }

    public void setGroupOwner(boolean groupOwner) {
        isGroupOwner = groupOwner;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getState() {
        return state;
    }
}
