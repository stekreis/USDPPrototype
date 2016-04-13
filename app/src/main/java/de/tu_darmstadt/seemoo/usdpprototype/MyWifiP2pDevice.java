package de.tu_darmstadt.seemoo.usdpprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Messenger;

/**
 * Created by kenny on 13.04.16.
 */
public class MyWifiP2pDevice {
    private WifiP2pDevice device;
    private UsdpPacket packet;
    private Messenger mess;

    public MyWifiP2pDevice(WifiP2pDevice device, UsdpPacket packet, Messenger mess) {
        this.device = device;
        this.packet = packet;
        this.mess = mess;
    }
}
