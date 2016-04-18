package de.tu_darmstadt.seemoo.usdpprototype.devicebasics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;

/**
 * Created by kenny on 11.04.16.
 */
public class Helper {

    private static final String LOGTAG = "Helper";

    public static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Bitmap generateQR(String input) {
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

    public static boolean[] getPrimitiveArrayMsbFront(ArrayList<Boolean> pattern) {
        boolean[] primitives = new boolean[pattern.size()];
        int index = 0;
        for (Boolean bit : pattern) {
            primitives[index++] = bit;
        }
        return primitives;
    }

    public static ArrayList<Integer> getDigitlistFromInt(int val) {
        ArrayList<Integer> digits = new ArrayList<Integer>();
        do {
            digits.add(0, val % 10);
            val /= 10;
        } while (val > 0);
        return digits;
    }

    public static boolean[] getPrimitiveArrayMsbBack(ArrayList<Boolean> pattern) {
        boolean[] primitives = new boolean[pattern.size()];
        int index = pattern.size() - 1;
        for (Boolean object : pattern) {
            primitives[index--] = object;
        }
        return primitives;
    }

    public static boolean[] getSendingPattern(boolean[] data) {
        ArrayList<Boolean> pattern = new ArrayList<>();
        pattern.add(Boolean.FALSE);
        pattern.add(Boolean.FALSE);
        for (int pos = 0; pos < data.length; pos++) {
            pattern.add(Boolean.TRUE);
            if (data[pos]) {
                pattern.add(Boolean.TRUE);
            }
            pattern.add(Boolean.FALSE);
        }
        return getPrimitiveArrayMsbFront(pattern);
    }

    public static boolean[] getBinaryArray(int data, int bits) {
        boolean[] pattern = new boolean[bits];
        for (int i = 0; i < pattern.length; i++) {
            pattern[pattern.length - i - 1] = ((data >> i) & 1) == 1;
        }
        return pattern;
    }

    /*
    http://introcs.cs.princeton.edu/java/31datatype/Hex2Decimal.java.html
     */
    public static int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }

    public static int getInt(boolean[] data) {
        int res = 0, l = data.length;
        for (int i = 0; i < l; ++i) {
            res = (res << 1) + (data[i] ? 1 : 0);
        }
        return res;
    }

    public static char findHighestValPos(int[] val) {
        char res = 0;
        for (char pos = 0; pos < val.length; pos++) {
            if (val[pos] > val[res]) {
                res = pos;
            }
        }
        if (val[res] < 5) {
            return '\uffff';
        }
        return res;
    }

    public String getBinaryStringFromArray(boolean[] ray) {
        String str = "";
        for (int pos = 0; pos < ray.length; pos++) {
            if (ray[pos]) {
                str += "1";
            } else {
                str += "0";
            }
        }
        return str;
    }
}
