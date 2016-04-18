package de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 26.01.16.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private final String LOGTAG = "WiFiDirectBroadcastRcv";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private UsdpService mService;

    private WifiP2pManager.PeerListListener mPeerListListener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       UsdpService service) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mService = service;
        // init();
    }

    private void init() {
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                mService.peersAvailable(peers);
                Log.d(LOGTAG, "peersAvlbl");
            }
        };
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
//                mActivity.
            } else {
                // Wi-Fi P2P is not enabled
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(LOGTAG, "peersChanged");
            // Call WifiP2pManager.requestPeers() to get a list of current peers

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        mService.peersAvailable(peers);
                        Log.d(LOGTAG, "peersAvlbl");
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(LOGTAG,
                        "Connected to p2p network. Requesting network details");
                mManager.requestConnectionInfo(mChannel,
                        (WifiP2pManager.ConnectionInfoListener) mService);
            } else {
                // It's a disconnect
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if (mService != null) {
                mService.deviceChanged(device);
            }
        }

    }
}


