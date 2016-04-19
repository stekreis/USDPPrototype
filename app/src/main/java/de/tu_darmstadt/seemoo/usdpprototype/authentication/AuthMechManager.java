package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by kenny on 06.04.16.
 * <p>
 * manages available authentication mechanisms
 */
public class AuthMechManager {
    private static AuthMechanism[] authmechs;

    public AuthMechManager() {

    }

    public static AuthMechanism getSingleMechByName(String name) {
        for (int ipos = 0; ipos < authmechs.length; ipos++) {
            if (authmechs[ipos].getShortName().equals(name)) {
                return authmechs[ipos];
            }
        }
        return null;
    }

    public boolean isCapableOf(String mech) {
        for (int pos = 0; pos < authmechs.length; pos++) {
            if (authmechs[pos].equals(mech)) {
                return true;
            }
        }
        return false;
    }

    public void readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        readAuthMechs(reader);
        reader.close();
    }

    public void readAuthMechs(JsonReader reader) throws IOException {
        ArrayList<AuthMechanism> authMechList = new ArrayList<AuthMechanism>();

        reader.beginObject();
        reader.nextName();
        reader.beginObject();
        authMechList = readMessage(reader);
        reader.endObject();

        authmechs = authMechList.toArray(new AuthMechanism[authMechList.size()]);
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
    public String[] getSupportedMechs(String[] devCaps) {
        if (authmechs != null) {
            ArrayList<String> supMechs = new ArrayList<String>();
            for (int pos = 0; pos < authmechs.length; pos++) {
                if (fulfillsReq(authmechs[pos].getReqCapSend(), devCaps) && fulfillsReq(authmechs[pos].getReqCapRec(), devCaps)) {
                    supMechs.add(authmechs[pos].getShortName());
                }
            }
            return supMechs.toArray(new String[supMechs.size()]);
        }
        return null;
    }

    public String[] getSupportedRecMechs(String[] devCaps) {
        if (authmechs != null) {
            ArrayList<String> supMechs = new ArrayList<String>();
            for (int pos = 0; pos < authmechs.length; pos++) {
                if (fulfillsReq(authmechs[pos].getReqCapRec(), devCaps)) {
                    supMechs.add(authmechs[pos].getShortName());
                }
            }
            return supMechs.toArray(new String[supMechs.size()]);
        }
        return null;
    }

    public String[] getSupportedSendMechs(String[] devCaps) {
        if (authmechs != null) {
            ArrayList<String> supMechs = new ArrayList<String>();
            for (int pos = 0; pos < authmechs.length; pos++) {
                if (fulfillsReq(authmechs[pos].getReqCapSend(), devCaps)) {
                    supMechs.add(authmechs[pos].getShortName());
                }
            }
            return supMechs.toArray(new String[supMechs.size()]);
        }
        return null;
    }

    public void findComp(HashSet<String> res, String[] first, String[] second) {
        for (int firstPos = 0; firstPos < first.length; firstPos++) {
            for (int secPos = 0; secPos < second.length; secPos++) {
                String firstStr = first[firstPos];
                if (firstStr.equals(second[secPos])) {
                    res.add(firstStr);
                }
            }
        }
    }

    public ArrayList<AuthMechanism> getMechsByNames(String[] names) {
        ArrayList<AuthMechanism> res = new ArrayList<>();
        for (int pos = 0; pos < names.length; pos++) {
            for (int ipos = 0; ipos < authmechs.length; ipos++) {
                if (authmechs[ipos].getShortName().equals(names[pos])) {
                    res.add(authmechs[ipos]);
                }
            }
        }
        return res;
    }

    public AuthMechanism[] sortAuthMechsBySec(String[] authmechs) {
        String[] res = new String[authmechs.length];
        ArrayList<AuthMechanism> mechs = getMechsByNames(authmechs);
        Collections.sort(mechs);

        return mechs.toArray(new AuthMechanism[mechs.size()]);
    }


}
