package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.primarychannel.wifip2p.WiFiDirectBroadcastReceiver;

public class UsdpMainActivity extends AppCompatActivity {

    private final String LOGTAG = "UsdpMainActivity";

    //UI
    private ListView lv_discoveredDevices;
    private ArrayAdapter la_discoveredDevices;
    // List discoveredDevices = new ArrayList<String>();
    private HashMap<String, WifiP2pDevice> discoveredDevicesComplete = new HashMap<String, WifiP2pDevice>();
    private String[] deviceNames;
    private List<String> valueList = new ArrayList<String>();
    private ToggleButton btn_toggleSvc;
    private WifiP2pManager wifiP2pManager;
    private WiFiDirectBroadcastReceiver mReceiver;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;

    // Service connection
    private Messenger mService = null;
    private Intent bindServiceIntent;
    private boolean mBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());


    // handles messages from UsdpService
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsdpService.MSG_SAY_HELLO:
                    Toast.makeText(UsdpMainActivity.this, "service said hello!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usdp_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // UI
        Button btn_discover = (Button) findViewById(R.id.btn_discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(LOGTAG, "discoverSuccess");
                    }

                    @Override
                    public void onFailure(int reasonCode) {

                    }
                });
            }
        });

        btn_toggleSvc = (ToggleButton) findViewById(R.id.btn_toggleSvc);
        Intent in = new Intent(this, UsdpService.class);
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


        deviceNames = valueList.toArray(new String[0]);
        la_discoveredDevices = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, valueList);
        //la_discoveredDevices = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, valueList);
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
                WifiP2pDevice device = discoveredDevicesComplete.get(lv_discoveredDevices.getAdapter().getItem(position));
                if (device != null) {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    wifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Toast.makeText(UsdpMainActivity.this, "success", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(UsdpMainActivity.this, "failure" + reason, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });


        // TODO request other device to remove group
        lv_discoveredDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                wifiP2pManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        Log.d(LOGTAG, group.getNetworkName() + "pass: " + group.getPassphrase());
                    }
                });

                wifiP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(LOGTAG, "Disconnected from device");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(LOGTAG, "Couldn't disconnect from device");
                    }
                });
                wifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(LOGTAG, "Group removed");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(LOGTAG, "Couldn't remove group");
                    }
                });


                return true;
            }
        });


        // logic
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void sayHello(View v) {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, UsdpService.MSG_SAY_HELLO, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void someoneConnects() {
        Toast.makeText(UsdpMainActivity.this, "someoneConnects", Toast.LENGTH_SHORT).show();
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

        valueList.clear();
        valueList.addAll(discoveredDevicesComplete.keySet());
        //addDeviceNames();
        la_discoveredDevices.notifyDataSetChanged();


    }

    private void addDeviceNames() {
        ListIterator<String> item = valueList.listIterator();
        while (item.hasNext()) {
            String dev = item.next();
            item.set(discoveredDevicesComplete.get(dev).deviceName + " (" + dev + ")");
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

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindServiceIntent = new Intent(this, UsdpService.class);

    }

}
