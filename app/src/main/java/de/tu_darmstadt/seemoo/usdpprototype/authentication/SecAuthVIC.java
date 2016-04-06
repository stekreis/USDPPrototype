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

/**
 * Created by kenny on 08.02.16.
 */
public class SecAuthVIC extends SecureAuthentication {

    public final static int pValue = 913247;
    public final static int gValue = 370934;
    private static int bitlength = 20;
    private final String LOGTAG = "SecAuthVIC";
    private final int BITLENGTHMAX = 32;
    private BigInteger priv;
    private BigInteger pub;

    private int generatedKey = 0;

    @Override
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


    public static String toMD5(byte[] convertible) {
        return new String(Hex.encodeHex(DigestUtils.md5(convertible)));
    }


    public int getGeneratedKeyVal() {
        return generatedKey;
    }

    public int getPublicDeviceKey() {
        return pub.intValue();
    }
}
