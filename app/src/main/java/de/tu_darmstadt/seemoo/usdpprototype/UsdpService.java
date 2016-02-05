package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

//wifip2p
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kenny on 29.01.16.
 */
public class UsdpService extends Service {


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
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    //WifiP2p fields
    private WiFiDirectBroadcastReceiver mReceiver;
    private HashMap<String, WifiP2pDevice> discoveredDevicesComplete = new HashMap<String, WifiP2pDevice>();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private Handler handler;
    private Messenger activityMessenger;
    private String LOGTAG = "UsdpService";

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


        // logic
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // TODO register was in 'onResume' of Activity. No such method in Service, is it OK in onCreate?
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

    public void someoneConnects() {
        Log.d(LOGTAG, "someone connects");
    }

    private void sendMsgToActivity(Message msg) {
        try {
            activityMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler of incoming messages from clients.
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
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
