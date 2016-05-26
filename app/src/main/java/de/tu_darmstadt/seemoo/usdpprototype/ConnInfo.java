package de.tu_darmstadt.seemoo.usdpprototype;

import java.util.ArrayList;

import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechManager;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;
import de.tu_darmstadt.seemoo.usdpprototype.misc.Helper;

/**
 * Created by kenny on 19.04.16.
 *
 * Report for a single connection
 */
public class ConnInfo {

    //private ArrayList<AuthMechanism> usedMechs = new ArrayList<>();
    private ArrayList<String> usedMechs = new ArrayList<>();

    public ConnInfo() {

    }

    // for every pairing, regardless of its outcome, one entry is added. Currently as a simple String
    public void addAuthMech(String authmech, boolean success) {
        //usedMechs.add(AuthMechManager.getSingleMechByName(authmech));
        AuthMechanism mech = AuthMechManager.getSingleMechByName(authmech);

        long timestamp = System.currentTimeMillis();
        String timeStampDate = (String) Helper.getDate(timestamp);


        String res = timeStampDate +": "+ mech.getShortName() + " (" + mech.getValuation() + " mechVal)";
        if (success) {
            res += " (OK)";
        } else {
            res += " (failed!)";
        }
        usedMechs.add(res);
    }

    @Override
    public String toString() {
        String res = "";
        for (String mech : usedMechs) {
            res += mech + "\n";
        }
        return res;
    }

}
