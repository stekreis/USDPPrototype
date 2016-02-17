package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import java.math.BigInteger;

/**
 * Created by kenny on 06.02.16.
 */
public abstract class SecureAuthentication {




    public abstract void init();

    public abstract int generateKey(int othrPublicVal);

    public abstract int getGeneratedKeyVal();

    public abstract int getPublicDeviceKey();

}
