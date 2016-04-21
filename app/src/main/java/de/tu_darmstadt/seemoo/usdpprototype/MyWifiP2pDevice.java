package de.tu_darmstadt.seemoo.usdpprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Messenger;

import java.net.InetAddress;

import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.MessageManager;

/**
 * Created by kenny on 13.04.16.
 */
public class MyWifiP2pDevice {
    private WifiP2pDevice device;
    private UsdpPacket packet;
    private MessageManager messMan;
    private InetAddress inetAddress;

    public MyWifiP2pDevice(WifiP2pDevice device, UsdpPacket packet, MessageManager messMan, InetAddress inetAddress) {
        this.device = device;
        this.packet = packet;
        this.messMan = messMan;
        this.inetAddress = inetAddress;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public WifiP2pDevice getDevice() {
        return device;
    }

    public void setDevice(WifiP2pDevice device) {
        this.device = device;
    }

    public UsdpPacket getPacket() {
        return packet;
    }

    public void setPacket(UsdpPacket packet) {
        this.packet = packet;
    }

    public MessageManager getMessMan() {
        return messMan;
    }

    public void setMessMan(MessageManager messMan) {
        this.messMan = messMan;
    }
}
