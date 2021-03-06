package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.tu_darmstadt.seemoo.usdpprototype.misc.Helper;

/**
 * Created by kenny on 08.02.16.
 *
 * Diffie Hellman key generator
 *
 * initially based on http://www.java2s.com/Tutorial/Java/0490__Security/ImplementingtheDiffieHellmankeyexchange.htm
 *
 */
public class SecureAuthentication {

    public final static int pValue = 913247;
    public final static int gValue = 370934;
    public static final int OOB_BITLENGTH = 20;
    private static final String LOGTAG = "SecAuth";
    private static final int BITLENGTHMAX = 512;
    private static int bitlength = 256;
    private BigInteger priv;
    private BigInteger pub;

    public static String toMD5(byte[] convertible) {
        return new String(Hex.encodeHex(DigestUtils.md5(convertible)));
    }

    public static String toMD5(String convertible) {
        return new String(Hex.encodeHex(DigestUtils.md5(convertible)));
    }

    public static String getHashedVal(int generatedKey) {
        return toMD5(String.valueOf(generatedKey)).substring(0, 5);
    }

    public static int getHashedIntVal(int generatedKey) {
        int hashedkey = Helper.hex2decimal(getHashedVal(generatedKey));
        Log.d(LOGTAG, "genHkey:" + hashedkey);
        return hashedkey;
    }

    // generate local public and private key
    public void init() {
        if (bitlength > BITLENGTHMAX) {
            bitlength = BITLENGTHMAX;
        }
        int aMaxMod = (int) Math.pow(2, bitlength);

        priv = new BigDecimal(Math.random() * aMaxMod).toBigInteger();
        BigInteger g = BigInteger.valueOf(gValue);
        pub = g.modPow(priv, BigInteger.valueOf(pValue));

        Log.d(LOGTAG, "priv: " + priv.toString());
        Log.d(LOGTAG, "pub: " + pub.toString());
    }


    // generates common secret based on remote public key and local private key
    public int generateKey(int othrPublicVal) {
        BigInteger pVal = BigInteger.valueOf(pValue);
        BigInteger otherPublicVal = BigInteger.valueOf(othrPublicVal);
        return otherPublicVal.modPow(priv, pVal).intValue();
    }

    public int getPublicDeviceKey() {
        return pub.intValue();
    }
}
