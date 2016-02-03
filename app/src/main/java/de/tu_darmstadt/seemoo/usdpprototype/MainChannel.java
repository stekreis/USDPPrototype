package de.tu_darmstadt.seemoo.usdpprototype;

/**
 * Created by kenny on 26.01.16.
 */
public abstract class MainChannel {

    public abstract boolean init();
    public abstract boolean discoverPeers();
    public abstract boolean connect();
    public abstract boolean send();
    public abstract boolean disconnect();

}
