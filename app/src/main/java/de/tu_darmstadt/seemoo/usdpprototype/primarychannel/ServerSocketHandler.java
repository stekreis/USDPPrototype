package de.tu_darmstadt.seemoo.usdpprototype.primarychannel;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 *
 * based on https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/src/com/example/android/wifidirect/discovery/GroupOwnerSocketHandler.java
 */
public class ServerSocketHandler extends Thread {

    private static final String TAG = "ServerSocketHandler";
    private final int THREAD_COUNT = 10;
    /**
     * A ThreadPool for client sockets.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private ServerSocket socket = null;
    private Handler handler;

    public ServerSocketHandler(Handler handler) throws IOException {
        try {
            socket = new ServerSocket(4545);
            this.handler = handler;
            Log.d("ServerSocketHandler", "Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }

    }

    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a MessageManager instance when
                // there is a new connection
                pool.execute(new MessageManager(socket.accept(), handler));
                Log.d(TAG, "Launching the I/O handler");
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {

                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

}
