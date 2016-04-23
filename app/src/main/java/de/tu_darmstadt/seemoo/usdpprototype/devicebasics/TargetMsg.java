package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

import java.io.Serializable;

/**
 * Created by kenny on 23.04.16.
 */
public class TargetMsg implements Serializable{
    private static final long serialVersionUID = 601696025421177797L;
    private String targetAddress = null;
    private String senderAddress = null;
    private Object obj = null;

    public TargetMsg(String targetAddress, Object obj) {
        this.targetAddress = targetAddress;
        this.obj = obj;
    }
    public TargetMsg(String senderAddress, String targetAddress, Object obj) {
        this(targetAddress, obj);
        this.senderAddress = senderAddress;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
