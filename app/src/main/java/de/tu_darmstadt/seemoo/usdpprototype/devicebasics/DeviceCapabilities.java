package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

import java.util.ArrayList;

/**
 * Created by kenny on 08.04.16.
 */
public class DeviceCapabilities {
    public final static String[] capTitles = {"Camera", "Display", "Speaker", "Microphone", "Vibration", "LED", "Accelerometer", "NFC"};
    private final static int CAMERA = 0;
    private final static int DISPLAY = 1;
    private final static int SPEAKER = 2;
    private final static int MIC = 3;
    private final static int VIB = 4;
    private final static int LED = 5;
    private final static int ACCEL = 6;
    private final static int NFC = 7;
    private boolean[] deviceCaps = new boolean[8];

    public DeviceCapabilities() {
        for (int pos = 0; pos < deviceCaps.length; pos++) {
            deviceCaps[pos] = false;
        }
    }

    //TODO parse from file

    public void setCapability(int capability, boolean val) {
        deviceCaps[capability] = val;
    }

    public boolean isCapableOf(int capability) {
        return deviceCaps[capability];
    }

    public boolean[] getCapabilities() {
        return deviceCaps;
    }

    public void setCapabilities(boolean[] deviceCaps) {
        this.deviceCaps = deviceCaps;
    }

    public String[] getValidCapabilities() {
        ArrayList<String> caps = new ArrayList<>();
        for (int pos = 0; pos < deviceCaps.length; pos++) {
            if (deviceCaps[pos]) {
                caps.add(capTitles[pos]);
            }
        }
        return caps.toArray(new String[caps.size()]);
    }

}
