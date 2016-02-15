package de.tu_darmstadt.seemoo.usdpprototype;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

//wifip2p
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecAuthManaIV;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecureAuthentication;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.MessageManager;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ClientSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ServerSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by kenny on 29.01.16.
 */
public class UsdpService extends Service implements WifiP2pManager.ConnectionInfoListener, UsdpMainActivity.MessageTarget, Handler.Callback {

    public static final int SERVER_PORT = 4545;
    /**
     * Command to the service to display a message
     */
    public static final int MSG_SAY_HELLO = 0;
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;
    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    public static final int MSG_SET_VALUE = 3;
    public static final int MSG_TEST = 4;
    public static final int MSG_DISCOVERPEERS = 5;
    public static final int MSG_PEERSDISCOVERED = 6;
    public static final int MSG_CONNECT = 7;
    public static final int MSG_PAIR = 8;
    public static final int MSG_CHATMSGRECEIVED = 11;
    public static final int MSG_SENDCHATMSG = 12;
    public static final int AUTH_INITMSG = 13;
    public static final int AUTH_BARCODE = 14;
    final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    SecureAuthentication secureAuthentication = null;
    //TTStest
    TextToSpeech tts;
    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, WifiP2pDevice> discoveredDevices = new HashMap<String, WifiP2pDevice>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private Handler handler = new Handler(this);
    private Messenger activityMessenger;
    private String LOGTAG = "UsdpService";
    private MessageManager messageManager;

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {

        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOGTAG, "service was started. info:" + intent.getStringExtra("KEY1"));

        if (intent.getFlags() == START_FLAG_REDELIVERY) {
            //service was restarted

        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        Log.d(LOGTAG, "service was destroyed");
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Log.d(LOGTAG, "onCreate");
        Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show(); //is shown


        // wifip2p logic
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver, mIntentFilter);

        initTts();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void talk() {
        Log.d(LOGTAG, "i toagd1");

        //TODO support old API (then remove  @TargetApi(Build.VERSION_CODES.LOLLIPOP))
        //tts.speak("jet fuel", TextToSpeech.QUEUE_ADD, null, "testmessage673");
        Log.d(LOGTAG, "i toagd2");
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

    public void peersAvailable(WifiP2pDeviceList peers) {
        WifiP2pDevice device = null;
        Log.d(LOGTAG, "list incoming");

        for (WifiP2pDevice peer : peers.getDeviceList()) {
            device = peer;
            String deviceaddr = device.deviceAddress;
            discoveredDevices.put(deviceaddr, device);
            Log.d(LOGTAG, deviceaddr + " found");
        }
        if (activityMessenger != null) {
            ArrayList<String> deviceMacs = new ArrayList<String>(discoveredDevices.keySet());
            sendMsgToActivity(Message.obtain(null,
                    MSG_PEERSDISCOVERED, deviceMacs));
        }
    }

    private void sendMsgToActivity(Message msg) {
        try {
            activityMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * ServerSocketHandler}
         */

        if (p2pInfo.isGroupOwner) {
            Log.d(LOGTAG, "Connected as group owner");
            try {
                handler = new ServerSocketHandler(
                        ((UsdpMainActivity.MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(LOGTAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(LOGTAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    ((UsdpMainActivity.MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }

        // TODO inform Activity and provide message input
        /*chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container_root, chatFragment).commit();
        statusTxtView.setVisibility(View.GONE);*/
    }


    private Bitmap generateQRCode(String input) {
        BitMatrix qrMatrix;
        QRCodeWriter writer = new QRCodeWriter();
        Bitmap mBitmap = null;
        try {
            qrMatrix = writer.encode(input, BarcodeFormat.QR_CODE, 100, 100);
            mBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
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
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MessageManager.MC_INITHANDLER:
                Object obj = msg.obj;
                Log.d(LOGTAG, "initiating MessageManager");
                setMessageManager((MessageManager) obj);
                break;
            case MessageManager.MC_MSGRECEIVED:
                byte[] readBuf = (byte[]) msg.obj;
                byte msg_type = readBuf[0];
                Log.d(LOGTAG, msg_type + "");
                switch (msg_type) {
                    case MessageManager.MSGTYPE_PREAUTH:

                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 1, msg.arg1);
                        Log.d(LOGTAG, readMessage);
                        sendMsgToActivity(Message.obtain(null, MSG_CHATMSGRECEIVED, readMessage));
                        break;
                    case MessageManager.MSGTYPE_INAUTH:
                        sendMsgToActivity(Message.obtain(null,
                                AUTH_BARCODE, generateQRCode("jet fuel")));

                        //send int as byte[]
                        byte[] bytes = ByteBuffer.allocate(4).putInt(123123).array();
                        Log.d(LOGTAG, bytes.toString() + "");
                        //int resultInt = ByteBuffer.wrap(bytes).getInt();
                        int resultInt = ByteBuffer.wrap(Arrays.copyOfRange(readBuf, 1, readBuf.length)).getInt();
                        Log.d(LOGTAG, resultInt + "");
                        break;
                    case MessageManager.MSGTYPE_POSTAUTH:
                        Log.d(LOGTAG, "postauth");
                        break;
                    default:
                        Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) msg.obj, 0, msg.arg1));
                        Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) msg.obj, 1, msg.arg1));
                        talk();
                }
                break;
            default:
        }
        return true;
    }

    public void setMessageManager(MessageManager obj) {
        messageManager = obj;
    }


    /**
     * inner class, handles messages from @UsdpMainActivity
     */
    class InternalMsgIncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: hello!", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_REGISTER_CLIENT:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: client registered", Toast.LENGTH_SHORT).show();
                    activityMessenger = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: client unregistered", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SET_VALUE:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: value set", Toast.LENGTH_SHORT).show();
                    sendMsgToActivity(Message.obtain(null,
                            MSG_SAY_HELLO));
                    break;
                case MSG_TEST:
                    Log.d(LOGTAG, "testmsg received");
                    break;
                case MSG_DISCOVERPEERS:
                    wifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOGTAG, "discoverSuccess");
                        }

                        @Override
                        public void onFailure(int reasonCode) {

                        }
                    });
                    break;
                case MSG_CONNECT:
                    WifiP2pDevice device = discoveredDevices.get(msg.obj.toString());
                    if (device != null) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        wifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(LOGTAG, "success");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(LOGTAG, "failure");
                            }
                        });
                    }
                    break;
                case MSG_PAIR:
                    if (secureAuthentication == null) {
                        secureAuthentication = new SecAuthManaIV();
                        secureAuthentication.init();
                    }
                    int publicKey = secureAuthentication.getPublicDeviceKey();

                    Toast.makeText(getApplicationContext(), publicKey + "", Toast.LENGTH_LONG);

                    Log.d(LOGTAG, "publicKey: " + publicKey);
                    byte[] authMyPublicKey = ByteBuffer.allocate(4).putInt(publicKey).array();

                    if (messageManager != null) {
                        //byte[] msg_type = {};
                        //byte[] testmessage = "testmessage".getBytes();
                        byte[] testmessage = authMyPublicKey;
                        ByteBuffer target = ByteBuffer.allocate(testmessage.length + 1);
                        target.put(MessageManager.MSGTYPE_INAUTH);
                        target.put(testmessage);

                        messageManager.write(target.array());


                        // messageManager.write(authMyPublicKey);

                        //pushMessage("Me: " + chatLine.getText().toString());
                    }
                    break;
                case MSG_SENDCHATMSG:
                    if (messageManager != null) {
                        messageManager.write("hello there!".getBytes());
                        //pushMessage("Me: " + chatLine.getText().toString());
                        talk();
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
