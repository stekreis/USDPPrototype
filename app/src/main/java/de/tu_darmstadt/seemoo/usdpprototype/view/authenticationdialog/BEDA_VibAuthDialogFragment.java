package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 11.04.16.
 */
public class BEDA_VibAuthDialogFragment extends AuthDialogFragment {
    private static final String LOGTAG = "LEDBlinkAuthDialogFragment";
    private Vibrator vib;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        boolean[] pattern = bundle.getBooleanArray(AUTH_PATTERN);
        long[] vibPattern = new long[pattern.length * 2];
        for (int pos = 0; pos < pattern.length; pos++) {
            vibPattern[pos * 2] = 500;
            if (pattern[pos]) {
                vibPattern[pos * 2 + 1] = 1500;
            } else {
                vibPattern[pos * 2 + 1] = 500;
            }
        }
        vibPattern[0] = 3000;
        vib.vibrate(vibPattern, 0);


        View view = layoutInflater.inflate(R.layout.dialog_auth_infoonly, null);

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authinfo_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authinfo_info);
        tv_info.setText(info);

        TextView tv = (TextView) view.findViewById(R.id.tv_authinfo_explinfo);
        String text = bundle.getString(AUTH_EXPLINFO);
        tv.setText(text);
        tgtDevice = bundle.getString(AUTH_TARGET_DVC);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                vib.cancel();
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(tgtDevice, OOBData.BEDA_VB, true);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                vib.cancel();
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(tgtDevice, OOBData.BEDA_VB, false);
                BEDA_VibAuthDialogFragment.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onDestroy() {
        if (vib != null) {
            vib.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}