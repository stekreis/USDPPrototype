package de.tu_darmstadt.seemoo.usdpprototype.misc;

import java.io.Serializable;

/**
 * Created by kenny on 06.04.16.
 *
 * Bundle for OOB data, used to move OOBdata from Service to MainActivity for authentication
 */
public class OOBData implements Serializable {

    public static final String VIC_I = "VC-I";
    public static final String VIC_N = "VC-N";
    public static final String VIC_P = "VC-P";
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
    private static final long serialVersionUID = 584801670551829147L;
    private String type;
    private int authdata;
    private boolean roleSend;
    public OOBData(String type, int authdata, boolean roleSend) {
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

    public int getAuthdata() {
        return authdata;
    }

    public void setAuthdata(int authdata) {
        this.authdata = authdata;
    }
}
