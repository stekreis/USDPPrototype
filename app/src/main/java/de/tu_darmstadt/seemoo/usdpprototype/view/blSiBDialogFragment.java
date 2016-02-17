package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

import de.tu_darmstadt.seemoo.usdpprototype.R;

/**
 * Created by kenny on 15.02.16.
 */
public class blSiBDialogFragment extends DialogFragment {

    private static final String LOGTAG = "blSiBDialogFragment";
    final Handler myHandler = new Handler();
    boolean[] pattern = {true, false, false, true, true, true, false, true, false, true, false, false, true};
    int i = 0;
    Timer myTimer;
    private ImageView iv_blsib;
    final Runnable myRunnable = new Runnable() {
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

        View view = layoutInflater.inflate(R.layout.dialog_auth_blsib, null);

        iv_blsib = (ImageView) view.findViewById(R.id.iv_blsib);
        iv_blsib.setBackgroundColor(Color.LTGRAY);


        builder.setView(view).setPositiveButton("pos", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //TODO do something
            }
        }).setNegativeButton("neg", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                blSiBDialogFragment.this.getDialog().cancel();
            }
        });

        //anim();

        return builder.create();
    }

    public void anim() {
        final boolean[] pattern = {true, false, false, true, true, true, false, true, false, true, false, false, true};

/*
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int timeToBlink = 1000;    //in milliseconds
                try{Thread.sleep(timeToBlink);}catch (Exception e) {}
                handler.post(new Runnable() {
                    private static int pos = 0;
                    @Override
                    public void run() {

                        if(pattern[pos]){
                            iv_blsib.setBackgroundColor(Color.BLUE);
                        }else{
                            iv_blsib.setBackgroundColor(Color.RED);
                        }
                        blink();
                    }
                });
            }
        }).start();


        while(true) {
            for (int pos = 0; pos< pattern.length;pos++){
            new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                if(pattern[pos]){
                    iv_blsib.setBackgroundColor(Color.BLUE);
                }else{
                    iv_blsib.setBackgroundColor(Color.RED);
                }
            }
            }, 1000);


            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
    }

    public boolean isFragmentUIActive() {
        return isAdded() && !isDetached() && !isRemoving();
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