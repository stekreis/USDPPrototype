package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by kenny on 06.04.16.
 * <p/>
 * manages available authentication mechanisms
 */
public class AuthMechManager {

    // TODO read from file. device may not support all mechanisms
    private String[] authmechs = {"VCI_I", "VCI_N", "VCI_P", "SiB", "SiBBlink", "LACDS", "LACSS", "BEDA_VB", "BEDA_LB", "BEDA_BPB", "BEDA_BTB", "HAPADEP", "SWBU", "NFC"};

    private AuthMechanism[] authmechs2;

    public AuthMechManager() {

    }

    public void readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        readAuthMechs(reader);
        reader.close();
    }

    public void readAuthMechs(JsonReader reader) throws IOException {
        ArrayList<AuthMechanism> authMechList = new ArrayList<AuthMechanism>();

        reader.beginObject();
        Log.d("JSEONteost", reader.nextName());
        reader.beginObject();
/*
        Log.d("JSEONteost2", reader.nextName());
*/
        authMechList = readMessage(reader);


        reader.endObject();

        authmechs2 = authMechList.toArray(new AuthMechanism[authMechList.size()]);
    }

    public ArrayList<AuthMechanism> readMessage(JsonReader reader) throws IOException {
        ArrayList<AuthMechanism> authMechList = new ArrayList<AuthMechanism>();
        String shortName = null;
        String longName = null;
        String usedAuth = null;
        int secPoints = 0;
        ArrayList<String> reqCapSend = new ArrayList<String>();
        ArrayList<String> reqCapRec = new ArrayList<String>();

        String name;
        while (reader.hasNext()) {
            shortName = reader.nextName();
            reader.beginObject();
            while (reader.hasNext()) {

                name = reader.nextName();
                if (name.equals("name")) {
                    longName = reader.nextString();
                    Log.d("jseontseot", longName);
                } else if (name.equals("usedAuth")) {
                    usedAuth = reader.nextString();
                } else if (name.equals("secPoints")) {
                    secPoints = reader.nextInt();
                } else if (name.equals("reqCapSend") && reader.peek() != JsonToken.NULL) {
                    reqCapSend = readDevCaps(reader);
                } else if (name.equals("reqCapRec") && reader.peek() != JsonToken.NULL) {
                    reqCapRec = readDevCaps(reader);
                } else {
                    reader.skipValue();
                }

            }
            reader.endObject();
            authMechList.add(new AuthMechanism(shortName, longName, usedAuth, secPoints, reqCapSend, reqCapRec));
        }


        reader.endObject();
        return authMechList;
    }

    public ArrayList<String> readDevCaps(JsonReader reader) throws IOException {
        ArrayList<String> devCaps = new ArrayList<String>();

        reader.beginArray();
        while (reader.hasNext()) {
            devCaps.add(reader.nextString());
        }
        reader.endArray();
        return devCaps;
    }


    public void parseMechs() {

    }

    private boolean fulfillsReq(ArrayList<String> req, String[] devCaps) {
        ListIterator<String> authReqCaps = req.listIterator();
        boolean found = false;
        while (authReqCaps.hasNext()) {
            String next = authReqCaps.next();
            for (int inpos = 0; inpos < devCaps.length && !found; inpos++) {
                if (devCaps[inpos].toLowerCase().equals(next)) {
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }


    /*
    extract an array of supported authentication mechanisms based on the device capabilities
     */
    public String[] getSupportedMechs2(String[] devCaps) {
        if (authmechs2 != null) {
            ArrayList<String> supMechs = new ArrayList<String>();
            for (int pos = 0; pos < authmechs2.length; pos++) {
                if (fulfillsReq(authmechs2[pos].getReqCapSend(), devCaps) && fulfillsReq(authmechs2[pos].getReqCapRec(), devCaps)) {
                    supMechs.add(authmechs2[pos].getShortName());
                }
            }
            return supMechs.toArray(new String[supMechs.size()]);
        }
        return null;
    }

    public String[] getSupportedMechs() {
        return authmechs;
    }

}
