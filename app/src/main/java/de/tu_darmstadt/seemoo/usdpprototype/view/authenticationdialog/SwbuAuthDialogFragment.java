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
import de.tu_darmstadt.seemoo.usdpprototype.misc.Swbu;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 15.02.16.
 */
public class SwbuAuthDialogFragment extends AuthDialogFragment implements Swbu.SwbuListener {

    public static final String AUTH_INFOONLY = "AUTH_INFO_ONLY";
    private static final String LOGTAG = "AuthInfoDialogFrag";
    private Swbu swbu;
    private TextView tv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");
        swbu = new Swbu(getContext(), this);
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

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authinfo_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authinfo_info);
        tv_info.setText(info);

        tv = (TextView) view.findViewById(R.id.tv_authinfo_explinfo);
        String text = bundle.getString(AUTH_INFOONLY);
        tv.setText(text);
        mechType = bundle.getString(AUTH_MECHTYPE);
        tgtDevice = bundle.getString(AUTH_TARGET_DVC);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                swbu.stopSensor();
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobGenAuthResult(tgtDevice, mechType, (String) tv.getText());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                SwbuAuthDialogFragment.this.getDialog().cancel();
                swbu.stopSensor();
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobGenAuthResult(tgtDevice, mechType, null);
            }
        });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swbu.stopSensor();
    }

    @Override
    public void sequenceCompleted(String seq) {
        tv.setText(seq);
    }
}