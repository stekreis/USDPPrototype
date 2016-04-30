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
 */
public class Swbu implements SensorEventListener {

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

    public Swbu(Context ctx) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];


            //Log.d(LOGTAG, ax + "/" + ay + "/" + az);
            float totalAcc = Math.abs(ax) + Math.abs(ay) + Math.abs(az);
            Log.d(LOGTAG, "" + totalAcc);

            accval[accPos++] = totalAcc;
            if (accPos >= accval.length) {
                float max = 0;
                for (int pos = 0; pos < accval.length; pos++) {
                    if (accval[pos] > max) {
                        max = accval[pos];
                    }
                }
                Log.d(LOGTAG, "MAX: " + max);
                accPos = 0;
                accval = new float[accval.length];
            }

        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float val = event.values[0];
            /*long curTime = event.timestamp;*/
            long curTime = System.currentTimeMillis();
            //Log.d("XVAL", val + "");

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

            if (tempList.size() >= 7) {
                /*printMe();*/
                calcShakeVals();
                tempList.clear();

            }

            //


            //Log.d("YVAL",event.values[0] + "");
            //Log.d("ZVAL",event.values[0] + "");
        }
    }

    private void calcShakeVals(){
        long timeDiff =0;
        float valDiff = 0;
        ArrayList<Integer> vals = new ArrayList<>();
        ListIterator<SwbuPacket> iter = tempList.listIterator();
        SwbuPacket last = null;
        if(iter.hasNext()){
            last = iter.next();
        }
        while(iter.hasNext()){
            SwbuPacket curr = iter.next();
            timeDiff = curr.getTimeStamp() - last.timeStamp;
            valDiff = curr.getVal() - last.getVal();
            int x = Math.abs(Math.round((timeDiff*valDiff)/1000));
            if(x>10){
                x=9;
            }else if(x<0){
                x=0;
            }
            vals.add(x);
            last = curr;
        }
        String str = "";
        for (Integer intVal : vals) {
              str += String.valueOf(intVal) + "\n";
        }
        Log.d("TESTACC222", str);
    }


    private void printMe() {
        String str = "";
        for (SwbuPacket pack : tempList) {
            str += pack.getTimeStamp() + "//" + pack.getVal() + "\n";
        }
        Log.d("TESTACC", str);
    }

    private class SwbuPacket {
        private long timeStamp;
        private float val;

        public SwbuPacket(long timeStamp, float val) {
            this.timeStamp = timeStamp;
            this.val = val;
        }

        // Copy Constructor
        public SwbuPacket(SwbuPacket packet) {
            this.timeStamp = packet.getTimeStamp();
            this.val = packet.getTimeStamp();
        }

        public void set(long timeStamp, float val) {
            this.timeStamp = timeStamp;
            this.val = val;
            Log.d("XVAL", val + "");
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


