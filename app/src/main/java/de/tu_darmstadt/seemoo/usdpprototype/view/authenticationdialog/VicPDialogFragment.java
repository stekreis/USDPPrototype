package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 15.02.16.
 */
public class VicPDialogFragment extends AuthDialogFragment {

    private static final String LOGTAG = "VicPDialogFrag";

    public static final String AUTH_VICP = "AUTH_VICP";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");

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

        View view = layoutInflater.inflate(R.layout.dialog_auth_phrase, null);

        TextView tv = (TextView) view.findViewById(R.id.tv_vicp);
        String text = bundle.getString(AUTH_VICP);
        tv.setText(text);

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
                VicPDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

}