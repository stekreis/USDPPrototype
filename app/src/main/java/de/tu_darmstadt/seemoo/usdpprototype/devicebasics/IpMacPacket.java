package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by kenny on 22.04.16.
 */
public class IpMacPacket implements Serializable {
    private static final long serialVersionUID = 1049362891172852933L;
    private String clientMacAddress = null;
    private InetAddress clientIp = null;

    public IpMacPacket(InetAddress clientIp, String clientMacAddress) {
        this.clientIp = clientIp;
        this.clientMacAddress = clientMacAddress;
    }

    public String getClientMacAddress() {
        return clientMacAddress;
    }

    public void setClientMacAddress(String clientMacAddress) {
        this.clientMacAddress = clientMacAddress;
    }

    public InetAddress getClientIp() {
        return clientIp;
    }

    public void setClientIp(InetAddress clientIp) {
        this.clientIp = clientIp;
    }
}
