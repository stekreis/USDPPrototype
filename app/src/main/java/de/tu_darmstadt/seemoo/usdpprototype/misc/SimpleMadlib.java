package de.tu_darmstadt.seemoo.usdpprototype.misc;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by kenny on 29.03.16.
 *
 * substitutes MadLib, uses simple wordlist
 */
public class SimpleMadlib {

    public static final int WORDLIST_LENGTH = 20580;
    private static final String LOGTAG = "SimpleMadlib";
    private final String[] words = new String[WORDLIST_LENGTH];

    public String getWord(int pos) {
        return words[pos];
    }

    public String getSentence(int num) {
        int rest = num;
        String retVal = "";
        while (rest != 0) {
            retVal = words[(rest % 10) * 1000] + " " + retVal;
            rest /= 10;
        }
        return retVal.trim();
    }


    // read wordlist file
    public boolean parseWordlist(Context context) {
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open("wordlist.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            for (int pos = 0; pos < words.length; pos++) {
                words[pos] = br.readLine();
            }
            return true;
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
            return false;
        }
    }

}
