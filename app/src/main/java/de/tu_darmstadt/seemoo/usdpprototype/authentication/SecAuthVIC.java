package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import de.tu_darmstadt.seemoo.usdpprototype.devicebasics.Helper;

/**
 * Created by kenny on 08.02.16.
 */
public class SecAuthVIC {

    public final static int pValue = 913247;
    public final static int gValue = 370934;
    public static final int OOB_BITLENGTH = 20;
    private static final String LOGTAG = "SecAuthVIC";
    private static final int BITLENGTHMAX = 512;
    private static int bitlength = 256;
    private BigInteger priv;
    private BigInteger pub;

    private int generatedKey = 0;

    public static String toMD5(byte[] convertible) {
        return new String(Hex.encodeHex(DigestUtils.md5(convertible)));
    }

    public static String toMD5(String convertible) {
        return new String(Hex.encodeHex(DigestUtils.md5(convertible)));
    }

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

    private void genPAndG() throws InvalidParameterSpecException, NoSuchAlgorithmException {
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
        paramGen.init(bitlength);

        AlgorithmParameters params = paramGen.generateParameters();
        DHParameterSpec dhSpec = (DHParameterSpec) params.getParameterSpec(DHParameterSpec.class);

        Log.d(LOGTAG, "testingtesting" + dhSpec.getP() + "," + dhSpec.getG() + "," + dhSpec.getL());
    }

    public int generateKey(int othrPublicVal) {
        BigInteger pVal = BigInteger.valueOf(pValue);
        BigInteger otherPublicVal = BigInteger.valueOf(othrPublicVal);
        return generatedKey = otherPublicVal.modPow(priv, pVal).intValue();
    }


    public int getGeneratedKeyVal() {
        return generatedKey;
    }

    public int getPublicDeviceKey() {
        return pub.intValue();
    }

    public String getHashedVal() {
        return toMD5(String.valueOf(generatedKey)).substring(0, 5);
    }

    public int getHashedIntVal() {
        Log.d(LOGTAG, "genPkey:" + generatedKey);
        int hashedkey = Helper.hex2decimal(getHashedVal());
        Log.d(LOGTAG, "genHkey:" + hashedkey);
        return hashedkey;
    }
}
