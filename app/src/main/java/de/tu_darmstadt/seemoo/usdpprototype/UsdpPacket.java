package de.tu_darmstadt.seemoo.usdpprototype;

import java.io.Serializable;

/**
 * Created by kenny on 09.04.16.
 */
public class UsdpPacket implements Serializable {
    private static final long serialVersionUID = 8200060948728170659L;

    private String uniqueId;
    private String protVersion;
    private String[] mechsRec;
    private String[] mechsSend;
    private int publicKey;


    public UsdpPacket(String[] mechsRec, String[] mechsSend, int publicKey) {
        this.mechsRec = mechsRec;
        this.mechsSend = mechsSend;
        this.publicKey = publicKey;
    }

    public UsdpPacket(String uniqueId, String protVersion, int publicKey) {
        this.uniqueId = uniqueId;
        this.protVersion = protVersion;
        this.publicKey = publicKey;
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

    public int getRemoteDevPublicKey() {
        return publicKey;
    }
}
