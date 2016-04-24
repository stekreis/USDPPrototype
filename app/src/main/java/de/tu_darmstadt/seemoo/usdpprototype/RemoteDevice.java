package de.tu_darmstadt.seemoo.usdpprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Messenger;

import java.net.InetAddress;

import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.MessageManager;

/**
 * Created by kenny on 13.04.16.
 */
public class RemoteDevice {
    private WifiP2pDevice device = null;
    private UsdpPacket packet = null;
    private MessageManager messMan = null;
    private InetAddress inetAddress = null;
    private int symKey = -1;
    private ConnInfo connInfo = null;

    public RemoteDevice(WifiP2pDevice device, UsdpPacket packet, MessageManager messMan, InetAddress inetAddress) {
        this.device = device;
        this.packet = packet;
        this.messMan = messMan;
        this.inetAddress = inetAddress;
    }

    public RemoteDevice(WifiP2pDevice device) {
        this.device = device;
    }

    public ConnInfo getConnInfo() {
        return connInfo;
    }

    public void setConnInfo(ConnInfo connInfo) {
        this.connInfo = connInfo;
    }

    public int getSymKey() {
        return symKey;
    }

    public void setSymKey(int symKey) {
        this.symKey = symKey;
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
