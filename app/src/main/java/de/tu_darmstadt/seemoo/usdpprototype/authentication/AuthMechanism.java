package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import java.util.ArrayList;

/**
 * Created by kenny on 09.04.16.
 */
public class AuthMechanism implements Comparable<AuthMechanism> {
    private String shortName;
    private String longName;
    private String authUsed;
    private int secPoints = 0;
    private ArrayList<String> reqCapSend = new ArrayList<String>();
    private ArrayList<String> reqCapRec = new ArrayList<String>();

    public AuthMechanism(String shortName, String longName, String authUsed, int secPoints, ArrayList<String> reqCapSend, ArrayList<String> reqCapRec) {
        this.shortName = shortName;
        this.longName = longName;
        this.authUsed = authUsed;
        this.secPoints = secPoints;
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

    @Override
    public int compareTo(AuthMechanism another) {
        if (secPoints > another.getSecPoints()) {
            return -1;
        } else if (secPoints < another.getSecPoints()) {
            return 1;
        }
        return 0;
    }

    public int getSecPoints() {
        return secPoints;
    }
}
