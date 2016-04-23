package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.scottyab.aescrypt.AESCrypt;

import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechManager;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthResult;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecAuthVIC;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecureAuthentication;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.UsdpCipher;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.IpMacPacket;
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
    public static final int MSG_DISCOVERPEERS = 5;
    public static final int MSG_PEERSDISCOVERED = 6;
    public static final int MSG_CONNECT = 7;
    public static final int MSG_AUTHMECHS = 15;
    public static final int MSG_AUTHENTICATION_DIALOG_DATA = 16;
    public static final int MSG_CHOSEN_AUTHMECH = 17;
    public static final int MSG_UNPAIR = 20;
    public static final int MSG_WIFIDEVICE_CHANGED = 22;
    public static final int MSG_DISCONNECT = 23;
    public static final int MSG_ABRT_CONN = 24;
    public static final int MSG_ACCEPT_AUTH = 25;
    public static final int MSG_ACCEPT_AUTH_WDATA = 26;
    public static final int MSG_SEND_ENCRYPTED = 27;
    public static final int MSG_REQ_CONNINFO = 28;
    public static final int MSG_CONNINFO = 29;

    public static final int MSG_TEST = 30;

    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    private final String LOGTAG = "UsdpService";
    int x = 0;
    private SecureAuthentication secureAuthentication = null;
    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, MyWifiP2pDevice> discoveredDevicesNEW = new HashMap<>();
    private HashMap<String, WifiP2pDevice> discoveredDevices = new HashMap<>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Handler handler = new Handler(this);
    private Messenger activityMessenger;
    private MessageManager messageManager;
    private AuthMechManager authMechManager;
    private DeviceCapabilities deviceCapabilities;
    private ArrayList<MessageManager> messageManagers = new ArrayList<>();

    private HashMap<InetAddress, MessageManager> tempDevices = new HashMap<>();

    private InetAddress goAddress;

    private WifiP2pDevice thisDevice;

    private UsdpPacket remoteDevice;
    private String uniqueID;
    private ConnInfo connInfo;

    // if group owner, keep list of all devices. This involves connection information
    // android unique id
    // conn info (failed connections, timestamps, pairing duration
    // mac, ip


    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "service binding");
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


        uniqueID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

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

        Log.d(LOGTAG, "list incoming");
        ArrayList<ListDevice> listdevices = new ArrayList<>();
        for (WifiP2pDevice device : peers.getDeviceList()) {
            String deviceaddr = device.deviceAddress;
            discoveredDevices.put(deviceaddr, device);
            listdevices.add(new ListDevice(deviceaddr, device.deviceName, device.status, device.isGroupOwner()));
            Log.d(LOGTAG, deviceaddr + " found");
        }
        if (activityMessenger != null) {
            /*ArrayList<String> deviceMacs = new ArrayList<>(discoveredDevices.keySet());*/
            sendMsgToActivity(Message.obtain(null,
                    MSG_PEERSDISCOVERED, listdevices));
        }
    }

    private void sendMsgToActivity(Message msg) {
        if (activityMessenger != null) {
            try {
                activityMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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

        Toast.makeText(getApplicationContext(), "goaddress: " + p2pInfo.groupOwnerAddress, Toast.LENGTH_LONG).show();

        if (p2pInfo.isGroupOwner) {
            Log.d(LOGTAG, "Connected as group owner");
            //goAddress = p2pInfo.groupOwnerAddress;
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
            Toast.makeText(getApplicationContext(), "imapeer", Toast.LENGTH_LONG).show();
            Log.d(LOGTAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    ((UsdpMainActivity.MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }
    }


    private void requestMacFromClients() {
        if (!tempDevices.isEmpty()) {
            for (MessageManager curr : tempDevices.values()) {

                ByteBuffer target = ByteBuffer.allocate(1);
                target.put(MessageManager.MSGTYPE_REQMAC);
                messageManager.write(target.array());
            }
        }
    }

    private byte[] getContent(Object obj) {
        byte[] recData = (byte[]) obj;
        byte[] data = new byte[recData.length - 1];
        System.arraycopy(recData, 1, data, 0, recData.length - 1);
        return data;
    }

    /*
     handles incoming messages from other device forwarded by MessageManager
      */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MessageManager.MC_INITHANDLER:
                Object obj = msg.obj;
                MessageManager messman = (MessageManager) obj;
                Log.d(LOGTAG, "initiating MessageManager");
                InetAddress ipAddress1 = messman.getSocket().getInetAddress();

                tempDevices.put(ipAddress1, messman);

                /*WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                String address = info.getMacAddress();*/
                String address = Helper.getWFDMacAddress();
                IpMacPacket ipMacPacket = new IpMacPacket(messman.getSocket().getLocalAddress(), address);
                Toast.makeText(getApplicationContext(), "MACaddress: " + address , Toast.LENGTH_LONG).show();

                byte[] ipMacSendPack = SerializationUtils.serialize(ipMacPacket);
                ByteBuffer sendByteBuffer = ByteBuffer.allocate(ipMacSendPack.length + 1);
                sendByteBuffer.put(MessageManager.MSGTYPE_SNDMAC);
                sendByteBuffer.put(ipMacSendPack);


                Toast.makeText(getApplicationContext(), "address: " + ipAddress1.getHostAddress() + " localaddress: " + messman.getSocket().getLocalAddress(), Toast.LENGTH_LONG).show();

                messageManagers.add(messman);
                setMessageManager(messman);

                messman.write(sendByteBuffer.array());

                break;
            case MessageManager.MC_MSGRECEIVED:
                byte[] readBuf = (byte[]) msg.obj;
                byte msg_type = readBuf[0];
                Log.d(LOGTAG, msg_type + "");
                switch (msg_type) {
                    case MessageManager.MSGTYPE_SNDMAC:
                        byte[] macData = getContent(msg.obj);
                        IpMacPacket ipMacPacket1 = SerializationUtils.deserialize(macData);
                        Toast.makeText(getApplicationContext(), "remotedevice\nipaddress: " + ipMacPacket1.getClientIp() + " macaddress: " + ipMacPacket1.getClientMacAddress(), Toast.LENGTH_LONG).show();

                        String macAddress = ipMacPacket1.getClientMacAddress();
                        InetAddress ipAddress = ipMacPacket1.getClientIp();

                        MyWifiP2pDevice tempDev= new MyWifiP2pDevice(discoveredDevices.get(macAddress),null, tempDevices.get(ipAddress), ipAddress);

                        discoveredDevicesNEW.put(macAddress, tempDev);


                        break;
                    /*case MessageManager.MSGTYPE_REQMAC:
                        byte[] ipMacData = getContent(msg.obj);

                        byte[] dataSend3 = address.getBytes();
                        ByteBuffer target3 = ByteBuffer.allocate(dataSend3.length + 1);
                        target3.put(MessageManager.MSGTYPE_SNDMAC);
                        target3.put(dataSend3);
                        break;*/
                    case MessageManager.MSGTYPE_ENCR:
                        byte[] recData2 = (byte[]) msg.obj;
                        byte[] data2 = new byte[recData2[1]];
                        System.arraycopy(recData2, 2, data2, 0, recData2[1]);
                        String encryptedMsg = new String(data2);
                        //Toast.makeText(getApplicationContext(), "testTEST " + encryptedMsg, Toast.LENGTH_LONG).show();
                        String password = String.valueOf(secureAuthentication.getGeneratedKeyVal());

                        String messageAfterDecrypt = "";
                        try {
                            messageAfterDecrypt = AESCrypt.decrypt(password, encryptedMsg);
                        } catch (GeneralSecurityException e) {
                            //handle error - could be due to incorrect password or tampered encryptedMsg
                        }
                        Toast.makeText(getApplicationContext(), "encrypted message:\n" + encryptedMsg + "\ndecrypted message:\n" + messageAfterDecrypt, Toast.LENGTH_LONG).show();
                        break;
                    case MessageManager.MSGTYPE_TEST:
                        Toast.makeText(getApplicationContext(), "fuck flowers and fuck your happiness", Toast.LENGTH_LONG).show();
                        break;
                    case MessageManager.MSGTYPE_HELLO:
                        if (messageManager != null) {
                            byte[] data = getContent(msg.obj);
                            UsdpPacket prettyData = (UsdpPacket) SerializationUtils.deserialize(data);
                            remoteDevice = prettyData;
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
                        }
                        break;
                    case MessageManager.MSGTYPE_HELLOBACK:
                        byte[] data = getContent(msg.obj);
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


                        // HERE (resRay)
                        Message authMechsMsg = Message.obtain(null, MSG_AUTHMECHS, res2);

                        sendMsgToActivity(authMechsMsg);

                        Log.d(LOGTAG, "chustcheckin " + prettyData.getMechsRec()[0]);
                        break;
                    case MessageManager.MSGTYPE_AUTH_DIALOG:
                        byte[] actualData = getContent(msg.obj);
                        OOBData prettyData2 = (OOBData) SerializationUtils.deserialize(actualData);
                        secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey());
                        prettyData2.setAuthdata(secureAuthentication.getHashedIntVal());
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

    public void deviceChanged(WifiP2pDevice device) {
        if (device != null) {
            thisDevice = device;
            Message retmsg = Message.obtain(null,
                    MSG_WIFIDEVICE_CHANGED, device);
            sendMsgToActivity(retmsg);
        }

    }

    public void disconnect() {
        if (wifiP2pManager != null && mChannel != null) {

            wifiP2pManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && wifiP2pManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        wifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(LOGTAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(LOGTAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * inner class, handles messages from @UsdpMainActivity
     */
    class InternalMsgIncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
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
                                    secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey());
                                    OOBData data = new OOBData(authMechStr, secureAuthentication.getHashedIntVal(), true);
                                    Message retmsg = Message.obtain(null,
                                            MSG_AUTHENTICATION_DIALOG_DATA, data);
                                    sendMsgToActivity(retmsg);
                                    //TODO inform other device to receive
                                    OOBData dataRemote = new OOBData(authMechStr, -1, false);
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
                                        secureAuthentication.generateKey(remoteDevice.getRemoteDevPublicKey());
                                        OOBData data = new OOBData(authMechStr, secureAuthentication.getHashedIntVal(), false);
                                        Message retmsg = Message.obtain(null,
                                                MSG_AUTHENTICATION_DIALOG_DATA, data);
                                        sendMsgToActivity(retmsg);
                                        //TODO inform other device to send
                                        OOBData dataRemote = new OOBData(authMechStr, -1, true);
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
                    Log.d(LOGTAG, "ACTSEND, Service says: client registered");
                    activityMessenger = msg.replyTo;
                    sendMsgToActivity(Message.obtain(null, MSG_WIFIDEVICE_CHANGED, thisDevice));
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(LOGTAG, "ACTSEND, Service says: client unregistered");
                    break;
                case MSG_CONFIG:
                    Log.d(LOGTAG, "Service started. Device config transmitted");
                    deviceCapabilities = (DeviceCapabilities) msg.obj;
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
                case MSG_DISCONNECT:
                    final String disConnDevAddr = msg.obj.toString();
                    WifiP2pDevice devc = discoveredDevices.get(disConnDevAddr);
                    if (devc != null) {
                        disconnect();
                    }
                    break;
                case MSG_ABRT_CONN:
                    final String cnclConnDevAddr = msg.obj.toString();
                    WifiP2pDevice devi = discoveredDevices.get(cnclConnDevAddr);
                    if (devi != null) {
                        wifiP2pManager.cancelConnect(mChannel, null);
                    }
                    break;
                case MSG_REQ_CONNINFO:
                    sendMsgToActivity(Message.obtain(null, MSG_CONNINFO, connInfo));
                    break;
                case MSG_ACCEPT_AUTH:
                    //TODO if failed, send failmsg to other device
                    AuthResult res = (AuthResult) msg.obj;
                    if (connInfo == null) {
                        connInfo = new ConnInfo();
                    }
                    connInfo.addAuthMech(res.getMech(), res.isSuccess());
                    if (res.isSuccess()) {
                        Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Pairing failed!", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case MSG_ACCEPT_AUTH_WDATA:
                    AuthResult resWData = (AuthResult) msg.obj;
                    if (connInfo == null) {
                        connInfo = new ConnInfo();
                    }
                    final String data = resWData.getData();
                    String val = String.valueOf(secureAuthentication.getHashedIntVal());
                    if (val.equals(data)) {
                        connInfo.addAuthMech(resWData.getMech(), true);
                        Toast.makeText(getApplicationContext(), "Accepting data: " + val, Toast.LENGTH_SHORT).show();
                    } else {
                        connInfo.addAuthMech(resWData.getMech(), false);
                        Toast.makeText(getApplicationContext(), "Pairing failed!", Toast.LENGTH_SHORT).show();
                    }
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


                                    byte[] data2 = SerializationUtils.serialize(new UsdpPacket(uniqueID, "protVersion1.0", publicKey));
                                    ByteBuffer target = ByteBuffer.allocate(data2.length + 1);
                                    target.put(MessageManager.MSGTYPE_HELLO);
                                    target.put(data2);
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
                case MSG_SEND_ENCRYPTED:
                   /* if (messageManager != null) {
                        String text = (String) msg.obj;
                        String key = String.valueOf(secureAuthentication.getGeneratedKeyVal());

                        String encryptedMsg = "";
                        try {
                            encryptedMsg = AESCrypt.encrypt(key, text);
                        } catch (GeneralSecurityException e) {
                            //handle error
                        }

                        byte[] encrData = encryptedMsg.getBytes();
                        //byte[] encrData = text.getBytes();
                        ByteBuffer target = ByteBuffer.allocate(encrData.length + 2);
                        target.put(MessageManager.MSGTYPE_ENCR);
                        // encrypted messages have a max length of 256

                        target.put((byte) encrData.length);
                        target.put(encrData);
                        messageManager.write(target.array());


                    }*/

                    if (!messageManagers.isEmpty()) {
                        MessageManager man = messageManagers.get(x++ % messageManagers.size());

                        Toast.makeText(getApplicationContext(), messageManagers.size() + " mesmans", Toast.LENGTH_LONG).show();

                        byte[] data2 = "teststring".getBytes();

                        ByteBuffer target2 = ByteBuffer.allocate(data2.length + 1);
                        target2.put(MessageManager.MSGTYPE_TEST);
                        target2.put(data2);
                        man.write(target2.array());
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
