package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

/**
 * Created by kenny on 12.04.16.
 */
public class ListDevice {
    private String address = "";
    private String name = "";

    public ListDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
