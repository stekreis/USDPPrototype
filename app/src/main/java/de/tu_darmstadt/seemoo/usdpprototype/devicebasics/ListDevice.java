package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by kenny on 12.04.16.
 */
public class ListDevice {
    private String address = "";
    private String name = "";

    private int state = WifiP2pDevice.AVAILABLE;

    public ListDevice(String address, String name, int state) {
        this.address = address;
        this.name = name;
        this.state = state;
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
