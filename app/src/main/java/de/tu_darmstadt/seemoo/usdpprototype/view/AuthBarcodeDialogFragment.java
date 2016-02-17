package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.tu_darmstadt.seemoo.usdpprototype.R;

/**
 * Created by kenny on 15.02.16.
 */
public class AuthBarcodeDialogFragment extends DialogFragment {

    public static final String BARCODE_CODE = "BARCODE_CODE";
    public static final String BARCODE_WIDTH = "BARCODE_WIDTH";
    public static final String BARCODE_HEIGHT = "BARCODE_HEIGHT";

    private static final String LOGTAG = "AuthBarcodeDialogFrag";

    private Bitmap barcode = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");
        int width = getArguments().getInt(BARCODE_WIDTH);
        int height = getArguments().getInt(BARCODE_HEIGHT);
        barcode = Bitmap.createBitmap((int[]) getArguments().get(BARCODE_CODE), 0, width, width, height, Bitmap.Config.ARGB_8888);
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

        View view = layoutInflater.inflate(R.layout.dialog_auth_barcode, null);

        ImageView iv_barcode = (ImageView) view.findViewById(R.id.iv_barcode);
        iv_barcode.setImageBitmap(barcode);

        builder.setView(view).setPositiveButton("pos", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Intent result is received by Activity

                if (UsdpMainActivity.isPackageInstalled("com.google.zxing.client", getContext())) {
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
                AuthBarcodeDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    public boolean isFragmentUIActive() {
        return isAdded() && !isDetached() && !isRemoving();
    }

}