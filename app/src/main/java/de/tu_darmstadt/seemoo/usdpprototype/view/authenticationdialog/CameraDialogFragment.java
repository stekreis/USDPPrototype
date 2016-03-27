package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.view.CameraPreview;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 15.02.16.
 */
public class CameraDialogFragment extends AuthDialogFragment {

    //    public static final String BARCODE_CODE = "BARCODE_CODE";
    public static final String BARCODE_WIDTH = "BARCODE_WIDTH";
    public static final String BARCODE_HEIGHT = "BARCODE_HEIGHT";

    private static final String LOGTAG = "AuthBarcodeDialogFrag";

    private Bitmap barcode = null;

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");

    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d(LOGTAG, "has camera");
            return true;
        } else {
            // no camera on this device
            Log.d(LOGTAG, "no camera");
            return false;
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View view = layoutInflater.inflate(R.layout.dialog_auth_cam, null);

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authdialogcam_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authdialogcam_info);
        tv_info.setText(info);


        checkCameraHardware(getContext());

        CameraPreview mPreview = new CameraPreview(getContext(), getCameraInstance());
        FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        int width = bundle.getInt(BARCODE_WIDTH);
        int height = bundle.getInt(BARCODE_HEIGHT);

        builder.setView(view).setPositiveButton("pos", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Intent result is received by Activity
                Toast.makeText(getContext(), "checking if zxing is installed", Toast.LENGTH_SHORT).show();
                if (UsdpMainActivity.isPackageInstalled("com.google.zxing.client.android", getContext())) {
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "QR_CODE_MODE");
                    getActivity().startActivityForResult(intent, 0);
                } else {
                    // TODO goto market, install. check this at the start (kind of init app generally)
                    Toast.makeText(getContext(), "install zxing Barcode Scanner", Toast.LENGTH_SHORT).show();
                    Log.d(LOGTAG, "install zxing Barcode Scanner");
                }
            }
        }).setNegativeButton("neg", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                CameraDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

}