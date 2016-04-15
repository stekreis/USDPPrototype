package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;

/**
 * SurfaceView to show camera preview and
 */

// TODO adapt to Camera2 API
@SuppressWarnings("deprecation")
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    public final static int BLSIB_DURATION_FALSE = 500;
    public final static int BLSIB_DURATION_TRUE = BLSIB_DURATION_FALSE * 2;
    public final static int BLSIB_DURATION_MAXERR = BLSIB_DURATION_FALSE / 2;
    private final static int CAM_ERR = -1;
    private final static int CAM_FALSE = 0;
    private final static int CAM_TRUE = 1;
    private static final String LOGTAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private LinkedList<Integer> transmittedVal = new LinkedList<Integer>();
    private ArrayList<Boolean> transmittedBinary = null;
    private long brightTimeBegin = 0;
    private long firstTimeDark = 0;


    public CameraSurfaceView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    private boolean ledBright(byte[] data, int w, int h) {
        int total = 0;
        int size = w * h;

        // first two thirds of NV21 (YUV420) color space data is Y (luminance)
        for (int x = 0; x < size; x++) {
            total += data[x];
        }

        //Log.d(LOGTAG, "brightness: " + total);
        return (total < 0);
    }

    private boolean isNewSequence(long oldTime, long newTime) {
        return oldTime != 0 && (newTime - oldTime) > BLSIB_DURATION_FALSE * 3 - BLSIB_DURATION_MAXERR;
    }

    private int checkInterval(long oldTime, long newTime) {
        long timeDiff = newTime - oldTime;

        if (timeDiff > (BLSIB_DURATION_FALSE - BLSIB_DURATION_MAXERR) && newTime - oldTime < (BLSIB_DURATION_TRUE + BLSIB_DURATION_MAXERR)) {
            if (timeDiff > (BLSIB_DURATION_FALSE + BLSIB_DURATION_MAXERR)) {
                return CAM_TRUE;
            } else {
                return CAM_FALSE;
            }
        }
        return CAM_ERR;
    }


    private void checklight(byte[] data, int width, int height) {
        long curTime = System.currentTimeMillis();
        if (ledBright(data, width, height)) {
            //led glows
            if (isNewSequence(firstTimeDark, curTime)) {
                //dark time interval exceeded
                if (transmittedBinary != null) {
                    Log.d(LOGTAG, "sequence completed");
                    Log.d("recvd val: ", Helper.getInt(Helper.getPrimitiveArrayMsbFront(transmittedBinary)) + "");

                    // sequence completed
                    //checkSequence
                    //sendSequence
                    //transmittedBinary = null;
                }
                transmittedBinary = new ArrayList<>();
            } else if (brightTimeBegin == 0) {
                brightTimeBegin = curTime;
            } else {
                // wait until bright sequence completed

            }
            firstTimeDark = 0;
        } else {
            if (brightTimeBegin != 0 && transmittedBinary != null) {
                int check = checkInterval(brightTimeBegin, curTime);
                switch (check) {
                    case 0:
                        transmittedBinary.add(false);
                        break;
                    case 1:
                        transmittedBinary.add(true);
                        break;
                    default:
                        // do nothing
                }
            }
            brightTimeBegin = 0;

            if (firstTimeDark == 0) {
                firstTimeDark = curTime;
            } else {
// wait until dark sequence completed
            }
        }
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
        parameters.setPreviewSize(smallest.width, smallest.height);

        /*Log.d("FLATTEN", parameters.flatten());*/


        //parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
        //parameters.setExposureCompensation(0);

        // prevent camera from dynamically adapting brightness
        // TODO get SGS4 camera working with this setting
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(true);
        }

        mCamera.setParameters(parameters);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                checklight(data, camera.getParameters().getPreviewSize().width, mCamera.getParameters().getPreviewSize().height);
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