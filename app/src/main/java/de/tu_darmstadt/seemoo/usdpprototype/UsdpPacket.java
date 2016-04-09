package de.tu_darmstadt.seemoo.usdpprototype;

import java.io.Serializable;

/**
 * Created by kenny on 09.04.16.
 */
public class UsdpPacket implements Serializable {

    private String uniqueId;
    private String protVersion;
    private String[] mechsRec;
    private String[] mechsSend;


    public UsdpPacket(String[] mechsRec, String[] mechsSend) {
        this.mechsRec = mechsRec;
        this.mechsSend = mechsSend;
    }

    public UsdpPacket(String uniqueId, String protVersion) {
        this.uniqueId = uniqueId;
        this.protVersion = protVersion;
    }


    public String[] getMechsRec() {
        return mechsRec;
    }

    public String[] getMechsSend() {
        return mechsSend;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getProtVersion() {
        return protVersion;
    }
}
