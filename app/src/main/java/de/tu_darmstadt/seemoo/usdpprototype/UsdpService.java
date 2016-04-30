package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
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
import java.util.HashMap;
import java.util.HashSet;

import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechManager;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthResult;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.SecureAuthentication;
import de.tu_darmstadt.seemoo.usdpprototype.misc.DeviceCapabilities;
import de.tu_darmstadt.seemoo.usdpprototype.misc.Helper;
import de.tu_darmstadt.seemoo.usdpprototype.misc.TargetMsg;
import de.tu_darmstadt.seemoo.usdpprototype.misc.IpMacPacket;
import de.tu_darmstadt.seemoo.usdpprototype.misc.ListDevice;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ClientSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.MessageManager;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ServerSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;
import de.tu_darmstadt.seemoo.usdpprototype.misc.OOBData;
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
    public static final int MSG_PAIRSEC = 31;

    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());
    private final String LOGTAG = "UsdpService";

    private SecureAuthentication secureAuthentication = null;
    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, RemoteDevice> remoteDevices = new HashMap<>();
    private HashMap<String, InetAddress> tempIpList = new HashMap<>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Handler handler = new Handler(this);
    private Messenger activityMessenger;
    private AuthMechManager authMechManager;
    private DeviceCapabilities deviceCapabilities;
    private HashMap<InetAddress, MessageManager> tempDevices = new HashMap<>();

    private InetAddress goAddress;

    private WifiP2pDevice thisDevice;
    private RemoteDevice thisMyDevice;
    private String thisDeviceAddress;

    private String uniqueID;

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

        initSecAuth();

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
            // create RemoteDevice ("extended" WifiP2pDevice) TODO maybe extend as subclass
            if (remoteDevices.containsKey(deviceaddr)) {
                RemoteDevice tempDev = remoteDevices.get(deviceaddr);
                if (tempDev.getInetAddress() == null) {
                    InetAddress ipAddr = tempIpList.get(deviceaddr);
                    tempDev.setInetAddress(ipAddr);
                }
                remoteDevices.put(deviceaddr, tempDev);
            } else {
                RemoteDevice tempDev = new RemoteDevice(device);
                InetAddress ipAddr = tempIpList.get(deviceaddr);
                tempDev.setInetAddress(ipAddr);
                tempDev.setMessMan(tempDevices.get(ipAddr));
                remoteDevices.put(deviceaddr, tempDev);
            }
            listdevices.add(new ListDevice(deviceaddr, device.deviceName, device.status, device.isGroupOwner()));
            Log.d(LOGTAG, deviceaddr + " found");
        }
        if (activityMessenger != null) {
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
            Log.d(LOGTAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    ((UsdpMainActivity.MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }
    }

    private byte[] getContent(Object obj) {
        byte[] recData = (byte[]) obj;
        byte[] data = new byte[recData.length - 1];
        System.arraycopy(recData, 1, data, 0, recData.length - 1);
        return data;
    }

    private void initSecAuth() {
        secureAuthentication = new SecureAuthentication();
        secureAuthentication.init();
    }

    /*
     handles incoming messages from other device forwarded by MessageManager
      */
    @Override
    public boolean handleMessage(Message msg) {
        Object obj = msg.obj;
        switch (msg.what) {
            case MessageManager.MC_INITHANDLER:
                // initialization of MessageManagers
                MessageManager messman = (MessageManager) obj;
                Log.d(LOGTAG, "initiating MessageManager");
                InetAddress ipAddress1 = messman.getSocket().getInetAddress();

                tempDevices.put(ipAddress1, messman);

                String address = Helper.getWFDMacAddress();
                thisDeviceAddress = address;
                IpMacPacket ipMacPacket = new IpMacPacket(messman.getSocket().getLocalAddress(), address);

                byte[] ipMacSendPack = SerializationUtils.serialize(new TargetMsg(thisDeviceAddress, ipMacPacket));
                ByteBuffer sendByteBuffer = ByteBuffer.allocate(ipMacSendPack.length + 1);
                sendByteBuffer.put(MessageManager.MSGTYPE_SNDMAC);
                sendByteBuffer.put(ipMacSendPack);
                messman.write(sendByteBuffer.array());

                break;
            case MessageManager.MC_MSGRECEIVED:
                byte[] readBuf = (byte[]) obj;
                byte msg_type = readBuf[0];
                byte[] content = getContent(obj);
                TargetMsg tMsg = SerializationUtils.deserialize(content);
                Log.d(LOGTAG, msg_type + "");
                switch (msg_type) {
                    case MessageManager.MSGTYPE_SNDMAC:
                        IpMacPacket ipMacPacket1 = (IpMacPacket) tMsg.getObj();

                        String macAddress = ipMacPacket1.getClientMacAddress();
                        InetAddress ipAddress = ipMacPacket1.getClientIp();

                        RemoteDevice tempDev = remoteDevices.get(macAddress);
                        if (tempDev == null) {
                            tempIpList.put(macAddress, ipAddress);
                        } else {
                            tempDev.setInetAddress(ipAddress);
                            tempDev.setMessMan(tempDevices.get(ipAddress));
                            remoteDevices.put(macAddress, tempDev);
                        }


                        break;
                    case MessageManager.MSGTYPE_ENCR:
                        String targetAddr = tMsg.getTargetAddress();
                        if (targetAddr.equals(thisDeviceAddress)) {
                            String encryptedMsgStr = (String) tMsg.getObj();
                            RemoteDevice remoteDevice = remoteDevices.get(tMsg.getSenderAddress());
                            String key = String.valueOf(remoteDevice.getSymKey());

                            String messageAfterDecrypt = "";
                            try {
                                messageAfterDecrypt = AESCrypt.decrypt(key, encryptedMsgStr);
                            } catch (GeneralSecurityException e) {
                                //handle error - could be due to incorrect password or tampered encryptedMsg
                            }
                            Toast.makeText(getApplicationContext(), "encrypted message:\n" + encryptedMsgStr + "\ndecrypted message:\n" + messageAfterDecrypt, Toast.LENGTH_LONG).show();
                        } else {
                            //this is NOT the target device. forward the packet
                            RemoteDevice targetDevice = remoteDevices.get(tMsg.getTargetAddress());
                            if (targetDevice != null) {
                                MessageManager man = targetDevice.getMessMan();
                                byte[] data5 = SerializationUtils.serialize(tMsg);
                                ByteBuffer target = ByteBuffer.allocate(data5.length + 1);
                                target.put(MessageManager.MSGTYPE_ENCR);
                                target.put(data5);
                                man.write(target.array());
                            }
                        }
                        break;
                    case MessageManager.MSGTYPE_TEST:
                        String contentStr = (String) tMsg.getObj();
                        Toast.makeText(getApplicationContext(), "testmessage " + contentStr, Toast.LENGTH_LONG).show();
                        break;
                    case MessageManager.MSGTYPE_HELLO:
                        // pairing initiated by other device. check if this is the target device
                        UsdpPacket helloPacket = (UsdpPacket) tMsg.getObj();
                        if (tMsg.getTargetAddress().equals(thisDeviceAddress)) {
                            // this is the target device
                            String senderAddress = tMsg.getSenderAddress();
                            RemoteDevice myDiscDev = remoteDevices.get(senderAddress);
                            if (myDiscDev != null) {
                                MessageManager currMessMan = myDiscDev.getMessMan();
                                if (currMessMan == null) {
                                    currMessMan = getGroupOwnerMessMan();
                                }
                                if (currMessMan != null) {
                                    myDiscDev.setPacket(helloPacket);

                                    int publicKey = secureAuthentication.getPublicDeviceKey();

                                    String[] supRecMechs = authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities());
                                    String[] supSendMechs = authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities());
                                    byte[] dataSend = SerializationUtils.serialize(new TargetMsg(thisDeviceAddress, senderAddress, new UsdpPacket(supRecMechs, supSendMechs, publicKey)));
                                    ByteBuffer target = ByteBuffer.allocate(dataSend.length + 1);
                                    target.put(MessageManager.MSGTYPE_HELLOBACK);
                                    target.put(dataSend);
                                    currMessMan.write(target.array());
                                }
                            }
                        } else {
                            //this is NOT the target device. forward the packet
                            RemoteDevice tgtDev = remoteDevices.get(tMsg.getTargetAddress());
                            if (tgtDev != null) {
                                MessageManager actualTargetMessMan = tgtDev.getMessMan();
                                if (actualTargetMessMan != null) {
                                    byte[] data5 = SerializationUtils.serialize(tMsg);
                                    ByteBuffer target = ByteBuffer.allocate(data5.length + 1);
                                    target.put(MessageManager.MSGTYPE_HELLO);
                                    target.put(data5);
                                    actualTargetMessMan.write(target.array());
                                }
                            }
                        }
                        break;
                    case MessageManager.MSGTYPE_HELLOBACK:

                        UsdpPacket hellobackPacket = (UsdpPacket) tMsg.getObj();
                        if (tMsg.getTargetAddress().equals(thisDeviceAddress)) {
                            String sdrAddress = tMsg.getSenderAddress();
                            RemoteDevice dev = remoteDevices.get(sdrAddress);
                            if (dev != null) {
                                dev.setPacket(hellobackPacket);
                            }

                            HashSet<String> res = new HashSet<>();

                            authMechManager.findComp(res, hellobackPacket.getMechsSend(), authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities()));
                            authMechManager.findComp(res, hellobackPacket.getMechsRec(), authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities()));

                            String[] resRay = res.toArray(new String[res.size()]);

                            AuthMechanism[] res2 = authMechManager.sortAuthMechsBySec(resRay);


                            for (int pos = 0; pos < resRay.length; pos++) {
                                resRay[pos] = res2[pos].getShortName(); //  + " (" + res2[pos].getSecPoints() + ")";
                            }

                            // HERE (resRay)
                            Message authMechsMsg = Message.obtain(null, MSG_AUTHMECHS, new TargetMsg(sdrAddress, thisDeviceAddress, res2));

                            sendMsgToActivity(authMechsMsg);

                            Log.d(LOGTAG, "chustcheckin " + hellobackPacket.getMechsRec()[0]);
                        } else {
                            //this is NOT the target device. forward the packet
                            RemoteDevice tgtDev = remoteDevices.get(tMsg.getTargetAddress());
                            if (tgtDev != null) {
                                MessageManager actualTargetMessMan = tgtDev.getMessMan();
                                if (actualTargetMessMan != null) {
                                    byte[] data5 = SerializationUtils.serialize(tMsg);
                                    ByteBuffer target = ByteBuffer.allocate(data5.length + 1);
                                    target.put(MessageManager.MSGTYPE_HELLOBACK);
                                    target.put(data5);
                                    actualTargetMessMan.write(target.array());
                                }
                            }
                        }
                        break;
                    case MessageManager.MSGTYPE_AUTH_DIALOG:
                        Log.d(LOGTAG, "target:" + tMsg.getTargetAddress() + " sender:" + tMsg.getSenderAddress());
                        if (tMsg.getTargetAddress().equals(thisDeviceAddress)) {
                            OOBData oobData = (OOBData) tMsg.getObj();
                            Log.d(LOGTAG, "target:" + tMsg.getTargetAddress() + " sender:" + tMsg.getSenderAddress());
                            RemoteDevice tempoDev = remoteDevices.get(tMsg.getSenderAddress());
                            int genKey = secureAuthentication.generateKey(tempoDev.getPacket().getRemoteDevPublicKey());
                            tempoDev.setSymKey(genKey);
                            oobData.setAuthdata(SecureAuthentication.getHashedIntVal(genKey));
                            //this device is now the "sender" for this TargetMsg
                            TargetMsg newMsg = new TargetMsg(tMsg.getTargetAddress(), tMsg.getSenderAddress(), tMsg.getObj());
                            Message retmsg = Message.obtain(null,
                                    MSG_AUTHENTICATION_DIALOG_DATA, newMsg);
                            sendMsgToActivity(retmsg);
                        } else {
                            // forward msg
                            RemoteDevice tgtDev = remoteDevices.get(tMsg.getTargetAddress());
                            if (tgtDev != null) {
                                MessageManager man = tgtDev.getMessMan();
                                if (man != null) {
                                    man.write((byte[]) obj);
                                }
                            }
                        }

                    default:
                        /*Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) obj, 0, msg.arg1));
                        Log.d(LOGTAG, "missing/wrong MSGTYPE: " + new String((byte[]) obj, 1, msg.arg1));*/
                }
                break;
            default:
        }

        return true;
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

    private void pairSecure(RemoteDevice myDev) {
        MessageManager curMessMan = myDev.getMessMan();
        if (curMessMan == null) {
            curMessMan = getGroupOwnerMessMan();
        }
        if (curMessMan != null) {
            int publicKey = secureAuthentication.getPublicDeviceKey();

            byte[] data2 = SerializationUtils.serialize(new TargetMsg(thisDeviceAddress, myDev.getDevice().deviceAddress, new UsdpPacket(uniqueID, "protVersion1.0", publicKey)));
            ByteBuffer target = ByteBuffer.allocate(data2.length + 1);
            target.put(MessageManager.MSGTYPE_HELLO);
            target.put(data2);
            curMessMan.write(target.array());
            Toast.makeText(getApplicationContext(), "pairing in progress...", Toast.LENGTH_SHORT).show();
        } else {
            // no messageManagers -> no devices connected
        }

    }

    private MessageManager getGroupOwnerMessMan() {
        ArrayList<RemoteDevice> remDevices = new ArrayList<>();
        remDevices.addAll(remoteDevices.values());
        int x = 0;
        for (RemoteDevice remDev : remDevices) {
            x++;
            Toast.makeText(getApplicationContext(), remDev.getDevice().deviceAddress + " found", Toast.LENGTH_SHORT).show();
            if (remDev.getDevice().isGroupOwner()) {

                return remDev.getMessMan();
            }
        }

        Toast.makeText(getApplicationContext(), "no GO found! (" + x + " devices found)", Toast.LENGTH_SHORT).show();
        return null;
    }

    /**
     * inner class, handles messages from @UsdpMainActivity
     */
    class InternalMsgIncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            Object obj = msg.obj;
            switch (msg.what) {
                case MSG_UNPAIR:
                    //TODO: unpair
                    break;
                case MSG_CHOSEN_AUTHMECH:
                    TargetMsg chAuthMchMsg = (TargetMsg) msg.obj;

                    String[] supSendMechs = authMechManager.getSupportedSendMechs(deviceCapabilities.getValidCapabilities());
                    boolean matches = false;

                    String authMechStr = (String) chAuthMchMsg.getObj();
                    RemoteDevice targetDev = remoteDevices.get(chAuthMchMsg.getTargetAddress());
                    for (int pos = 0; pos < supSendMechs.length && !matches; pos++) {
                        if (supSendMechs[pos].equals(authMechStr)) {
                            if (targetDev != null) {
                                String[] supRemoteRecMechs = targetDev.getPacket().getMechsRec();
                                for (int rpos = 0; rpos < supRemoteRecMechs.length && !matches; rpos++) {
                                    if (supRemoteRecMechs[rpos].equals(authMechStr)) {
                                        matches = true;
                                        Log.d(LOGTAG, "found match thissend");
                                        //this device sends, remote device receives
                                        int genKey = secureAuthentication.generateKey(targetDev.getPacket().getRemoteDevPublicKey());
                                        targetDev.setSymKey(genKey);
                                        OOBData data = new OOBData(authMechStr, SecureAuthentication.getHashedIntVal(genKey), true);
                                        chAuthMchMsg.setObj(data);
                                        Message retmsg = Message.obtain(null,
                                                MSG_AUTHENTICATION_DIALOG_DATA, chAuthMchMsg);
                                        sendMsgToActivity(retmsg);
                                        //TODO inform other device to receive
                                        OOBData dataRemote = new OOBData(authMechStr, -1, false);
                                        MessageManager messManChAuthMch = targetDev.getMessMan();
                                        if (messManChAuthMch == null) {
                                            messManChAuthMch = getGroupOwnerMessMan();
                                        }
                                        if (messManChAuthMch != null) {
                                            TargetMsg chAuthMchRetMsg = new TargetMsg(chAuthMchMsg.getSenderAddress(), chAuthMchMsg.getTargetAddress(), dataRemote);
                                            byte[] dataRem = SerializationUtils.serialize(chAuthMchRetMsg);
                                            ByteBuffer target = ByteBuffer.allocate(dataRem.length + 1);
                                            target.put(MessageManager.MSGTYPE_AUTH_DIALOG);
                                            target.put(dataRem);
                                            messManChAuthMch.write(target.array());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!matches) {
                        String[] supRecMechs = authMechManager.getSupportedRecMechs(deviceCapabilities.getValidCapabilities());
                        for (int pos = 0; pos < supRecMechs.length && !matches; pos++) {
                            if (supRecMechs[pos].equals(authMechStr)) {
                                String[] supRemoteSendMechs = remoteDevices.get(chAuthMchMsg.getTargetAddress()).getPacket().getMechsSend();
                                for (int rpos = 0; rpos < supRemoteSendMechs.length && !matches; rpos++) {
                                    if (supRemoteSendMechs[rpos].equals(authMechStr)) {
                                        matches = true;
                                        Log.d(LOGTAG, "found match thisreceive");
                                        //remote device sends, this device receives
                                        RemoteDevice sndDevice = remoteDevices.get(chAuthMchMsg.getTargetAddress());
                                        int genKey = secureAuthentication.generateKey(sndDevice.getPacket().getRemoteDevPublicKey());
                                        sndDevice.setSymKey(genKey);
                                        OOBData data = new OOBData(authMechStr, SecureAuthentication.getHashedIntVal(genKey), false);
                                        chAuthMchMsg.setObj(data);
                                        Message retmsg = Message.obtain(null,
                                                MSG_AUTHENTICATION_DIALOG_DATA, chAuthMchMsg);
                                        sendMsgToActivity(retmsg);
                                        //TODO inform other device to send
                                        OOBData dataRemote = new OOBData(authMechStr, -1, true);
                                        MessageManager messManChAuthMch = targetDev.getMessMan();
                                        if (messManChAuthMch != null) {
                                            TargetMsg chAuthMchRetMsg = new TargetMsg(chAuthMchMsg.getSenderAddress(), chAuthMchMsg.getTargetAddress(), dataRemote);
                                            byte[] dataRem = SerializationUtils.serialize(chAuthMchRetMsg);
                                            ByteBuffer target = ByteBuffer.allocate(dataRem.length + 1);
                                            target.put(MessageManager.MSGTYPE_AUTH_DIALOG);
                                            target.put(dataRem);
                                            messManChAuthMch.write(target.array());
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
                    deviceCapabilities = (DeviceCapabilities) obj;
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
                    final String disConnDevAddr = (String) obj;
                    WifiP2pDevice devc = remoteDevices.get(disConnDevAddr).getDevice();
                    if (devc != null) {
                        disconnect();
                    }
                    break;
                case MSG_ABRT_CONN:
                    final String cnclConnDevAddr = (String) obj;
                    WifiP2pDevice devi = remoteDevices.get(cnclConnDevAddr).getDevice();
                    if (devi != null) {
                        wifiP2pManager.cancelConnect(mChannel, null);
                    }
                    break;
                case MSG_REQ_CONNINFO:
                    final String tgtDeviceAddr = (String) obj;
                    sendMsgToActivity(Message.obtain(null, MSG_CONNINFO, remoteDevices.get(tgtDeviceAddr).getConnInfo()));
                    break;
                case MSG_ACCEPT_AUTH:
                    //TODO if failed, send failmsg to other device
                    TargetMsg authMsg = (TargetMsg) obj;
                    AuthResult res = (AuthResult) authMsg.getObj();
                    String address = authMsg.getTargetAddress();
                    if (thisDeviceAddress.equals(address)) {
                        address = authMsg.getSenderAddress();
                    }
                    RemoteDevice ledev = remoteDevices.get(address);
                    if (ledev != null) {
                        ConnInfo connInfo = ledev.getConnInfo();
                        if (connInfo == null) {
                            connInfo = new ConnInfo();
                            ledev.setConnInfo(connInfo);
                        }
                        connInfo.addAuthMech(res.getMech(), res.isSuccess());
                    }
                    if (res.isSuccess()) {
                        Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Pairing failed! nodata", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case MSG_ACCEPT_AUTH_WDATA:
                    TargetMsg authMsgwd = (TargetMsg) obj;
                    AuthResult resWData = (AuthResult) authMsgwd.getObj();
                    String addressWdata = authMsgwd.getTargetAddress();
                    if (thisDeviceAddress.equals(addressWdata)) {
                        addressWdata = authMsgwd.getSenderAddress();
                    }
                    RemoteDevice mymyDev = remoteDevices.get(addressWdata);
                    if (mymyDev != null) {
                        ConnInfo connInfoWData = mymyDev.getConnInfo();
                        if (connInfoWData == null) {
                            connInfoWData = new ConnInfo();
                            mymyDev.setConnInfo(connInfoWData);
                        }

                        final String data = resWData.getData();

                        String val = String.valueOf(SecureAuthentication.getHashedIntVal(mymyDev.getSymKey()));
                        Log.d("pairing rec", data);
                        if (val.equals(data)) {
                            connInfoWData.addAuthMech(resWData.getMech(), true);
                            Toast.makeText(getApplicationContext(), "Accepting data: " + val, Toast.LENGTH_SHORT).show();
                        } else {
                            connInfoWData.addAuthMech(resWData.getMech(), false);
                            Toast.makeText(getApplicationContext(), "Pairing failed! " + data, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case MSG_CONNECT:
                    final String connDevAddr = obj.toString();
                    RemoteDevice myDev = remoteDevices.get(connDevAddr);
                    WifiP2pDevice dev = myDev.getDevice();
                    if (dev != null) {
                        switch (dev.status) {
                            case WifiP2pDevice.AVAILABLE:
                                // if available, just connect
                                // if device already in group, no direct connection possible. instead, pair.
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
                                pairSecure(myDev);
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
                case MSG_PAIRSEC:
                    final String pairAddr = obj.toString();
                    RemoteDevice myPairDev = remoteDevices.get(pairAddr);
                    pairSecure(myPairDev);
                    break;
                case MSG_SEND_ENCRYPTED:
                    TargetMsg iChatMsg = (TargetMsg) msg.obj;
                    String text = (String) iChatMsg.getObj();
                    RemoteDevice remoteDevice = remoteDevices.get(iChatMsg.getTargetAddress());
                    if (remoteDevice != null) {
                        MessageManager messageManager = remoteDevice.getMessMan();
                        if (messageManager != null) {
                            int key = remoteDevice.getSymKey();
                            String encryptedMsg = Helper.encrypt(text, String.valueOf(key));
                            iChatMsg.setObj(encryptedMsg);
                            iChatMsg.setSenderAddress(thisDeviceAddress);
                            byte[] encrData = SerializationUtils.serialize(iChatMsg);
                            ByteBuffer target = ByteBuffer.allocate(encrData.length + 1);
                            target.put(MessageManager.MSGTYPE_ENCR);
                            target.put(encrData);
                            messageManager.write(target.array());
                        } else {
                            // TODO if not in discovered devices but part of group, send to group owner. but: if not in remoteDevices, there is no encryption ?!

                            MessageManager man = getGroupOwnerMessMan();
                            if (man != null) {
                                int key = remoteDevice.getSymKey();
                                String encryptedMsg = Helper.encrypt(text, String.valueOf(key));
                                iChatMsg.setObj(encryptedMsg);
                                iChatMsg.setSenderAddress(thisDeviceAddress);
                                byte[] encrData = SerializationUtils.serialize(iChatMsg);
                                ByteBuffer target = ByteBuffer.allocate(encrData.length + 1);
                                target.put(MessageManager.MSGTYPE_ENCR);
                                target.put(encrData);
                                man.write(target.array());
                            }


                        }
                    }

/*                   //TODO use if not paired
                    MessageManager messageManager1 = remoteDevices.get(targetDevice).getMessMan();
                    if (messageManager1 != null) {
                        iChatMsg.setSenderAddress(thisDeviceAddress);
                        byte[] data2 = SerializationUtils.serialize(iChatMsg);
                        ByteBuffer target2 = ByteBuffer.allocate(data2.length + 1);
                        target2.put(MessageManager.MSGTYPE_TEST);
                        target2.put(data2);
                        messageManager1.write(target2.array());
                    }*/

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
