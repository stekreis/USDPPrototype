package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;
import de.tu_darmstadt.seemoo.usdpprototype.view.identicons.Identicon;

public class UsdpMainActivity extends AppCompatActivity {

    private static final String LOGTAG = "UsdpMainActivity";
    private final Messenger mMessenger = new Messenger(new InternalMsgIncomingHandler());

    //UI
    private ListView lv_discoveredDevices;
    private ArrayAdapter la_discoveredDevices;
    private ArrayList<String> valueList = new ArrayList<>();

    private EditText et_authtext;

    private AuthBarcodeDialogFragment authDialog;
    private blSiBDialogFragment authblsibDialog;

    // Service connection
    private Messenger mService = null;
    private Intent bindServiceIntent;
    private boolean mBound;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usdp_main);
        initViewComponents();
    }

    // TODO move to own class? with other overall usable methods
    public static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void showAuthBarcodeDialogFragment(Bitmap bmp) {
        Bundle bundle = new Bundle();
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        bundle.putIntArray(AuthBarcodeDialogFragment.BARCODE_CODE, pixels);
        bundle.putInt(AuthBarcodeDialogFragment.BARCODE_HEIGHT, bmp.getHeight());
        bundle.putInt(AuthBarcodeDialogFragment.BARCODE_WIDTH, bmp.getWidth());
        if (!authDialog.isFragmentUIActive()) {
            authDialog.setArguments(bundle);
            authDialog.show(getSupportFragmentManager(), "auth");
        }
    }

    private void initViewComponents() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_SENDCHATMSG, et_authtext.getText().toString()));
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

        Button btn_pair = (Button) findViewById(R.id.btn_pair);
        btn_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_PAIR));
            }
        });

        final Identicon identicon = (Identicon) findViewById(R.id.identicon);
        identicon.show("john.doe@example.orw");


        ImageButton btn_auth = (ImageButton) findViewById(R.id.btn_auth);
        btn_auth.setImageBitmap(generateQR("jet fuel"));

        btn_auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO uncomment
                //showAuthBarcodeDialogFragment(generateQR("jetfuelmeltstealbeams!"));

                // currently not supported(anim error as iv_blsib is not accessible
                //authblsibDialog.show(getSupportFragmentManager(), "blsib");
                //authblsibDialog.anim();




                identicon.show(et_authtext.getText().toString());


                Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(100);
            }
        });






        authDialog = new AuthBarcodeDialogFragment();
        authblsibDialog = new blSiBDialogFragment();

        et_authtext = (EditText) findViewById(R.id.et_text);

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

        valueList.add("Discovered devices");

        la_discoveredDevices = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, valueList);
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
                String deviceMac = lv_discoveredDevices.getAdapter().getItem(position).toString();
                Log.d(LOGTAG, "DEVICEMAC: " + deviceMac);
                Message msg = Message.obtain(null, UsdpService.MSG_CONNECT, deviceMac);
                sendMsgtoService(msg);
            }
        });


        // TODO request other device to remove group
        lv_discoveredDevices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceMac = lv_discoveredDevices.getAdapter().getItem(position).toString();
                sendMsgtoService(Message.obtain(null, UsdpService.MSG_PAIR, deviceMac));

                return true;
            }
        });
    }

    /**
     * Send a prepared message to the service, method takes care of error handling
     *
     * @param msg message to send
     */
    private void sendMsgtoService(Message msg) {
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
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

    @Override
    protected void onResume() {
        super.onResume();
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

    private Bitmap generateQR(String input) {
        int width = 200;
        int height = 200;
        BitMatrix qrMatrix;
        QRCodeWriter writer = new QRCodeWriter();
        Bitmap mBitmap = null;
        try {
            qrMatrix = writer.encode(input, BarcodeFormat.QR_CODE, width, height);
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                Toast.makeText(UsdpMainActivity.this, contents, Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
                Toast.makeText(UsdpMainActivity.this, "intent was cancelled", Toast.LENGTH_SHORT).show();
            }
        }
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
                case UsdpService.MSG_SAY_HELLO:
                    Toast.makeText(UsdpMainActivity.this, "service said hello!", Toast.LENGTH_SHORT).show();
                    break;
                case UsdpService.MSG_PEERSDISCOVERED:
                    valueList.clear();

                    valueList.addAll((List<String>) msg.obj);
                    //addDeviceNames();
                    la_discoveredDevices.notifyDataSetChanged();
                    break;
                case UsdpService.MSG_CHATMSGRECEIVED:
                    Toast.makeText(UsdpMainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
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
