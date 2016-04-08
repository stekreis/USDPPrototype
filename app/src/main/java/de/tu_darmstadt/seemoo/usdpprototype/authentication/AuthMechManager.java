package de.tu_darmstadt.seemoo.usdpprototype.authentication;

/**
 * Created by kenny on 06.04.16.
 *
 * manages available authentication mechanisms
 */
public class AuthMechManager {

    // TODO read from file. device may not support all mechanisms
    private String[] authmechs = {"VCI_I","VCI_N","VCI_P","SiB", "SiBBlink","LACDS","LACSS", "BEDA_VB", "BEDA_LB", "BEDA_BPB", "BEDA_BTB", "HAPADEP","SWBU","NFC"};

    public AuthMechManager(){

    }

    public String[] getSupportedMechs(){
        return authmechs;
    }

}
