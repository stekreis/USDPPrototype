package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.Swbu;

/**
 * Created by kenny on 15.02.16.
 */
public class AuthInfoDialogFragment extends AuthDialogFragment {

    public static final String AUTH_INFOONLY = "AUTH_INFO_ONLY";
    private static final String LOGTAG = "AuthInfoDialogFrag";
    Swbu sif;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");
        sif = new Swbu(getContext());
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

        View view = layoutInflater.inflate(R.layout.dialog_auth_infoonly, null);

        TextView tv = (TextView) view.findViewById(R.id.tv_authdialog_explinfo);
        String text = bundle.getString(AUTH_INFOONLY);
        tv.setText(text);


        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AuthInfoDialogFragment.this.getDialog().cancel();
                sif.stopSensor();
            }
        });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sif.stopSensor();
    }
}