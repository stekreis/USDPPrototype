package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;

/**
 * Created by kenny on 15.02.16.
 */
public class BEDAButtonDialogFragment extends AuthDialogFragment {

    public static final String AUTH_BTN = "AUTH_BTN";
    private static final String LOGTAG = "VerificationButtonDialogFragment";

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

        View view = layoutInflater.inflate(R.layout.dialog_auth_btn, null);

        Button userbtn = (Button) view.findViewById(R.id.btn_recauth);

        userbtn.setOnTouchListener(new ReceiverButtonListener());

/*        TextView tv = (TextView) view.findViewById(R.id.tv_vicp);
        String text = bundle.getString(AUTH_BTN);
        tv.setText(text);
*/
        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Intent result is received by Activity


            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                BEDAButtonDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    public void test() {

    }

}

class ReceiverButtonListener implements View.OnTouchListener {
    private static final String LOGTAG = "ReceiverBtnListener";
    private final long DOWNTHRESHOLD = 1000;
    private final long UPTHRESHOLD = 2000;
    private long lastUp = 0;
    private long lastDown = 0;
    private long lastDuration = 0;
    private ArrayList<Boolean> data = new ArrayList<>();

    private boolean decide(long duration) {
        return duration > DOWNTHRESHOLD;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long curr = System.currentTimeMillis();
            if (curr -lastUp > UPTHRESHOLD) {
                if(!data.isEmpty()){
                    // TODO: send data
                    int val = Helper.getInt(Helper.getPrimitiveArrayMsbFront(data));
                    Log.d(LOGTAG, "value: " + val);
                }
                data.clear();
            }
            lastDown = curr;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            lastUp = System.currentTimeMillis();
            lastDuration = lastUp - lastDown;
            data.add(decide(lastDuration));
            Log.d(LOGTAG, lastDuration + " ms");
        }
        return false;
    }


}
