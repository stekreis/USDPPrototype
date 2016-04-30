package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.DTMF;
import be.tarsos.dsp.pitch.Goertzel;
import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.misc.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 15.02.16.
 */
public class HapaRecAuthDialogFragment extends AuthDialogFragment {

    public static final String AUTH_VICP = "AUTH_DATA";
    private static final String LOGTAG = "VicPDialogFrag";
    String humVerifData;
    private Thread goertzhread;
    private AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100, 2048, 1024);
    private String dtmfCharacters = "";
    private final AudioProcessor goertzelAudioProcessor = new Goertzel(44100, 256, DTMF.DTMF_FREQUENCIES, new Goertzel.FrequenciesDetectedHandler() {

        private int[] currCharacters = new int[10];
        private boolean newVal = false;
        private boolean finished = false;

        @Override
        public void handleDetectedFrequencies(final double[] frequencies, final double[] powers, final double[] allFrequencies, final double allPowers[]) {
            if (frequencies.length == 2) {
                int rowIndex = -1;
                int colIndex = -1;
                for (int i = 0; i < 4; i++) {
                    if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
                        rowIndex = i;
                }
                for (int i = 4; i < DTMF.DTMF_FREQUENCIES.length; i++) {
                    if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i])
                        colIndex = i - 4;
                }
                if (rowIndex >= 0 && colIndex >= 0) {

                    char curChar = DTMF.DTMF_CHARACTERS[rowIndex][colIndex];
                    if (curChar >= '0' && curChar <= '9') {
                        currCharacters[curChar - '0']++;
                        newVal = true;
                    } else if (curChar == 'A' && newVal) {
                        newVal = false;
                        char res = Helper.findHighestValPos(currCharacters);
                        if (res != '\uffff') {
                            String val = String.valueOf((char) (res + '0'));
                            //updateGUI(val);
                            Log.d(LOGTAG, "newdtmffound: " + val);
                            dtmfCharacters += String.valueOf((char) (res + '0'));
                            currCharacters = new int[currCharacters.length];
                        }
                    } else if (curChar == 'B' && !finished) {
                        finished = true;
                        UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                        activity.transformAndTalk(Integer.parseInt(humVerifData));
                        //test(dtmfCharacters);
                    }
                }
            }
        }
    });
    private TextView tv;

    private void updateGUI(String str) {
        tv.setText(tv.getText() + str + "sekdhfbbvhjsdrgfn bjhfjn");
    }

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

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authphrase_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authphrase_info);
        tv_info.setText(info);

        tv = (TextView) view.findViewById(R.id.tv_authphrase_text);
        String text = "";
        tv.setText(text);
        mechType = bundle.getString(AUTH_MECHTYPE);
        tgtDevice = bundle.getString(AUTH_TARGET_DVC);
        dtmfCharacters = "";


        dispatcher.addAudioProcessor(goertzelAudioProcessor);
        if (goertzhread != null && goertzhread.isAlive()) {
            Log.d(LOGTAG, "itsALIVE");
        } else {
            Log.d(LOGTAG, "itsDEAD");
            goertzhread = new Thread(dispatcher);
            goertzhread.start();
            Log.d(LOGTAG, "goertzel now started");
        }

        humVerifData = bundle.getString(AUTH_DATA);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dispatcher.stop();
                dispatcher.removeAudioProcessor(goertzelAudioProcessor);
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(tgtDevice, mechType, dtmfCharacters);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dispatcher.stop();
                dispatcher.removeAudioProcessor(goertzelAudioProcessor);
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(tgtDevice, mechType, false);
                HapaRecAuthDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }
}