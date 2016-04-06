package de.tu_darmstadt.seemoo.usdpprototype.view;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.NEWSimpleMadlib;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBDataVCI_I;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthInfoDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BarSibDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BlSiBDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.ButtonDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.CameraDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicIDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicPDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.AsymmetricIdenticon;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.Identicon;

//import android.app.FragmentManager;

public class UsdpMainActivity extends AppCompatActivity {

    private static final String LOGTAG = "UsdpMainActivity";
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    MediaRecorder mRecorder;
    String mFileName;
    private NfcAdapter mNfcAdapter;
    //UI
    private ListView lv_discoveredDevices;
    private ArrayAdapter la_discoveredDevices;
    private ArrayList<String> devList = new ArrayList<>();
    private EditText et_authtext;
    private AuthDialogFragment authDialog;
    private Spinner sp_authmechs;
    private ArrayAdapter spa_authmechs;
    private ArrayList<String> mechList = new ArrayList<>();
    // Service connection
    private Messenger mService = null;
    private Intent bindServiceIntent;
    private boolean mBound;


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

                // Give it some value as an example.
                msg = Message.obtain(null,
                        UsdpService.MSG_SET_VALUE, this.hashCode(), 0);
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

    // TODO move to own class? with other overall usable methods
    public static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usdp_main);
        initViewComponents();
    }

    private void showAuthCamDialogFragment(String phrase) {
        authDialog = new CameraDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CameraDialogFragment.AUTH_BLSIB, phrase);
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
        bundle.putBooleanArray(BarSibDialogFragment.AUTH_BLSIB, pattern);
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

    private void showBtnDialogFragment() {
        authDialog = new ButtonDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA_VB");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press button when triggered by vibration");
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        Log.d(LOGTAG, mFileName);
        try {
            mRecorder.prepare();
        } catch (IOException e) {

            Log.e(LOGTAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void record() {
        mRecorder = new MediaRecorder();
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/record.3gp";

        startRecording();
    }

    @SuppressWarnings("deprecation")
    private void initViewComponents() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_SENDCHATMSG, et_authtext.getText().toString()));

/*                try {
                    Toast.makeText(UsdpMainActivity.this, toMD5("hallo".getBytes("UTF-8")), Toast.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }*/

                if (mRecorder != null) {
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                    MediaPlayer mPlayer = new MediaPlayer();
                    try {
                        mPlayer.setDataSource(mFileName);
                        mPlayer.prepare();
                        mPlayer.start();
                    } catch (IOException e) {
                        Log.e(LOGTAG, "prepare() failed");
                    }
                }
            }
        });

        sp_authmechs = (Spinner) findViewById(R.id.sp_authmechs);
        mechList = new ArrayList<String>();
        mechList.add("test1");
        mechList.add("test2");

        spa_authmechs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mechList);
        spa_authmechs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_authmechs.setAdapter(spa_authmechs);


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


        // final Identicon identicon = (Identicon) findViewById(R.id.identicon);
        //identicon.show("jet fuel");


        ImageButton btn_auth = (ImageButton) findViewById(R.id.btn_auth);
        btn_auth.setImageBitmap(generateQR("jet fuel"));

        btn_auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showAuthBarcodeDialogFragment(generateQR("jetfuelmeltstealbeams!"));

                // currently not supported(anim error as iv_blsib is not accessible
                boolean[] pattern = {false, false, false, true, false, true, true, false, true, true, true};
                //  showAuthBlSibDialogFragment(pattern);

                //String phrase = "one, two, three, four, five, six, seven, eight, nine, ten";
                //showAuthVicPDialogFragment(phrase);

                final Identicon identicon = new AsymmetricIdenticon(getApplicationContext());
                String testtext = et_authtext.getText().toString();

                Bitmap bm = identicon.getBitmap(testtext);
                showAuthVicIDialogFragment(bm);

               /* showAuthVicPDialogFragment(testtext);

                showAuthCamDialogFragment(testtext);

                showBtnDialogFragment();

                showAuthInfoDialogFragment("shakeshake");
*/

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
                                "application/vnd.de.tu_darmstadt.seemoo.usdpprototype", text.getBytes())
                                /**
                                 * The Android Application Record (AAR) is commented out. When a device
                                 * receives a push with an AAR in it, the application specified in the AAR
                                 * is guaranteed to run. The AAR overrides the tag dispatch system.
                                 * You can add it back in to guarantee that this
                                 * activity starts when receiving a beamed message. For now, this code
                                 * uses the tag dispatch system.
                                 */
                                //,NdefRecord.createApplicationRecord("com.example.android.beam")
                        });
                mNfcAdapter.setNdefPushMessage(msg, UsdpMainActivity.this);

                NEWSimpleMadlib nsml = new NEWSimpleMadlib();
                nsml.parseWordlist(getApplicationContext());

/*
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

                PitchDetectionHandler pdh = new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                        final float pitchInHz = result.getPitch();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(LOGTAG, "pitch: " + pitchInHz);
                                int code = -1;

    /*
    610
    * */

    /*
    * 600/630
    * 650/690
    * 710/740
    * 1200/1230
    * 1300/1350
    * 1470/1490
    * 400/420
    * 1300/1350
    * 740/770
    * 770/790
    * *//*
                                if (pitchInHz > 600 && pitchInHz < 630) {
                                    code = 1;
                                } else if (pitchInHz > 660 && pitchInHz < 680) {
                                    code = 2;
                                } else if (pitchInHz > 730 && pitchInHz < 750) {
                                    code = 3;
                                } else if (pitchInHz > 400 && pitchInHz < 420) {
                                    code = 4;
                                } else if (pitchInHz > 430 && pitchInHz < 450) {
                                    code = 5;
                                } else if (pitchInHz > 750 && pitchInHz < 770) {
                                    code = 6;
                                } else if (pitchInHz > 400 && pitchInHz < 420) {
                                    code = 7;
                                } else if (pitchInHz > 430 && pitchInHz < 450) {
                                    code = 8;
                                } else if (pitchInHz > 750 && pitchInHz < 770) {
                                    code = 9;
                                } else if (pitchInHz > 790 && pitchInHz < 810) {
                                    code = 10;
                                }
                                Log.d(LOGTAG, "code: " + code + "\t" + pitchInHz);
                            }
                        });
                    }
                };
                AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
                dispatcher.addAudioProcessor(p);
                new Thread(dispatcher, "Audio Dispatcher").start();
*/

                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 20);
                //        playSound(toneGen, ToneGenerator.TONE_DTMF_1);
                //      playSound(toneGen, ToneGenerator.TONE_DTMF_2);
                //playSound(toneGen, ToneGenerator.TONE_DTMF_3);
              /*  playSound(toneGen, ToneGenerator.TONE_DTMF_4);
                playSound(toneGen, ToneGenerator.TONE_DTMF_5);
                playSound(toneGen, ToneGenerator.TONE_DTMF_6);
                playSound(toneGen, ToneGenerator.TONE_DTMF_7);
                playSound(toneGen, ToneGenerator.TONE_DTMF_8);
                playSound(toneGen, ToneGenerator.TONE_DTMF_9);
                playSound(toneGen, ToneGenerator.TONE_DTMF_A);
*/

                //record();


                Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(100);
            }
        });


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

        devList.add("Discovered devices");

        la_discoveredDevices = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, devList);
        lv_discoveredDevices = (ListView) findViewById(R.id.lv_discoveredDev);
        lv_discoveredDevices.setAdapter(la_discoveredDevices);
        lv_discoveredDevices.setBackgroundColor(Color.BLUE);
        lv_discoveredDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO build own adapter
                /*
                String deviceText = ((String) lv_discoveredDevices.getAdapter().getItem(position));
                int dvcTextLength = deviceText.length();
                deviceText = deviceText.substring(dvcTextLength - 11, dvcTextLength - 1);
                Log.d(LOGTAG, "test: " + deviceText);
                WifiP2pDevice device = discoveredDevicesComplete.get(deviceText);
                */
                // end TODO
                String deviceMac = lv_discoveredDevices.getAdapter().getItem(position).toString();
                Log.d(LOGTAG, "DEVICEMAC: " + deviceMac);
                Message msg = Message.obtain(null, UsdpService.MSG_CONNECT, deviceMac);
                sendMsgtoService(msg);
            }
        });


        // TODO request other device to remove group
        lv_discoveredDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceMac = lv_discoveredDevices.getAdapter().getItem(position).toString();
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_PAIR, deviceMac));

                return true;
            }
        });
    }

    private void playSound(ToneGenerator toneGen, int toneType) {
        if (toneGen.startTone(toneType)) {
            try {
                Thread.sleep(1000);
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
        Log.d(LOGTAG, "intent im zelt");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //startActivity(intent);
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present

        Toast.makeText(UsdpMainActivity.this, "NFC message transmitted! " + new String(msg.getRecords()[0].getPayload()), Toast.LENGTH_SHORT).show();
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
            Log.d(LOGTAG, "intent im zelt NFC");
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

    private Bitmap generateQR(String input) {
        int width = 200;
        int height = 200;
        BitMatrix qrMatrix;
        QRCodeWriter writer = new QRCodeWriter();
        Bitmap mBitmap = null;
        try {
            qrMatrix = writer.encode(input, BarcodeFormat.QR_CODE, width, height);
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (qrMatrix.get(i, j)) {
                        mBitmap.setPixel(i, j, Color.BLACK);
                    } else {
                        mBitmap.setPixel(i, j, Color.WHITE);
                    }


                    //mBitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            Log.d(LOGTAG, "qrCreated");

        } catch (WriterException e) {
            e.printStackTrace();
        }
        return mBitmap;
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

    private void manageAuthenticationDialog(OOBData oobData) {
        switch (oobData.getType()) {
            case OOBData.VCI_I:
                OOBDataVCI_I oobVCI_i = (OOBDataVCI_I) oobData;
                final Identicon identicon = new AsymmetricIdenticon(getApplicationContext());
                Bitmap bm = identicon.getBitmap(oobVCI_i.getAuthText());
                showAuthVicIDialogFragment(bm);
                break;
            case OOBData.VCI_N:
                break;
            case "VCI_P":
                break;
            case "SiB":
                break;
        }
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
                    devList.clear();
                    devList.addAll((List<String>) msg.obj);
                    //addDeviceNames();
                    la_discoveredDevices.notifyDataSetChanged();
                    //TODO move sendmsg somewhere more appropriate
                    sendMsgtoService(Message.obtain(null, UsdpService.MSG_AUTHMECHS));
                    break;
                case UsdpService.MSG_AUTHMECHS:
                    mechList.clear();
                    mechList.addAll((List<String>) msg.obj);
                    spa_authmechs.notifyDataSetChanged();
                    break;
                case UsdpService.MSG_AUTHENTICATION_DIALOG_DATA:
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
