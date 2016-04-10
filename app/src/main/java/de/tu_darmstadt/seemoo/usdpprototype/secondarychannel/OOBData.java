package de.tu_darmstadt.seemoo.usdpprototype.secondarychannel;

import java.io.Serializable;

/**
 * Created by kenny on 06.04.16.
 */
public class OOBData implements Serializable {

    public static final String VIC_I = "VIC-I";
    public static final String VIC_N = "VIC-N";
    public static final String VIC_P = "VIC-P";
    public static final String SiB = "SiB";
    public static final String SiBBlink = "SiBBlink";
    public static final String LaCDS = "LaCDS";
    public static final String LaCSS = "LaCSS";
    public static final String BEDA_VB = "BEDA_VB";
    public static final String BEDA_LB = "BEDA_LB";
    public static final String BEDA_BPBT = "BEDA_BPBT";
    public static final String BEDA_BTBT = "BEDA_BTBT";
    public static final String HAPADEP = "HAPADEP";
    public static final String NFC = "NFC";
    public static final String SWBU = "SWBU";

    private String type;
    private String authdata;
    private boolean roleSend;

    public OOBData(String type, String authdata, boolean roleSend) {
        this.type = type;
        this.authdata = authdata;
        this.roleSend = roleSend;
    }

    public String getType() {
        return type;
    }

    public boolean isSendingDevice() {
        return roleSend;
    }

    public String getAuthdata() {
        return authdata;
    }
}
