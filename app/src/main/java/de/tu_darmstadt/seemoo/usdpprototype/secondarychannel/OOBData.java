package de.tu_darmstadt.seemoo.usdpprototype.secondarychannel;

/**
 * Created by kenny on 06.04.16.
 */
public class OOBData {

    public static final String VIC_I = "VIC-I";
    public static final String VCI_N = "VIC-N";
    public static final String VIC_P = "VIC-P";

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

    public String getAuthdata() {
        return authdata;
    }
}
