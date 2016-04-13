
package de.tu_darmstadt.seemoo.usdpprototype.primarychannel;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.tu_darmstadt.seemoo.usdpprototype.UsdpService;

public class ClientSocketHandler extends Thread {

    private static final String LOGTAG = "ClientSocketHandler";
    private Handler handler;
    private MessageManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    UsdpService.SERVER_PORT), 5000);
            Log.d(LOGTAG, "Launching the I/O handler");
            chat = new MessageManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    public MessageManager getChat() {
        return chat;
    }

}
