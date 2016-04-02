package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by kenny on 27.03.16.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOGTAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private LinkedList<Integer> transmittedVal = new LinkedList<Integer>();


    private long lastTime = 0;


    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private boolean ledBright(byte[] data, int w, int h) {

        byte[] luma = new byte[w * h];
        byte[] chroma = new byte[w * h / 2];
        System.arraycopy(data, 0, luma, 0, w * h);
        System.arraycopy(data, w * h, chroma, 0, w * h / 2);

        int total = 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int Y_at_x_y = luma[x + y * w];           // or data[x + y*w]
                total += Y_at_x_y;
            }
        }

        //Log.d(LOGTAG, "brightness: " + total);
        if (total < 0) {

            return true;
        }
        return false;


    }

    private void checklight(byte[] data, int width, int height) {
        long time = System.currentTimeMillis();
        if (ledBright(data, width, height)) {
            if (lastTime == 0) {
                lastTime = time;
            } else {


                Log.d(LOGTAG, "white");

            }
        } else {
            if (lastTime == 0) {
                // do not react yet
            } else {
                transmittedVal.add(getDigitFromTimeDifference(lastTime, time));
                lastTime = 0;
            }
        }
    }

    private int getDigitFromTimeDifference(long lastTime, long time) {
        long diff = time - lastTime;
        int diffInt = Math.round(((float)diff)/200);
        Log.d("digit", diffInt + "");
        return diffInt;
    }

    private void configCamera() {

        Camera.Parameters parameters = mCamera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        }


        List<Camera.Size> picSize = parameters.getSupportedPreviewSizes();
        ListIterator<Camera.Size> liter = picSize.listIterator();
        Camera.Size smallest = liter.next();
        while (liter.hasNext()) {
            Camera.Size curr = liter.next();
            if (curr.width < smallest.width) {
                smallest = curr;
            }
        }

        //parameters.setPictureSize(smallest.width, smallest.height);
        //parameters.setPictureSize(720, 480);
        parameters.setPreviewSize(smallest.width, smallest.height);

        Log.d("FLATTEN", parameters.flatten());

        //parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
        //parameters.setExposureCompensation(0);

        // prevent camera from dynamically adapting brightness
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(true);
        }

        mCamera.setParameters(parameters);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                checklight(data, camera.getParameters().getPreviewSize().width,  mCamera.getParameters().getPreviewSize().height);
            }
        });

    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(holder);
                configCamera();
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(LOGTAG, e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
/*
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(LOGTAG, e.getMessage());
        }

    */
    }
}