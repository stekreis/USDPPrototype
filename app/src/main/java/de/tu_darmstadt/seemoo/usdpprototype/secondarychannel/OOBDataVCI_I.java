package de.tu_darmstadt.seemoo.usdpprototype.secondarychannel;

/**
 * Created by kenny on 06.04.16.
 */
public class OOBDataVCI_I extends OOBData{
    private String authText = "";

    public OOBDataVCI_I(){

    }

    public void setAuthText(String authText) {
        this.authText = authText;
    }

    public String getAuthText(){
        return authText;
    }

    @Override
    public String getType() {
        return VCI_I;
    }
}
