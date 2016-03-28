package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by kenny on 27.03.16.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOGTAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;


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


        int yStride = (int) Math.ceil(width / 16.0) * 16;
//        int yRowIndex = yStride * y;

        //Log.d(LOGTAG, data[yStride]+"/0");

        int total = 0;
        int totalCount = 0;
        int yPos = yStride;
        while (yPos < data.length) {
            totalCount++;
            total += data[yPos];
            yPos += yStride;
        }


        Log.d(LOGTAG, "guggemol " + total/totalCount);
        if(total/totalCount>0) {
            Log.d(LOGTAG, "white");
        }


//        Log.d(LOGTAG, data[2]+"/2");


        return true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    //og.d(LOGTAG, "prevFormat " + camera.getParameters().getPreviewFormat());
                    ledBright(data, camera.getParameters().getPreviewSize().width);
                }
            });

            mCamera.startPreview();
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