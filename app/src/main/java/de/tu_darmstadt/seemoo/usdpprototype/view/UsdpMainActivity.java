package de.tu_darmstadt.seemoo.usdpprototype.view;


import android.app.AlertDialog;
import android.app.Dialog;
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
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.DTMF;
import be.tarsos.dsp.pitch.Goertzel;
import de.tu_darmstadt.seemoo.usdpprototype.ConnInfo;
import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthResult;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecAuthVIC;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.ListDevice;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.SimpleMadlib;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.InfoAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BEDA_BtnAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BEDA_VibAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.BarSibAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.LEDBlinkAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.CamAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.ImgAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.StringAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.AsymmetricIdenticon;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.Identicon;

public class UsdpMainActivity extends AppCompatActivity implements AuthDialogFragment.oobResultInterface {

    private static final String LOGTAG = "UsdpMainActivity";
    private final static int CTXM_SENDMSG = 0;
    private final static int CTXM_GETRPRT = 1;
    private final static int CTXM_DISCONN = 2;
    private static final int CTXM_CNCLINV = 3;
    private static final int AUTH_OOB_RESULT = 1;
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
        authDialog = new CamAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SibBlink");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "use camera on other device to capture blinking sequence");
        bundle.putString(CamAuthDialogFragment.AUTH_PATTERN, phrase);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsibcam");
        }
    }

    private void showAuthVicNDialogFragment(String number) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(StringAuthDialogFragment.AUTH_VICP, number);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_N);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VIC-N: compare numbers");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with number on other device");
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvicn");
        }
    }

    private void showAuthVicPDialogFragment(String phrase) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(StringAuthDialogFragment.AUTH_VICP, phrase);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_P);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VIC-P: compare phrases");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with phrase on other device");
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvicp");
        }
    }

    private void showAuthVerifDialogFragment(String title, String info, String mechtype) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, mechtype);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), title);
        }
    }

    private void showAuthLacdsDialogFragment(String title, String info, String phrase) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(StringAuthDialogFragment.AUTH_VICP, phrase);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.LaCDS);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authLaC_DS");
        }
    }

    private void showAuthInfoDialogFragment(String info) {
        authDialog = new InfoAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(InfoAuthDialogFragment.AUTH_INFOONLY, info);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "tzefuginfo");
        }
    }


    private void showAuthVicIDialogFragment(Bitmap bmp) {

        authDialog = new ImgAuthDialogFragment();
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VIC-I: compare images");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with image on other device");
        bundle.putIntArray(ImgAuthDialogFragment.IMG_IMAGE, pixels);
        bundle.putInt(ImgAuthDialogFragment.IMG_HEIGHT, bmp.getHeight());
        bundle.putInt(ImgAuthDialogFragment.IMG_WIDTH, bmp.getWidth());
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_I);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvici");
        }
    }

    private void showAuthBlSibDialogFragment(boolean[] pattern) {
        authDialog = new LEDBlinkAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBblink: blinking sequence");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "capture blinking sequence");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.SiBBlink);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBedaVibDialogFragment(boolean[] pattern) {
        authDialog = new BEDA_VibAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA Vibrate-Button");
        bundle.putString(AuthDialogFragment.AUTH_EXPLINFO, "press button on other device when vibrating");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press OK when finished");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBEDA_LB_DialogFragment(boolean[] pattern) {
        authDialog = new LEDBlinkAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA LED-Button");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press button on other device as long as LED is bright");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.BEDA_LB);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBarcodeDialogFragment(Bitmap bmp) {
        authDialog = new BarSibAuthDialogFragment();
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBcode: Scan Barcode");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with image on other device");
        bundle.putIntArray(BarSibAuthDialogFragment.BARCODE_CODE, pixels);
        bundle.putInt(BarSibAuthDialogFragment.BARCODE_HEIGHT, bmp.getHeight());
        bundle.putInt(BarSibAuthDialogFragment.BARCODE_WIDTH, bmp.getWidth());
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.SiB);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authbarsib");
        }
    }

    private void showBEDARecBtnDialogFragment(String title, String info, String bedaType) {
        authDialog = new BEDA_BtnAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, bedaType);
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
                    case R.id.mnu_action_settings:
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
                    case R.id.mnu_get_report:
                        sendMsgtoService(Message.obtain(null, UsdpService.MSG_REQ_CONNINFO));
                        break;
                    case R.id.mnu_test:

                        break;
                    case R.id.mnu_wifi_settings:
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        break;
                }
                return false;
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(UsdpMainActivity.this);
                LayoutInflater inflater = UsdpMainActivity.this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.dialog_message, null);
                dialogBuilder.setView(dialogView);

                final EditText et_msg = (EditText) dialogView.findViewById(R.id.et_msg);

                dialogBuilder.setTitle("Send secure message");
                dialogBuilder.setMessage("Enter text below");
                dialogBuilder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String res = et_msg.getText().toString();
                        sendMsgtoService(Message.obtain(null, UsdpService.MSG_SEND_ENCRYPTED, res));
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //pass
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();





                /*String res = "";
                for (Character x : dtmfCharacters) {
                    res += x + "/";
                }

                Log.d("GODRESULT", res);
                dtmfCharacters.clear();*/
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


        tv_status = (TextView) findViewById(R.id.status);

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

        devList.add(new ListDevice("press \"Discover devices\"", "No other devices found", WifiP2pDevice.UNAVAILABLE, false));

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


        registerForContextMenu(lv_discoveredDevices);

/*        // TODO request other device to remove group
        lv_discoveredDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceMac = ((ListDevice) (lv_discoveredDevices.getAdapter().getItem(position))).getAddress();
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_UNPAIR, deviceMac));

                return true;
            }
        });*/
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.lv_discoveredDev) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ListDevice selDevice = (ListDevice) lv.getItemAtPosition(acmi.position);
            menu.setHeaderTitle("Device actions (" + selDevice.getName() + ")");
            menu.add(Menu.NONE, CTXM_SENDMSG, CTXM_SENDMSG, R.string.send_message);
            menu.add(Menu.NONE, CTXM_GETRPRT, CTXM_GETRPRT, R.string.get_report);
            if (selDevice.getState() == WifiP2pDevice.CONNECTED) {
                menu.add(Menu.NONE, CTXM_DISCONN, CTXM_DISCONN, R.string.disconnect);
            } else if (selDevice.getState() == WifiP2pDevice.INVITED) {
                menu.add(Menu.NONE, CTXM_CNCLINV, CTXM_CNCLINV, R.string.cancel_invite);
            }
        }


/*
        menu.add(0, v.getId(), 0, "send Message");
        menu.add(0, v.getId(), 0, "get report");
        menu.add(0, v.getId(), 0, "Disconnect");
*/
    }

    private void showShortToast(String text) {
        Toast.makeText(UsdpMainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String text) {
        Toast.makeText(UsdpMainActivity.this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        String selectedDeviceMac = ((TextView) info.targetView.findViewById(android.R.id.text2)).getText().toString();
        switch (item.getItemId()) {
            case CTXM_SENDMSG:
                showShortToast("sendMsg " + selectedDeviceMac);
                return true;
            case CTXM_GETRPRT:
                showShortToast("getrep");
                return true;
            case CTXM_DISCONN:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_DISCONNECT, selectedDeviceMac));
                showShortToast("disconn");
                return true;
            case CTXM_CNCLINV:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_ABRT_CONN, selectedDeviceMac));
                showShortToast("cncl invit");
                return true;

            default:
                return super.onContextItemSelected(item);
        }
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

    private void setStateInfo(WifiP2pDevice dev) {
        String devName = dev.deviceName;
        if (dev.isGroupOwner()) {
            devName += " (GO)";
        }
        String devAddress = dev.deviceAddress;
        tv_status.setText("Device info:\n" + devName
                + " (" + devAddress + ")");

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
        String data = new String(msg.getRecords()[0].getPayload());
        oobResult(OOBData.NFC, data);
        Toast.makeText(UsdpMainActivity.this, "NFC message received! " + data, Toast.LENGTH_SHORT).show();
        Log.d(LOGTAG, new String(msg.getRecords()[0].getPayload()));
    }

    /**
     * Send a prepared message to the service, method takes care of error handling
     *
     * @param msg message to send
     */
    private boolean sendMsgtoService(Message msg) {
        if (mService != null) {
            try {
                mService.send(msg);
                return true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(UsdpMainActivity.this, "start Service first", Toast.LENGTH_SHORT).show();
        }
        return false;
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
        if (id == R.id.mnu_action_settings) {
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
                oobResult(OOBData.SiB, contents);
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
                    tts.setSpeechRate(0.8f);
                }
            }
        });
    }

    public void acceptOobAuthentication() {
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_ACCEPT_AUTH));
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
                showAuthVicNDialogFragment(authdataStr);
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
                    boolean[] pattern = Helper.getBinaryArray(authdata, SecAuthVIC.OOB_BITLENGTH);
                    showAuthBlSibDialogFragment(pattern);
                } else {
                    showAuthCamDialogFragment("ajsnd");
                }
                break;
            case OOBData.LaCDS:
                String sentenceDS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    showAuthLacdsDialogFragment("LaC DS", "compare spoken and displayed sentence", sentenceDS);
                } else {
                    showAuthVerifDialogFragment("LaC DS", "compare spoken and displayed sentence", OOBData.LaCDS);
                    talk(sentenceDS);

                }
                break;
            case OOBData.LaCSS:
                String sentenceSS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    //speaker
                    showAuthVerifDialogFragment("LaC SS", "compare both spoken sentences", OOBData.LaCSS);
                    talk(sentenceSS);
                } else {
                    //speaker
                    showAuthVerifDialogFragment("LaC SS", "compare both spoken sentences", OOBData.LaCSS);
                    talk(sentenceSS);
                }
                break;
            case OOBData.BEDA_VB:
                if (oobData.isSendingDevice()) {
                    //vibrate
                    showAuthBedaVibDialogFragment(Helper.getBinaryArray(authdata, SecAuthVIC.OOB_BITLENGTH));
                } else {
                    showBEDARecBtnDialogFragment("BEDA Vibrate Button", "press button when triggered by vibration", OOBData.BEDA_VB);
                }
                break;
            case OOBData.BEDA_LB:
                if (oobData.isSendingDevice()) {
                    showAuthBEDA_LB_DialogFragment(Helper.getBinaryArray(authdata, SecAuthVIC.OOB_BITLENGTH));
                } else {
                    showBEDARecBtnDialogFragment("BEDA LED Button", "press button as long as triggered by LED", OOBData.BEDA_LB);
                }
                break;
            case OOBData.BEDA_BPBT:
                if (oobData.isSendingDevice()) {
                    //speaker
                    boolean[] data = Helper.getBinaryArray(authdata, SecAuthVIC.OOB_BITLENGTH);
                    playSequence(data);
                } else {
                    showBEDARecBtnDialogFragment("BEDA Beep Button", "press button as long as triggered by beeping", OOBData.BEDA_BPBT);
                }
                break;
            case OOBData.BEDA_BTBT:
                // TODO not using SAS protocol
                if (oobData.isSendingDevice()) {
                    //button
                    showBEDARecBtnDialogFragment("BEDA Button Button", "press button several random times but equal on both devices", OOBData.BEDA_BTBT);
                } else {
                    showBEDARecBtnDialogFragment("BEDA Button Button", "press button several random times but equal on both devices", OOBData.BEDA_BTBT);
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
                    showAuthVerifDialogFragment("NFC", "hold devices together", OOBData.NFC);
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

    @Override
    public void oobResult(String mech, boolean result) {

        AuthResult res = new AuthResult(mech, result);
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_ACCEPT_AUTH, res));

    }

    @Override
    public void oobResult(String mech, String data) {
        AuthResult res = new AuthResult(mech, data);
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_ACCEPT_AUTH_WDATA, res));
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
                case UsdpService.MSG_PEERSDISCOVERED:
                    List<ListDevice> devices = (List<ListDevice>) msg.obj;
                    devList.clear();
                    if (devices.isEmpty()) {
                        devList.add(new ListDevice("press \"Discover Devices\"", "No other devices found", WifiP2pDevice.UNAVAILABLE, false));
                    } else {
                        devList.addAll(devices);
                    }
                    la_discoveredDevices.notifyDataSetChanged();
                    break;

                case UsdpService.MSG_WIFIDEVICE_CHANGED:
                    WifiP2pDevice dev = (WifiP2pDevice) msg.obj;

                    setStateInfo(dev);
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
                case UsdpService.MSG_CONNINFO:
                    ConnInfo info = (ConnInfo) msg.obj;
                    //ReportDialogFragment rdf = new ReportDialogFragment();
                    //rdf.show(getSupportFragmentManager(), info.toString());
                    if (info == null) {
                        Toast.makeText(UsdpMainActivity.this, "no secure connection established", Toast.LENGTH_SHORT).show();
                    } else {
                        AlertDialog alertDialog = new AlertDialog.Builder(UsdpMainActivity.this).create();
                        alertDialog.setTitle("Connection info");
                        alertDialog.setMessage(info.toString());
                        
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
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
