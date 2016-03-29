package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import de.tu_darmstadt.seemoo.usdpprototype.R;

/**
 * Created by kenny on 15.02.16.
 */
public class BlSiBDialogFragment extends AuthDialogFragment {

    private static final String LOGTAG = "BlSiBDialogFragment";
    private final Handler myHandler = new Handler();
    private boolean[] pattern = null;
    private int i = 0;
    private Timer myTimer;
    private ImageView iv_blsib;
    private final Runnable myRunnable = new Runnable() {
        public void run() {
            Log.d(LOGTAG, "running " + i);
            if (pattern[i % pattern.length]) {
                iv_blsib.setVisibility(View.VISIBLE);
            } else {
                iv_blsib.setVisibility(View.INVISIBLE);
            }
        }
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View view = layoutInflater.inflate(R.layout.dialog_auth_img, null);

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authdialog_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authdialog_info);
        tv_info.setText(info);

        iv_blsib = (ImageView) view.findViewById(R.id.iv_image);
        iv_blsib.setBackgroundColor(Color.LTGRAY);
        pattern = bundle.getBooleanArray(AUTH_BLSIB);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //TODO do something
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                BlSiBDialogFragment.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    private void UpdateGUI() {
        i++;
        myHandler.post(myRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        i = 0;
        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                UpdateGUI();
            }
        }, 0, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (myTimer != null) {
            myTimer.cancel();
        }
    }
}