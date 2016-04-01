package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by kenny on 29.03.16.
 */
public class AuthSwbu implements SensorEventListener {

    private final String LOGTAG = "AuthSwbu";
    private SensorManager sensorManager;

    private float[] accval = new float[10];
    private int accPos = 0;

    public AuthSwbu(Context ctx) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
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



            /*
            Log.d(LOGTAG, ax + "/" + ay + "/" + az);
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
            */
        }if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            Log.d("XVAL",event.values[0] + "");
            //Log.d("YVAL",event.values[0] + "");
            //Log.d("ZVAL",event.values[0] + "");
        }
    }
}
