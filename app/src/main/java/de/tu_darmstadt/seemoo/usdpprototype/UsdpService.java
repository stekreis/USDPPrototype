package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

//wifip2p
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ChatManager;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ClientSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.GroupOwnerSocketHandler;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.ServerSocket;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WfP2pServerSocket;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
    public static final int CHAT_MY_HANDLE = 9;
    public static final int CHAT_MESSAGE_READ = 10;
    public static final int MSG_CHATMSGRECEIVED = 11;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    ServerSocket serverSocket;
    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, WifiP2pDevice> discoveredDevicesComplete = new HashMap<String, WifiP2pDevice>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private Handler handler = new Handler(this);
    private Messenger activityMessenger;
    private String LOGTAG = "UsdpService";

    private ChatManager chatManager;

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
    }

    public void peersAvailable(WifiP2pDeviceList peers) {
        WifiP2pDevice device = null;
        Log.d(LOGTAG, "list incoming");

        for (WifiP2pDevice peer : peers.getDeviceList()) {
            device = peer;
            String deviceaddr = device.deviceAddress;
            discoveredDevicesComplete.put(deviceaddr, device);
            Log.d(LOGTAG, deviceaddr + " found");
        }
        if (activityMessenger != null) {
            ArrayList<String> deviceMacs = new ArrayList<String>(discoveredDevicesComplete.keySet());
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
         * GroupOwnerSocketHandler}
         */

        if (p2pInfo.isGroupOwner) {
            Log.d(LOGTAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CHAT_MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(LOGTAG, readMessage);
                sendMsgToActivity(Message.obtain(null, MSG_CHATMSGRECEIVED, readMessage));
                //(chatFragment).pushMessage("Buddy: " + readMessage);
                break;

            case CHAT_MY_HANDLE:
                Object obj = msg.obj;
                Log.d(LOGTAG, "testchat");
                setChatManager((ChatManager) obj);

        }
        return true;
    }

    public void setChatManager(ChatManager obj) {
        chatManager = obj;
    }

    /**
     * inner class, handles messages from @UsdpMainActivity
     *
     */
    class IncomingHandler extends Handler {
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
                    WifiP2pDevice device = discoveredDevicesComplete.get(msg.obj.toString());
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
                    if (chatManager != null) {
                        chatManager.write("teststring".getBytes());
                        //pushMessage("Me: " + chatLine.getText().toString());
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
