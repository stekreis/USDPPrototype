package de.tu_darmstadt.seemoo.usdpprototype.view;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.ToneGenerator;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
//import android.app.FragmentManager;
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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthSif;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthInfoDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BarSibDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BlSiBDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.ButtonDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.CameraDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicIDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.VicPDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.Identicon;

public class UsdpMainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {

    NfcAdapter mNfcAdapter;

    private static final String LOGTAG = "UsdpMainActivity";
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());

    //UI
    private ListView lv_discoveredDevices;
    private ArrayAdapter la_discoveredDevices;
    private ArrayList<String> valueList = new ArrayList<>();
    private EditText et_authtext;
    private AuthDialogFragment authDialog;
    // Service connection
    private Messenger mService = null;
    private Intent bindServiceIntent;
    private boolean mBound;

    private SensorManager sensorManager = null;
    private Sensor lightSensor = null;
    private float lightQuantity;


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

        //fNFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);


        //lightsensor();
    }

    private void lightsensor() {
        // Obtain references to the SensorManager and the Light Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Implement a listener to receive updates
        SensorEventListener listener = new SensorEventListener() {
            boolean light = false;
            long lastTimeMillis = 0;

            @Override
            public void onSensorChanged(SensorEvent event) {
                lightQuantity = event.values[0];
//                Log.d(LOGTAG, "licht: " + lightQuantity);
                if (light && lightQuantity < 10) {
                    Log.d(LOGTAG, "off");
                    Log.d(LOGTAG, "time passed :" + (System.currentTimeMillis() - lastTimeMillis));
                    light = false;
                } else if (!light && lightQuantity > 80) {
                    Log.d(LOGTAG, "on");
                    Log.d(LOGTAG, "time passed :" + (System.currentTimeMillis() - lastTimeMillis));
                    light = true;
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.d(LOGTAG, "accuracy changed");
            }
        };

        // Register the listener with the light sensor -- choosing
        // one of the SensorManager.SENSOR_DELAY_* constants.
        sensorManager.registerListener(
                listener, lightSensor, SensorManager.SENSOR_DELAY_UI);
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

    @SuppressWarnings("deprecation")
    private void initViewComponents() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_SENDCHATMSG, et_authtext.getText().toString()));
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

        final Identicon identicon = (Identicon) findViewById(R.id.identicon);
        //identicon.show("jet fuel");


        ImageButton btn_auth = (ImageButton) findViewById(R.id.btn_auth);
        btn_auth.setImageBitmap(generateQR("jet fuel"));

        btn_auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO uncomment
                showAuthBarcodeDialogFragment(generateQR("jetfuelmeltstealbeams!"));

                // currently not supported(anim error as iv_blsib is not accessible
                boolean[] pattern = {true, false, false, true, true, true, false, true, false, true, false, false, true};
                showAuthBlSibDialogFragment(pattern);

                //String phrase = "one, two, three, four, five, six, seven, eight, nine, ten";
                //showAuthVicPDialogFragment(phrase);


                String testtext = et_authtext.getText().toString();
                identicon.show(testtext);


                Bitmap bm = identicon.getBitmap(testtext);
                showAuthVicIDialogFragment(bm);

                showAuthVicPDialogFragment(testtext);

                showAuthCamDialogFragment(testtext);

                showBtnDialogFragment();

                showAuthInfoDialogFragment("shakeshake");

                // NFC
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
                if (nfcAdapter == null) return;  // NFC not available on this device
                NdefMessage msg = null;
                try {
                    msg = new NdefMessage("beammeupscotty".getBytes());
                    nfcAdapter.setNdefPushMessage(msg, UsdpMainActivity.this);
                } catch (FormatException e) {
                    e.printStackTrace();
                }


                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);

                if (toneGen.startTone(ToneGenerator.TONE_DTMF_1)) {

                    try {
                        Thread.sleep(500);
                        toneGen.stopTone();
                        toneGen.startTone(ToneGenerator.TONE_DTMF_1);
                        toneGen.stopTone();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


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

        valueList.add("Discovered devices");

        la_discoveredDevices = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, valueList);
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

    /*
    NFC test



    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }
    */
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Toast.makeText(UsdpMainActivity.this, "new intent", Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, "intent im zelt");
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

        Toast.makeText(UsdpMainActivity.this, "shaddap", Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, new String(msg.getRecords()[0].getPayload()));
    }


    /**
     * Send a prepared message to the service, method takes care of error handling
     *
     * @param msg message to send
     */
    private void sendMsgtoService(Message msg) {
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
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

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = ("Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis());
        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{NdefRecord.createMime(
                        "application/vnd.com.example.android.beam", text.getBytes())
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
        return msg;
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
                    valueList.clear();

                    valueList.addAll((List<String>) msg.obj);
                    //addDeviceNames();
                    la_discoveredDevices.notifyDataSetChanged();
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
