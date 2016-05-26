package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import java.util.ArrayList;

/**
 * Created by kenny on 09.04.16.
 *
 * Provides properties for a single authentication mechanism
 */
public class AuthMechanism implements Comparable<AuthMechanism> {
    private String shortName;
    private String longName;
    private String mechDesc;
    private int mechVal = 0;
    private ArrayList<String> reqCapSend = new ArrayList<String>();
    private ArrayList<String> reqCapRec = new ArrayList<String>();

    public AuthMechanism(String shortName, String longName, String mechDesc, int valuation, ArrayList<String> reqCapSend, ArrayList<String> reqCapRec) {
        this.shortName = shortName;
        this.longName = longName;
        this.mechDesc = mechDesc;
        this.mechVal = valuation;
        this.reqCapSend = reqCapSend;
        this.reqCapRec = reqCapRec;
    }

    public String getShortName() {
        return shortName;
    }

    public ArrayList<String> getReqCapSend() {
        return reqCapSend;
    }

    public ArrayList<String> getReqCapRec() {
        return reqCapRec;
    }


    //Compare mechanisms by their valuation
    @Override
    public int compareTo(AuthMechanism another) {
        if (mechVal > another.getValuation()) {
            return -1;
        } else if (mechVal < another.getValuation()) {
            return 1;
        }
        return 0;
    }

    public int getValuation() {
        return mechVal;
    }
}
