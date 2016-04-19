package de.tu_darmstadt.seemoo.usdpprototype.authentication;

/**
 * Created by kenny on 19.04.16.
 */
public class AuthResult {
    private String mech;
    private String data;
    private boolean success;

    public AuthResult(String mech, String data) {
        this.mech = mech;
        this.data = data;
    }

    public AuthResult(String mech, boolean success) {
        this.mech = mech;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMech() {
        return mech;
    }

    public void setMech(String mech) {
        this.mech = mech;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
