package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;

import java.util.ArrayList;

import de.tu_darmstadt.seemoo.usdpprototype.TwoLineArrayAdapter;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.ListDevice;

/**
 * Created by kenny on 03.02.16.
 */
public class DeviceArrayAdapter extends TwoLineArrayAdapter<ListDevice> {
    public DeviceArrayAdapter(Context context, ArrayList<ListDevice> employees) {
        super(context, employees);
    }

    @Override
    public String lineOneText(ListDevice e) {
        return e.getName();
    }

    @Override
    public String lineTwoText(ListDevice e) {
        return e.getAddress();
    }
}