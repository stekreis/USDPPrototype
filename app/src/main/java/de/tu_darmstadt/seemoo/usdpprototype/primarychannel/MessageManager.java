
package de.tu_darmstadt.seemoo.usdpprototype.primarychannel;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to Service.
 */
public class MessageManager implements Runnable {

    public static final byte MC_INITHANDLER = 1;
    public static final byte MC_MSGRECEIVED = 2;
    public static final byte MSGTYPE_HELLO = 4;
    public static final byte MSGTYPE_HELLOBACK = 5;
    public static final byte MSGTYPE_AUTH_DIALOG = 6;
    public static final byte MSGTYPE_ENCR = 7;
    public static final byte MSGTYPE_TEST = 8;

    private Socket socket = null;
    private Handler handler;

    public Socket getSocket(){
        return socket;
    }

    public MessageManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    private InputStream inStream;
    private OutputStream outStream;
    private static final String LOGTAG = "MessageManager";

    @Override
    public void run() {
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(MC_INITHANDLER, this)
                    .sendToTarget();
            while (true) {
                try {
                    // read from InputStream. 'bytes' represents size
                    bytes = inStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // forward received bytes to the Service
                    Log.d(LOGTAG, "received: " + String.valueOf(buffer));
                    handler.obtainMessage(MC_MSGRECEIVED,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(LOGTAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            outStream.write(buffer);
        } catch (IOException e) {
            Log.e(LOGTAG, "Exception during write", e);
        }
    }

}
