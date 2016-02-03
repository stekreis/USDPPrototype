package de.tu_darmstadt.seemoo.usdpprototype;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

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
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
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
        Log.d(LOGTAG, "service was destroyed");
        super.onDestroy();
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
                    if (activityMessenger != null) {
                        Message mess = Message.obtain(null,
                                MSG_SAY_HELLO);
                        try {
                            activityMessenger.send(mess);
                        } catch (RemoteException e) {

                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
