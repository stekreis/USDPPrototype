package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechManager;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecAuthVIC;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecureAuthentication;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.ListDevice;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ClientSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.MessageManager;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ServerSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

//wifip2p

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
    public static final int MSG_CONFIG = 3;
    public static final int MSG_TEST = 4;
    public static final int MSG_DISCOVERPEERS = 5;
    public static final int MSG_PEERSDISCOVERED = 6;
    public static final int MSG_CONNECT = 7;
    public static final int MSG_PAIR = 8;
    public static final int MSG_CHATMSGRECEIVED = 11;
    public static final int MSG_SENDCHATMSG = 12;
    public static final int AUTH_INITMSG = 13;
    public static final int AUTH_BARCODE = 14;
    public static final int MSG_AUTHMECHS = 15;
    public static final int MSG_AUTHENTICATION_DIALOG_DATA = 16;
    public static final int MSG_CHOSEN_AUTHMECH = 17;
    public static final int MSG_CHOSEN_ROLE = 18;
    public static final int MSG_AUTHENTICATION_DIALOG_DATA_REM = 19;
    public static final int MSG_UNPAIR = 20;
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    private final String LOGTAG = "UsdpService";
    private SecureAuthentication secureAuthentication = null;

    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, WifiP2pDevice> discoveredDevices = new HashMap<>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Handler handler = new Handler(this);
    private Messenger activityMessenger;
    private MessageManager messageManager;
    private AuthMechManager authMechManager;
    private DeviceCapabilities deviceCapabilities;

    private UsdpPacket remoteDevice;

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

        authMechManager = new AuthMechManager();
        AssetManager am = getApplicationContext().getAssets();
        try {
            InputStream is = am.open("authmechanisms.json");

            authMechManager.readJsonStream(is);
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
        }


        // wifip2p logic
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, mChannel, this);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver, mIntentFilter);
    }


    public void peersAvailable(WifiP2pDeviceList peers) {
        WifiP2pDevice device;
        Log.d(LOGTAG, "list incoming");
        ArrayList<ListDevice> listdevices = new ArrayList<>();
        for (WifiP2pDevice peer : peers.getDeviceList()) {
            device = peer;
            String deviceaddr = device.deviceAddress;
            discoveredDevices.put(deviceaddr, device);
            listdevices.add(new ListDevice(deviceaddr, device.deviceName, device.status));
            Log.d(LOGTAG, deviceaddr + " found");
        }
        if (activityMessenger != null) {
            /*ArrayList<String> deviceMacs = new ArrayList<>(discoveredDevices.keySet());*/
            sendMsgToActivity(Message.obtain(null,
                    MSG_PEERSDISCOVERED, listdevices));
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
        Thread handler;
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
                }
            }
            Log.d(LOGTAG, "qrCreated");

        } catch (WriterException e) {
            e.printStackTrace();
        }
        return mBitmap;
    }

    /*
    handles incoming messages from other device forwarded by MessageManager
     */
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
                    case MessageManager.MSGTYPE_HELLO:
                        Log.d(LOGTAG, "kokett will be sent");
                        if (messageManager != null) {
                            byte[] recData = (byte[]) msg.obj;
                            byte[] data = new byte[recData.length - 1];
                            System.arraycopy(recData, 1, data, 0, recData.length - 1);
                            UsdpPacket prettyData = (UsdpPacket) SerializationUtils.deserialize(data);
                            if (secureAuthentication == null) {
                                secureAuthentication = new SecAuthVIC();
                                secureAuthentication.init();
                            }
                            int publicKey = secureAuthentication.getPublicDeviceKey();

                            String[] supRecMechs = authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities());
                            String[] supSendMechs = authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities());
                            byte[] dataSend = SerializationUtils.serialize(new UsdpPacket(supRecMechs, supSendMechs, publicKey));
                            ByteBuffer target = ByteBuffer.allocate(dataSend.length + 1);
                            target.put(MessageManager.MSGTYPE_HELLOBACK);
                            target.put(dataSend);

                            messageManager.write(target.array());
                            Log.d(LOGTAG, "kokett sent");
                        }
                        break;
                    case MessageManager.MSGTYPE_HELLOBACK:
                        byte[] recData = (byte[]) msg.obj;
                        byte[] data = new byte[recData.length - 1];
                        System.arraycopy(recData, 1, data, 0, recData.length - 1);
                        UsdpPacket prettyData = (UsdpPacket) SerializationUtils.deserialize(data);

                        remoteDevice = prettyData;

                        HashSet<String> res = new HashSet<>();

                        authMechManager.findComp(res, prettyData.getMechsSend(), authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities()));
                        authMechManager.findComp(res, prettyData.getMechsRec(), authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities()));

                        String[] resRay = res.toArray(new String[res.size()]);

                        AuthMechanism[] res2 = authMechManager.sortAuthMechsBySec(resRay);


                        for (int pos = 0; pos < resRay.length; pos++) {
                            resRay[pos] = res2[pos].getShortName(); //  + " (" + res2[pos].getSecPoints() + ")";
                        }

                        //String[] authMechs = authMechManager.getSupportedMechs(deviceCapabilities.getValidCapabilities());

                        Message authMechsMsg = Message.obtain(null, MSG_AUTHMECHS, resRay);

                        sendMsgToActivity(authMechsMsg);

                        Log.d(LOGTAG, "chustcheckin " + prettyData.getMechsRec()[0]);
                        break;
                    case MessageManager.MSGTYPE_AUTH_DIALOG:
                        byte[] recAuthData = (byte[]) msg.obj;
                        byte[] actualData = new byte[recAuthData.length - 1];
                        System.arraycopy(recAuthData, 1, actualData, 0, recAuthData.length - 1);
                        OOBData prettyData2 = (OOBData) SerializationUtils.deserialize(actualData);
                        Message retmsg = Message.obtain(null,
                                MSG_AUTHENTICATION_DIALOG_DATA, prettyData2);
                        sendMsgToActivity(retmsg);
                    default:
                        Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) msg.obj, 0, msg.arg1));
                        Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) msg.obj, 1, msg.arg1));
                }
                break;
            default:
        }

        return true;
    }

    public void setMessageManager(MessageManager obj) {
        messageManager = obj;
        Log.d(LOGTAG, "messman was set" + obj);
    }


    /**
     * inner class, handles messages from @UsdpMainActivity
     */
    class InternalMsgIncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: hello!", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_UNPAIR:
                    //TODO: unpair
                    break;
                case MSG_CHOSEN_AUTHMECH:
                    String[] supSendMechs = authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities());
                    boolean matches = false;
                    String authMechStr = (String) msg.obj;
                    for (int pos = 0; pos < supSendMechs.length && !matches; pos++) {
                        if (supSendMechs[pos].equals(authMechStr)) {
                            String[] supRemoteRecMechs = remoteDevice.getMechsRec();
                            for (int rpos = 0; rpos < supRemoteRecMechs.length && !matches; rpos++) {
                                if (supRemoteRecMechs[rpos].equals(authMechStr)) {
                                    matches = true;
                                    Log.d(LOGTAG, "found match thissend");
                                    //this device sends, remote device receives
                                    OOBData data = new OOBData(authMechStr, secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey()), true);
                                    Message retmsg = Message.obtain(null,
                                            MSG_AUTHENTICATION_DIALOG_DATA, data);
                                    sendMsgToActivity(retmsg);
                                    //TODO inform other device to receive
                                    OOBData dataRemote = new OOBData(authMechStr, secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey()), false);
                                    if (messageManager != null) {

                                        byte[] dataRem = SerializationUtils.serialize(dataRemote);
                                        ByteBuffer target = ByteBuffer.allocate(dataRem.length + 1);
                                        target.put(MessageManager.MSGTYPE_AUTH_DIALOG);
                                        target.put(dataRem);
                                        messageManager.write(target.array());
                                    }
                                }
                            }
                        }
                    }
                    if (!matches) {
                        String[] supRecMechs = authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities());
                        for (int pos = 0; pos < supRecMechs.length && !matches; pos++) {
                            if (supRecMechs[pos].equals(authMechStr)) {
                                String[] supRemoteSendMechs = remoteDevice.getMechsSend();
                                for (int rpos = 0; rpos < supRemoteSendMechs.length && !matches; rpos++) {
                                    if (supRemoteSendMechs[rpos].equals(authMechStr)) {
                                        matches = true;
                                        Log.d(LOGTAG, "found match thisreceive");
                                        //remote device sends, this device receives
                                        OOBData data = new OOBData(authMechStr, secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey()), false);
                                        Message retmsg = Message.obtain(null,
                                                MSG_AUTHENTICATION_DIALOG_DATA, data);
                                        sendMsgToActivity(retmsg);
                                        //TODO inform other device to send
                                    }
                                }
                            }
                        }
                    }
                    if (!matches) {
                        // invalid
                    }
                    break;
                case MSG_AUTHMECHS:
             /*       String[] authMechs = authMechManager.getSupportedMechs();
                    Message authMechsMsg = Message.obtain(null, MSG_AUTHMECHS, authMechs);
                    sendMsgToActivity(authMechsMsg);*/
                    break;
                case MSG_REGISTER_CLIENT:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: client registered", Toast.LENGTH_SHORT).show();
                    activityMessenger = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Toast.makeText(getApplicationContext(), "ACTSEND, Service says: client unregistered", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_CONFIG:
                    Toast.makeText(getApplicationContext(), "Service started. Device config transmitted", Toast.LENGTH_SHORT).show();
                    deviceCapabilities = (DeviceCapabilities) msg.obj;

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
                    final String connDevAddr = msg.obj.toString();
                    WifiP2pDevice dev = discoveredDevices.get(connDevAddr);
                    if (dev != null) {
                        switch (dev.status) {
                            case WifiP2pDevice.AVAILABLE:
                                // if available, just connect
                                Toast.makeText(getApplicationContext(), "connecting to device", Toast.LENGTH_SHORT).show();
                                if (dev != null) {
                                    WifiP2pConfig config = new WifiP2pConfig();
                                    config.deviceAddress = dev.deviceAddress;
                                    wifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                                        @Override
                                        public void onSuccess() {
                                            Log.d(LOGTAG, "success");
                                        }

                                        // TODO: pair
                                        @Override
                                        public void onFailure(int reason) {
                                            Log.d(LOGTAG, "failure");
                                        }
                                    });
                                }
                                break;
                            case WifiP2pDevice.CONNECTED:
                                // send first hello
                                if (messageManager != null) {
                                    Toast.makeText(getApplicationContext(), "messman not null", Toast.LENGTH_SHORT).show();
                                    if (secureAuthentication == null) {
                                        secureAuthentication = new SecAuthVIC();
                                        secureAuthentication.init();
                                    }
                                    int publicKey = secureAuthentication.getPublicDeviceKey();

                                    byte[] data = SerializationUtils.serialize(new UsdpPacket("uniqueID", "protVersion1.0", publicKey));
                                    ByteBuffer target = ByteBuffer.allocate(data.length + 1);
                                    target.put(MessageManager.MSGTYPE_HELLO);
                                    target.put(data);
                                    messageManager.write(target.array());
                                }
                                Toast.makeText(getApplicationContext(), "pairing in progress...", Toast.LENGTH_SHORT).show();
                                break;
                            case WifiP2pDevice.INVITED:
                                Toast.makeText(getApplicationContext(), "other device already invited...", Toast.LENGTH_SHORT).show();
                                //do nothing
                                break;
                        }
                    }
          /*          if (dev.deviceAddress.equals(connDevAddr)) {
                        Log.d(LOGTAG, "device found! " + dev.deviceAddress + "state : " + dev.status);
                    }else{
                        Log.d(LOGTAG, "other peer found!: " + dev.deviceAddress + "state : " + dev.status);
                    }*/


                    //check connection state of device

                   /* int state;
                    wifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {

                            ArrayList<WifiP2pDevice> peerlist = new ArrayList<WifiP2pDevice>(peers.getDeviceList());
                            ListIterator<WifiP2pDevice> liter = peerlist.listIterator();
                            while (liter.hasNext()) {
                                WifiP2pDevice dev = liter.next();
                                if (dev.deviceAddress.equals(connDevAddr)) {
                                    Log.d(LOGTAG, "device found! " + dev.deviceAddress + "state : " + dev.status);
                                }else{
                                    Log.d(LOGTAG, "other peer found!: " + dev.deviceAddress + "state : " + dev.status);
                                }
                            }
                        }
                    });*/

                    break;
                case MSG_PAIR:
                    if (secureAuthentication == null) {
                        secureAuthentication = new SecAuthVIC();
                        secureAuthentication.init();
                    }
                    int publicKey = secureAuthentication.getPublicDeviceKey();

                    Log.d(LOGTAG, "publicKey: " + publicKey);
                    byte[] authMyPublicKey = ByteBuffer.allocate(4).putInt(publicKey).array();

                    if (messageManager != null) {
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
                        String text = (String) msg.obj;
                        messageManager.write(text.getBytes());
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
