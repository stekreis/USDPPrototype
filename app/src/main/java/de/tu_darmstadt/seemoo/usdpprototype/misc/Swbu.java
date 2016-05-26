package de.tu_darmstadt.seemoo.usdpprototype.misc;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by kenny on 29.03.16.
 * <p>
 * Shake Well Before Use class, handles accelerometer data and extracts few discrete values
 */
public class Swbu implements SensorEventListener {

    private final static int VAL_AMOUNT = 7;
    private final static int POS_THRESH = 1;
    private final static int NEG_THRESH = -1;
    private final String LOGTAG = "Swbu";
    private SensorManager sensorManager;
    private ArrayList<SwbuPacket> tempList;
    private SwbuPacket lastPosPack;
    private SwbuPacket lastNegPack;

    private boolean wasPositive = true;

    private float[] accval = new float[5];
    private int accPos = 0;
    private SwbuListener swbuListener;

    public Swbu(Context ctx, SwbuListener swbuListener) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        this.swbuListener = swbuListener;
        tempList = new ArrayList<>();
        initPosPack();
        initNegPack();
    }

    private void initPosPack() {
        lastPosPack = new SwbuPacket(Long.MIN_VALUE, Float.MIN_VALUE);
    }

    private void initNegPack() {
        lastNegPack = new SwbuPacket(Long.MIN_VALUE, Float.MAX_VALUE);
    }

    public void stopSensor() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float val = event.values[0];
            long curTime = System.currentTimeMillis();

            //general procedure: only positive values higher than some positive threshold are counted as maximum positive.
            // After negative threshold was exceeded, last positive max is fix. The same procedure runs vice versa.

            if (wasPositive) {
                //is current value higher than previous value?
                if (val > POS_THRESH && val > lastPosPack.getVal()) {
                    lastPosPack.set(curTime, val);
                } else if (val < NEG_THRESH) {
                    // last positive val
                    SwbuPacket pack = new SwbuPacket(lastPosPack.getTimeStamp(), lastPosPack.getVal());
                    tempList.add(pack);
                    initPosPack();
                    lastNegPack.set(curTime, val);
                    wasPositive = false;
                }
            } else {
                if (val < NEG_THRESH && val < lastNegPack.getVal()) {
                    lastNegPack.set(curTime, val);
                } else if (val > POS_THRESH) {
                    // last positive val
                    SwbuPacket pack = new SwbuPacket(lastNegPack.getTimeStamp(), lastNegPack.getVal());
                    tempList.add(pack);
                    initNegPack();
                    lastPosPack.set(curTime, val);
                    wasPositive = true;
                }
            }

            if (tempList.size() >= VAL_AMOUNT) {
                calcShakeVals();
                tempList.clear();

            }
        }
    }

    // merges time and acceleration difference between subsequent max values to digits
    private void calcShakeVals() {
        long timeDiff = 0;
        float valDiff = 0;
        ArrayList<Integer> vals = new ArrayList<>();
        ListIterator<SwbuPacket> iter = tempList.listIterator();
        SwbuPacket last = null;
        if (iter.hasNext()) {
            last = iter.next();
        }
        while (iter.hasNext()) {
            SwbuPacket curr = iter.next();
            timeDiff = curr.getTimeStamp() - last.timeStamp;
            valDiff = curr.getVal() - last.getVal();
            int x = Math.abs(Math.round((timeDiff * valDiff) / 1000));
            if (x > 10) {
                x = 9;
            } else if (x < 0) {
                x = 0;
            }
            vals.add(x);
            last = curr;
        }
        String ret = "";
        for (Integer intVal : vals) {
            ret += String.valueOf(intVal);
        }
        swbuListener.sequenceCompleted(ret);
    }

    public interface SwbuListener {
        void sequenceCompleted(String seq);
    }


    private class SwbuPacket {
        private long timeStamp;
        private float val;

        public SwbuPacket(long timeStamp, float val) {
            this.timeStamp = timeStamp;
            this.val = val;
        }

        public void set(long timeStamp, float val) {
            this.timeStamp = timeStamp;
            this.val = val;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public float getVal() {
            return val;
        }

        public void setVal(float val) {
            this.val = val;
        }
    }
}


