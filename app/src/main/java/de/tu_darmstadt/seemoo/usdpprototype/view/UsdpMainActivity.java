package de.tu_darmstadt.seemoo.usdpprototype.view;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.p2p.WifiP2pDevice;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.DTMF;
import be.tarsos.dsp.pitch.Goertzel;
import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.ListDevice;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.SimpleMadlib;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthInfoDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BEDAButtonDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BEDA_VibDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BarSibDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BlSiBDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.CameraDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicIDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicPDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.AsymmetricIdenticon;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.Identicon;

public class UsdpMainActivity extends AppCompatActivity {

    private static final String LOGTAG = "UsdpMainActivity";
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    private final ArrayList<Character> dtmfCharacters = new ArrayList<>();
    private final AudioProcessor goertzelAudioProcessor = new Goertzel(44100, 256, DTMF.DTMF_FREQUENCIES, new Goertzel.FrequenciesDetectedHandler() {
        private int[] currCharacters = new int[10];
        private boolean newVal = false;

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
                            dtmfCharacters.add((char) (res + '0'));
                            currCharacters = new int[currCharacters.length];
                        }
                    }
                }
            }
        }
    });

    private NfcAdapter mNfcAdapter;
    private DeviceCapabilities devCap = new DeviceCapabilities();
    //UI
    private ListView lv_discoveredDevices;
    private DeviceArrayAdapter la_discoveredDevices;
    private ArrayList<ListDevice> devList = new ArrayList<>();
    private EditText et_authtext;
    private AuthDialogFragment authDialog;

    private TextView tv_status;

    // Service connection
    private Messenger mService = null;
    private Intent bindServiceIntent;
    private boolean mBound;
    private SimpleMadlib smplMadlib;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.

            mService = new Messenger(service);
            mBound = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        UsdpService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                msg = Message.obtain(null,
                        UsdpService.MSG_CONFIG, devCap);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Toast.makeText(UsdpMainActivity.this, "remote service connected",
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;

            // As part of the sample, tell the user what happened.
            Toast.makeText(UsdpMainActivity.this, "remote service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };
    private TextToSpeech tts;

    // TODO move to own class? with other overall usable methods


    private void init() {
        smplMadlib = new SimpleMadlib();
        smplMadlib.parseWordlist(getApplicationContext());

        initTts();

        initViewComponents();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usdp_main);

        init();
    }

    private void showAuthCamDialogFragment(String phrase) {
        authDialog = new CameraDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CameraDialogFragment.AUTH_BLSIBARRAY, phrase);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsibcam");
        }
    }

    private void showAuthVicPDialogFragment(String phrase) {
        authDialog = new VicPDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(VicPDialogFragment.AUTH_VICP, phrase);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvicp");
        }
    }

    private void showAuthLacdsDialogFragment(String phrase) {
        authDialog = new VicPDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(VicPDialogFragment.AUTH_VICP, phrase);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authLaC_DS");
        }
    }

    private void showAuthInfoDialogFragment(String info) {
        authDialog = new AuthInfoDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthInfoDialogFragment.AUTH_INFOONLY, info);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "tzefuginfo");
        }
    }

    private void showAuthVicIDialogFragment(Bitmap bmp) {

        authDialog = new VicIDialogFragment();
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VIC-I: compare images");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with image on other device");
        bundle.putIntArray(VicIDialogFragment.IMG_IMAGE, pixels);
        bundle.putInt(VicIDialogFragment.IMG_HEIGHT, bmp.getHeight());
        bundle.putInt(VicIDialogFragment.IMG_WIDTH, bmp.getWidth());
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvici");
        }
    }

    private void showAuthBlSibDialogFragment(boolean[] pattern) {
        authDialog = new BlSiBDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBblink: blinking sequence");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "capture blinking sequence");
        bundle.putBooleanArray(BarSibDialogFragment.AUTH_BLSIBARRAY, pattern);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBedaVibDialogFragment(boolean[] pattern) {
        authDialog = new BEDA_VibDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA vibrate-button");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press button on other device when vibrating");
        bundle.putBooleanArray(BarSibDialogFragment.AUTH_BLSIBARRAY, pattern);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBEDA_LB_DialogFragment(boolean[] pattern) {
        authDialog = new BlSiBDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBblink: blinking sequence");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "capture blinking sequence");
        bundle.putBooleanArray(BarSibDialogFragment.AUTH_BLSIBARRAY, pattern);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBarcodeDialogFragment(Bitmap bmp) {
        authDialog = new BarSibDialogFragment();
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBcode: Scan Barcode");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with image on other device");
        bundle.putIntArray(BarSibDialogFragment.BARCODE_CODE, pixels);
        bundle.putInt(BarSibDialogFragment.BARCODE_HEIGHT, bmp.getHeight());
        bundle.putInt(BarSibDialogFragment.BARCODE_WIDTH, bmp.getWidth());
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authbarsib");
        }
    }

    private void showBEDABtnDialogFragment() {
        authDialog = new BEDAButtonDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA_VB");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press button when triggered by vibration");
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "beda_vb");
        }
    }

    @SuppressWarnings("deprecation")
    private void initViewComponents() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_settings:
                        AlertDialog dialog;

                        final CharSequence[] items = DeviceCapabilities.capTitles;

                        final boolean[] backup = devCap.getCapabilities().clone();

                        AlertDialog.Builder builder = new AlertDialog.Builder(UsdpMainActivity.this);
                        builder.setTitle("Choose functionality");
                        builder.setMultiChoiceItems(items, devCap.getCapabilities(),
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    // indexSelected contains the index of item (of which checkbox checked)
                                    @Override
                                    public void onClick(DialogInterface dialog, int indexSelected,
                                                        boolean isChecked) {
                                        devCap.setCapability(indexSelected, isChecked);
                                    }
                                })
                                // Set the action buttons
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        devCap.setCapabilities(backup);
                                    }
                                });

                        dialog = builder.create();
                        dialog.show();

                        return true;
                }

                return false;
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sendMsgtoService(Message.obtain(null, UsdpService.MSG_SENDCHATMSG, et_authtext.getText().toString()));

                String res = "";
                for (Character x : dtmfCharacters) {
                    res += x + "/";
                }

                Log.d("GODRESULT", res);
                dtmfCharacters.clear();
            }
        });

        // UI
        Button btn_discover = (Button) findViewById(R.id.btn_discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    Message msg = Message.obtain(null, UsdpService.MSG_DISCOVERPEERS, 0, 0);
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(UsdpMainActivity.this, "start Service first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btn_pair = (Button) findViewById(R.id.btn_pair);
        btn_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_PAIR));
            }
        });

        tv_status = (TextView) findViewById(R.id.status);
        setStateInfo();

        et_authtext = (EditText) findViewById(R.id.et_text);

        ToggleButton btn_toggleSvc = (ToggleButton) findViewById(R.id.btn_toggleSvc);
        btn_toggleSvc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    bindService(bindServiceIntent, mConnection,
                            Context.BIND_AUTO_CREATE);

                } else {
                    if (mBound) {
                        unbindService(mConnection);
                        mBound = false;
                    }
                }
            }
        });

        devList.add(new ListDevice("press \"Discover devices\"", "No other devices found", WifiP2pDevice.UNAVAILABLE));

        la_discoveredDevices = new DeviceArrayAdapter(getApplicationContext(), devList);
        lv_discoveredDevices = (ListView) findViewById(R.id.lv_discoveredDev);

        lv_discoveredDevices.setAdapter(la_discoveredDevices);
        lv_discoveredDevices.setBackgroundColor(Color.BLUE);
        lv_discoveredDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceMac = ((ListDevice) (lv_discoveredDevices.getAdapter().getItem(position))).getAddress();
                Log.d(LOGTAG, "DEVICEMAC: " + deviceMac);
                Message msg = Message.obtain(null, UsdpService.MSG_CONNECT, deviceMac);
                sendMsgtoService(msg);
            }
        });


        // TODO request other device to remove group
        lv_discoveredDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceMac = ((ListDevice) (lv_discoveredDevices.getAdapter().getItem(position))).getAddress();
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_UNPAIR, deviceMac));

                return true;
            }
        });
    }

    private void playSequence(final boolean[] data) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_DTMF, 20);
                for (int pos = 0; pos < data.length; pos++) {
                    playSound(toneGen, ToneGenerator.TONE_DTMF_1, 500);
                    if (data[pos]) {
                        playSound(toneGen, ToneGenerator.TONE_DTMF_1, 1000);
                    }
                    playSound(toneGen, ToneGenerator.TONE_CDMA_SIGNAL_OFF, 1000);
                }
                playSound(toneGen, ToneGenerator.TONE_CDMA_SIGNAL_OFF, 2000);
            }
        };
        thread.start();
    }

    private void setStateInfo() {
        tv_status.setText("Connected Devices:\t" + 1337 + "\ncheck");
    }

    private void playSequence(final ArrayList<Integer> digits) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_DTMF, 50);
                // delay for other device to get started
                playSound(toneGen, ToneGenerator.TONE_CDMA_SIGNAL_OFF, 1000);
                for (Integer val : digits) {
                    playSound(toneGen, val, 300);
                    // use as seperator
                    playSound(toneGen, ToneGenerator.TONE_DTMF_A, 100);
                }
            }
        };
        thread.start();
    }

    private void playSound(final ToneGenerator toneGen, final int toneType, final int msduration) {
        if (toneGen.startTone(toneType)) {
            try {
                Thread.sleep(msduration);
                toneGen.stopTone();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Toast.makeText(UsdpMainActivity.this, "new intent", Toast.LENGTH_SHORT).show();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //startActivity(intent);
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent
     */
    void processIntent(Intent intent) {

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present

        Toast.makeText(UsdpMainActivity.this, "NFC message received! " + new String(msg.getRecords()[0].getPayload()), Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, new String(msg.getRecords()[0].getPayload()));
    }

    /**
     * Send a prepared message to the service, method takes care of error handling
     *
     * @param msg message to send
     */
    private void sendMsgtoService(Message msg) {
        if (mService != null) {
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(UsdpMainActivity.this, "start Service first", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_usdp_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGTAG, "intent onresum");
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindServiceIntent = new Intent(this, UsdpService.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                Toast.makeText(UsdpMainActivity.this, contents, Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
                Toast.makeText(UsdpMainActivity.this, "intent was cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void talk(String text) {
        //TODO support old API (then remove  @TargetApi(Build.VERSION_CODES.LOLLIPOP))
        tts.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    private void initTts() {
        //TODO move ttstest to own class
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    private void manageAuthenticationDialog(OOBData oobData) {
        int authdata = oobData.getAuthdata();
        String authdataStr = String.valueOf(authdata);
        switch (oobData.getType()) {
            case OOBData.VIC_I:
                final Identicon identicon = new AsymmetricIdenticon(getApplicationContext());
                Bitmap bm = identicon.getBitmap(authdataStr);
                showAuthVicIDialogFragment(bm);
                break;
            case OOBData.VIC_N:
                showAuthVicPDialogFragment(authdataStr);
                break;
            case OOBData.VIC_P:
                showAuthVicPDialogFragment(smplMadlib.getSentence(authdata));
                break;
            case OOBData.SiB:
                if (oobData.isSendingDevice()) {
                    showAuthBarcodeDialogFragment(Helper.generateQR(authdataStr));
                } else {
                    Toast.makeText(this, "checking if zxing is installed", Toast.LENGTH_SHORT).show();
                    if (Helper.isPackageInstalled("com.google.zxing.client.android", this)) {
                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                        intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "QR_CODE_MODE");
                        startActivityForResult(intent, 0);
                    } else {
                        // TODO goto market, install. check this at the start (kind of init app generally)
                        Toast.makeText(this, "install zxing Barcode Scanner", Toast.LENGTH_SHORT).show();
                        Log.d(LOGTAG, "install zxing Barcode Scanner");
                    }
                }
                break;
            case OOBData.SiBBlink:
                if (oobData.isSendingDevice()) {
                    boolean[] pattern = Helper.getBinaryArray(authdata, 20);
                    showAuthBlSibDialogFragment(pattern);
                } else {
                    showAuthCamDialogFragment("ajsnd");
                }
                break;
            case OOBData.LaCDS:
                String sentenceDS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    showAuthLacdsDialogFragment(sentenceDS);
                } else {
                    talk(sentenceDS);
                }
                break;
            case OOBData.LaCSS:
                String sentenceSS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    //speaker
                    talk(sentenceSS);
                } else {
                    //speaker
                    talk(sentenceSS);
                }
                break;
            case OOBData.BEDA_VB:
                if (oobData.isSendingDevice()) {
                    //vibrate
                    showAuthBedaVibDialogFragment(Helper.getBinaryArray(authdata, 20));
                } else {
                    showBEDABtnDialogFragment();
                }
                break;
            case OOBData.BEDA_LB:
                if (oobData.isSendingDevice()) {
                    showAuthBEDA_LB_DialogFragment(Helper.getBinaryArray(authdata, 20));
                } else {
                    showBEDABtnDialogFragment();
                }
                break;
            case OOBData.BEDA_BPBT:
                if (oobData.isSendingDevice()) {
                    //speaker
                    boolean[] data = Helper.getBinaryArray(authdata, 20);
                    playSequence(data);
                } else {
                    showBEDABtnDialogFragment();
                }
                break;
            case OOBData.BEDA_BTBT:
                if (oobData.isSendingDevice()) {
                    //button
                    showBEDABtnDialogFragment();
                } else {
                    showBEDABtnDialogFragment();
                }
                break;
            case OOBData.HAPADEP:
                if (oobData.isSendingDevice()) {
                    //speaker
                    playSequence(Helper.getDigitlistFromInt(authdata));
                } else {
                    //mic + speaker
                    AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100, 2048, 1024);
                    dispatcher.addAudioProcessor(goertzelAudioProcessor);

                    new Thread(dispatcher).start();
                    Log.d(LOGTAG, "goertzel now started");
                }
                break;
            case OOBData.NFC:
                if (oobData.isSendingDevice()) {
                    //nfc send
                    //fNFC
                    mNfcAdapter = NfcAdapter.getDefaultAdapter(UsdpMainActivity.this);
                    if (mNfcAdapter == null) {
                        Toast.makeText(UsdpMainActivity.this, "NFC is not available", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    // Register callback
                    String text = "Message from \"" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\"";
                    NdefMessage msg = new NdefMessage(
                            new NdefRecord[]{NdefRecord.createMime(
                                    "application/vnd.de.tu_darmstadt.seemoo.usdpprototype", authdataStr.getBytes())
                            });
                    mNfcAdapter.setNdefPushMessage(msg, UsdpMainActivity.this);
                } else {
                    //nfc receive
                }
                break;
            case OOBData.SWBU:
                if (oobData.isSendingDevice()) {
                    //accel
                    showAuthInfoDialogFragment("shakeshake");
                } else {
                    //accel
                    showAuthInfoDialogFragment("shakeshake");
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    // TODO move to its own file?
    public interface MessageTarget {
        public Handler getHandler();
    }

    /**
     * inner class, handles messages from @UsdpService
     */
    @SuppressWarnings("unchecked")
    class InternalMsgIncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsdpService.MSG_SAY_HELLO:
                    Toast.makeText(UsdpMainActivity.this, "service said hello!", Toast.LENGTH_SHORT).show();
                    break;
                case UsdpService.MSG_PEERSDISCOVERED:
                    List<ListDevice> devices = (List<ListDevice>) msg.obj;
                    devList.clear();
                    if (devices.isEmpty()) {
                        devList.add(new ListDevice("press \"Discover Devices\"", "No other devices found", WifiP2pDevice.UNAVAILABLE));
                    } else {
                        devList.addAll(devices);
                    }
                    la_discoveredDevices.notifyDataSetChanged();
                    break;
                case UsdpService.MSG_AUTHMECHS:
                    AlertDialog.Builder authMechDialog = new AlertDialog.Builder(UsdpMainActivity.this);
                    authMechDialog.setTitle("Choose authentication mechanism");
                    final String[] types = (String[]) msg.obj;
                    authMechDialog.setItems(types, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                            Log.d(LOGTAG, types[which] + " was chosen");
                            Toast.makeText(UsdpMainActivity.this, types[which] + " was chosen", Toast.LENGTH_SHORT).show();

                            Message msg = Message.obtain(null,
                                    UsdpService.MSG_CHOSEN_AUTHMECH, types[which]);
                            sendMsgtoService(msg);
                        }

                    });
                    authMechDialog.show();
                    break;
                case UsdpService.MSG_AUTHENTICATION_DIALOG_DATA:
                    Toast.makeText(UsdpMainActivity.this, "auth shows now", Toast.LENGTH_SHORT).show();
                    manageAuthenticationDialog((OOBData) msg.obj);

                    break;
                case UsdpService.MSG_CHATMSGRECEIVED:
                    Toast.makeText(UsdpMainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case UsdpService.AUTH_BARCODE:
                    Bitmap mBitmap = (Bitmap) msg.obj;
                    showAuthBarcodeDialogFragment(mBitmap);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
