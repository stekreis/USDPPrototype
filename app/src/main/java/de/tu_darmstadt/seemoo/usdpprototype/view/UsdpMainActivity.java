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

import de.tu_darmstadt.seemoo.usdpprototype.ConnInfo;
import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthResult;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecureAuthentication;
import de.tu_darmstadt.seemoo.usdpprototype.misc.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.misc.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.misc.TargetMsg;
import de.tu_darmstadt.seemoo.usdpprototype.misc.ListDevice;
import de.tu_darmstadt.seemoo.usdpprototype.misc.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.misc.SimpleMadlib;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.AuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.HapaRecAuthDialogFragment;
import de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog.SwbuAuthDialogFragment;
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
    private static final int CTXM_PAIR = 4;
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());


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
            Toast.makeText(UsdpMainActivity.this, "Service connected",
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;

            // As part of the sample, tell the user what happened.
            Toast.makeText(UsdpMainActivity.this, "Service disconnected",
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

    /*
    following: mechanism dialog creation
    TODO move mechanism handling to a dedicated class

     */

    private void showAuthCamDialogFragment(String phrase, String tDevice) {
        authDialog = new CamAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SibBlink");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "Use camera on other device to capture blinking sequence");
        bundle.putString(CamAuthDialogFragment.AUTH_PATTERN, phrase);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsibcam");
        }
    }

    private void showAuthVicNDialogFragment(String number, String tDevice) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_DATA, number);
        bundle.putString(AuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_N);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VC-N: compare numbers");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with number on other device");
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvcn");
        }
    }

    private void showAuthVicPDialogFragment(String phrase, String tDevice) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_DATA, phrase);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_P);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VC-P: compare phrases");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with phrase on other device");
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvcp");
        }
    }

    private void showHapaRecAuthDialogFragment(String tDevice, String data) {
        authDialog = new HapaRecAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_MECHTYPE, OOBData.HAPADEP);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "HAPADEP: audio verification");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "wait until sequence completed");
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        bundle.putString(AuthDialogFragment.AUTH_DATA, data);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "Hapadep");
        }
    }

    private void showAuthVerifDialogFragment(String title, String info, String mechtype, String tDevice) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, mechtype);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), title);
        }
    }

    private void showAuthLacdsDialogFragment(String title, String info, String phrase, String tDevice) {
        authDialog = new StringAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_DATA, phrase);
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(AuthDialogFragment.AUTH_MECHTYPE, OOBData.LaCDS);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authLaC_DS");
        }
    }

    private void showAuthInfoDialogFragment(String info, String tDevice) {
        authDialog = new SwbuAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(SwbuAuthDialogFragment.AUTH_INFOONLY, info);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        bundle.putString(AuthDialogFragment.AUTH_MECHTYPE, OOBData.SWBU);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "tzefuginfo");
        }
    }


    private void showAuthVicIDialogFragment(Bitmap bmp, String tDevice) {

        authDialog = new ImgAuthDialogFragment();
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "VC-I: compare images");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "compare with image on other device");
        bundle.putIntArray(ImgAuthDialogFragment.IMG_IMAGE, pixels);
        bundle.putInt(ImgAuthDialogFragment.IMG_HEIGHT, bmp.getHeight());
        bundle.putInt(ImgAuthDialogFragment.IMG_WIDTH, bmp.getWidth());
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.VIC_I);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authvci");
        }
    }

    private void showAuthBlSibDialogFragment(boolean[] pattern, String tDevice) {
        authDialog = new LEDBlinkAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "SiBblink: blinking sequence");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "capture blinking sequence");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.SiBBlink);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBedaVibDialogFragment(boolean[] pattern, String tDevice) {
        authDialog = new BEDA_VibAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA Vibrate-Button");
        bundle.putString(AuthDialogFragment.AUTH_EXPLINFO, "press button on other device simultaneously to vibration");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press OK when finished");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBEDA_LB_DialogFragment(boolean[] pattern, String tDevice) {
        authDialog = new LEDBlinkAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, "BEDA LED-Button");
        bundle.putString(AuthDialogFragment.AUTH_INFO, "press button on other device simultaneously when LED is bright");
        bundle.putBooleanArray(BarSibAuthDialogFragment.AUTH_PATTERN, pattern);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, OOBData.BEDA_LB);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authblsib");
        }
    }

    private void showAuthBarcodeDialogFragment(Bitmap bmp, String tDevice) {
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
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "authbarsib");
        }
    }

    private void showBEDARecBtnDialogFragment(String title, String info, String bedaType, String tDevice) {
        authDialog = new BEDA_BtnAuthDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AuthDialogFragment.AUTH_TITLE, title);
        bundle.putString(AuthDialogFragment.AUTH_INFO, info);
        bundle.putString(StringAuthDialogFragment.AUTH_MECHTYPE, bedaType);
        bundle.putString(AuthDialogFragment.AUTH_TARGET_DVC, tDevice);
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "beda_vb");
        }
    }


    // initialization of GUI components
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
                    case R.id.mnu_wifi_settings:
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        break;
                }
                return false;
            }
        });


        /*FloatingActionButton  = (FloatingActionButton) findViewById(R.id.fab);
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
                        // TODO remove? litter box
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
                dtmfCharacters.clear();
            }
        });
*/
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

    // device list item menu - items
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
            menu.add(Menu.NONE, CTXM_PAIR, CTXM_PAIR, R.string.pair);
            menu.add(Menu.NONE, CTXM_GETRPRT, CTXM_GETRPRT, R.string.get_report);
            if (selDevice.getState() == WifiP2pDevice.CONNECTED) {
                menu.add(Menu.NONE, CTXM_DISCONN, CTXM_DISCONN, R.string.disconnect);
            } else if (selDevice.getState() == WifiP2pDevice.INVITED) {
                menu.add(Menu.NONE, CTXM_CNCLINV, CTXM_CNCLINV, R.string.cancel_invite);
            }
        }

    }

    private void showShortToast(String text) {
        Toast.makeText(UsdpMainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String text) {
        Toast.makeText(UsdpMainActivity.this, text, Toast.LENGTH_LONG).show();
    }


    // device list item menu - actions
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        final String selectedDeviceMac = ((TextView) info.targetView.findViewById(android.R.id.text2)).getText().toString();
        switch (item.getItemId()) {
            case CTXM_SENDMSG:
                // message dialog
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(UsdpMainActivity.this);
                LayoutInflater inflater = UsdpMainActivity.this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.dialog_message, null);
                dialogBuilder.setView(dialogView);

                final EditText et_msg = (EditText) dialogView.findViewById(R.id.et_msg);

                dialogBuilder.setTitle("Send secure message");
                dialogBuilder.setMessage("Enter text below");
                dialogBuilder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendMsgtoService(Message.obtain(null, UsdpService.MSG_SEND_ENCRYPTED, new TargetMsg(selectedDeviceMac, et_msg.getText().toString())));
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //pass
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();


                return true;
            case CTXM_PAIR:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_PAIRSEC, selectedDeviceMac));
                return true;
            case CTXM_GETRPRT:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_REQ_CONNINFO, selectedDeviceMac));
                return true;
            case CTXM_DISCONN:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_DISCONNECT, selectedDeviceMac));
                showShortToast("Disconnecting");
                return true;
            case CTXM_CNCLINV:
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_ABRT_CONN, selectedDeviceMac));
                showShortToast("Canceling Invitation");
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    //TODO move and control from dialog
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
        if (dev != null) {
            String devName = dev.deviceName;
            if (dev.isGroupOwner()) {
                devName += " (GO)";
            }
            String devAddress = dev.deviceAddress;
            tv_status.setText("Device info:\n" + devName
                    + " (" + devAddress + ")");
        } else {
            Log.d(LOGTAG, "Device was null!");
        }
    }

    private void playHapaSequence(final ArrayList<Integer> digits) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                if (digits != null && !digits.isEmpty()) {
                    ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_DTMF, 50);
                    // delay for other device to get started
                    playSound(toneGen, ToneGenerator.TONE_CDMA_SIGNAL_OFF, 1000);
                    for (Integer val : digits) {
                        playSound(toneGen, val, 300);
                        // use as seperator
                        playSound(toneGen, ToneGenerator.TONE_DTMF_A, 100);
                    }
                    playSound(toneGen, ToneGenerator.TONE_DTMF_B, 100);
                    transformAndTalk(digits.get(digits.size() - 1));
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
        String rcvdStr = new String(msg.getRecords()[0].getPayload());
        String[] parts = rcvdStr.split("/");
        String addr = parts[0];
        String data = parts[1];
        oobResult(addr, OOBData.NFC, data);
        Toast.makeText(UsdpMainActivity.this, "NFC message received! (" + addr + ") " + data, Toast.LENGTH_SHORT).show();
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
                String rcvdStr = data.getStringExtra("SCAN_RESULT");
                String[] parts = rcvdStr.split("/");
                String addr = parts[0];
                String contents = parts[1];
                oobResult(addr, OOBData.SiB, contents);
                Toast.makeText(UsdpMainActivity.this, contents, Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
                Toast.makeText(UsdpMainActivity.this, "intent was cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void transformAndTalk(int num) {
        talk(smplMadlib.getSentence(num));
    }

    public void talk(String text) {
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

    private void manageAuthenticationDialog(TargetMsg tMsg) {
        OOBData oobData = (OOBData) tMsg.getObj();
        String tDevice = tMsg.getTargetAddress();
        int authdata = oobData.getAuthdata();
        String authdataStr = String.valueOf(authdata);
        switch (oobData.getType()) {
            case OOBData.VIC_I:
                final Identicon identicon = new AsymmetricIdenticon(getApplicationContext());
                Bitmap bm = identicon.getBitmap(authdataStr);
                showAuthVicIDialogFragment(bm, tDevice);
                break;
            case OOBData.VIC_N:
                showAuthVicNDialogFragment(authdataStr, tDevice);
                break;
            case OOBData.VIC_P:
                showAuthVicPDialogFragment(smplMadlib.getSentence(authdata), tDevice);
                break;
            case OOBData.SiB:
                if (oobData.isSendingDevice()) {
                    showAuthBarcodeDialogFragment(Helper.generateQR(tMsg.getSenderAddress() + '/' + authdataStr), tDevice);
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
                    boolean[] pattern = Helper.getBinaryArray(authdata, SecureAuthentication.OOB_BITLENGTH);
                    showAuthBlSibDialogFragment(pattern, tDevice);
                } else {
                    showAuthCamDialogFragment("SiBBlink", tDevice);
                }
                break;
            case OOBData.LaCDS:
                String sentenceDS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    showAuthLacdsDialogFragment("LaC DS", "compare spoken and displayed sentence", sentenceDS, tDevice);
                } else {
                    showAuthVerifDialogFragment("LaC DS", "compare spoken and displayed sentence", OOBData.LaCDS, tDevice);
                    talk(sentenceDS);

                }
                break;
            case OOBData.LaCSS:
                String sentenceSS = smplMadlib.getSentence(authdata);
                if (oobData.isSendingDevice()) {
                    //speaker
                    showAuthVerifDialogFragment("LaC SS", "compare both spoken sentences", OOBData.LaCSS, tDevice);
                    talk(sentenceSS);
                } else {
                    //speaker
                    showAuthVerifDialogFragment("LaC SS", "compare both spoken sentences", OOBData.LaCSS, tDevice);
                    talk(sentenceSS);
                }
                break;
            case OOBData.BEDA_VB:
                if (oobData.isSendingDevice()) {
                    //vibrate
                    showAuthBedaVibDialogFragment(Helper.getBinaryArray(authdata, SecureAuthentication.OOB_BITLENGTH), tDevice);
                } else {
                    showBEDARecBtnDialogFragment("BEDA Vibrate Button", "press button when triggered by vibration", OOBData.BEDA_VB, tDevice);
                }
                break;
            case OOBData.BEDA_LB:
                if (oobData.isSendingDevice()) {
                    showAuthBEDA_LB_DialogFragment(Helper.getBinaryArray(authdata, SecureAuthentication.OOB_BITLENGTH), tDevice);
                } else {
                    showBEDARecBtnDialogFragment("BEDA LED Button", "press button as long as triggered by LED", OOBData.BEDA_LB, tDevice);
                }
                break;
            case OOBData.BEDA_BPBT:
                if (oobData.isSendingDevice()) {
                    //speaker
                    boolean[] data = Helper.getBinaryArray(authdata, SecureAuthentication.OOB_BITLENGTH);
                    playSequence(data);
                } else {
                    showBEDARecBtnDialogFragment("BEDA Beep Button", "press button as long as triggered by beeping", OOBData.BEDA_BPBT, tDevice);
                }
                break;
            case OOBData.BEDA_BTBT:
                // TODO not using SAS protocol
                if (oobData.isSendingDevice()) {
                    //button
                    showBEDARecBtnDialogFragment("BEDA Button Button", "press button several random times simultaneously on both devices", OOBData.BEDA_BTBT, tDevice);
                } else {
                    showBEDARecBtnDialogFragment("BEDA Button Button", "press button several random times simultaneously on both devices", OOBData.BEDA_BTBT, tDevice);
                }
                break;
            case OOBData.HAPADEP:
                String humVerifStr = String.valueOf(authdata % 10);
                if (oobData.isSendingDevice()) {
                    //speaker
                    playHapaSequence(Helper.getDigitlistFromInt(authdata));
                } else {
                    //mic + speaker
                    showHapaRecAuthDialogFragment(tDevice, humVerifStr);
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
                                    "application/vnd.de.tu_darmstadt.seemoo.usdpprototype", (tMsg.getSenderAddress() + '/' + authdataStr).getBytes())
                            });
                    mNfcAdapter.setNdefPushMessage(msg, UsdpMainActivity.this);
                    showAuthVerifDialogFragment("NFC", "hold devices together", OOBData.NFC, tDevice);
                } else {
                    //nfc receive

                }
                break;
            case OOBData.SWBU:
                if (oobData.isSendingDevice()) {
                    //accel
                    //Swbu swbu = new Swbu(getApplication());

                    showAuthInfoDialogFragment("shakeshake", tDevice);
                } else {
                    //accel
                    //Swbu swbu = new Swbu(getApplication());
                    showAuthInfoDialogFragment("shakeshake", tDevice);
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
    public void oobResult(String tDevice, String mech, boolean result) {

        AuthResult res = new AuthResult(mech, result);
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_ACCEPT_AUTH, new TargetMsg(tDevice, res)));

    }

    @Override
    public void oobResult(String tDevice, String mech, String data) {
        AuthResult res = new AuthResult(mech, data);
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_ACCEPT_AUTH_WDATA, new TargetMsg(tDevice, res)));
    }


    @Override
    public void oobGenAuthResult(String tDevice, String mech, String data) {
        AuthResult res = new AuthResult(mech, data);
        sendMsgtoService(Message.obtain(null, UsdpService.MSG_GEN_AUTH_DATA, new TargetMsg(tDevice, res)));
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
                    final TargetMsg iMsg = (TargetMsg) msg.obj;
                    final AuthMechanism[] types = (AuthMechanism[]) iMsg.getObj();

                    AuthMechArrayAdapter adap = new AuthMechArrayAdapter(UsdpMainActivity.this, R.layout.mech_list_item, types);

                    AlertDialog.Builder builder = new AlertDialog.Builder(UsdpMainActivity.this);
                    builder.setTitle("Choose authentication mechanism");
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // TODO maybe tell service pairing was aborted
                            dialog.dismiss();
                        }
                    });
                    builder.setAdapter(adap,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int item) {
                                    Message msg = Message.obtain(null,
                                            UsdpService.MSG_CHOSEN_AUTHMECH, new TargetMsg(iMsg.getTargetAddress(), iMsg.getSenderAddress(), types[item].getShortName()));
                                    sendMsgtoService(msg);
                                    dialog.dismiss();
                                }
                            });
                    builder.show();

                    break;
                case UsdpService.MSG_AUTHENTICATION_DIALOG_DATA:
                    Toast.makeText(UsdpMainActivity.this, "auth shows now", Toast.LENGTH_SHORT).show();
                    manageAuthenticationDialog((TargetMsg) msg.obj);

                    break;
                case UsdpService.MSG_CONNINFO:
                    ConnInfo info = (ConnInfo) msg.obj;
                    //ReportDialogFragment rdf = new ReportDialogFragment();
                    //rdf.show(getSupportFragmentManager(), info.toString());
                    if (info == null) {
                        Toast.makeText(UsdpMainActivity.this, "no secure connection established", Toast.LENGTH_SHORT).show();
                    } else {
                        AlertDialog connInfoDialog = new AlertDialog.Builder(UsdpMainActivity.this).create();
                        connInfoDialog.setTitle("Connection info");
                        connInfoDialog.setMessage(info.toString());

                        connInfoDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        connInfoDialog.show();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
