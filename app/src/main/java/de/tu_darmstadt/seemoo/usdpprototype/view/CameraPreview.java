package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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

    private boolean ledBright(byte[] data, int width) {

// TODO check if correctly addressed brightness
        int yStride = (int) Math.ceil(width / 16.0) * 16;


        int total = 0;
        int totalCount = 0;
        int yPos = yStride;
        while (yPos < data.length) {
            totalCount++;
            total += data[yPos];
            yPos += yStride;
        }


        Log.d(LOGTAG, "brightness: " + total / totalCount);
        if (total / totalCount > 50) {

            return true;
        }
        return false;
    }

    private void checklight(byte[] data, int width) {
        long time = System.currentTimeMillis();
        if (ledBright(data, width)) {
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

        int diffInt = (int) diff;
        Log.d("digit", diffInt + "");
        return diffInt;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if (mCamera != null) {
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(holder);
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                    Log.d(LOGTAG, "using macro!");
                }
                mCamera.setParameters(parameters);
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        //og.d(LOGTAG, "prevFormat " + camera.getParameters().getPreviewFormat());
                        checklight(data, camera.getParameters().getPreviewSize().width);
                    }
                });

                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(LOGTAG, "Error setting camera preview: " + e.getMessage());
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
            Log.d(LOGTAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}