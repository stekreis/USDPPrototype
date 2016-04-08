package de.tu_darmstadt.seemoo.usdpprototype.secondarychannel;

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
 */
public class SimpleMadlib {

    public static final int WORDLIST_LENGTH = 1024;
    private static final String LOGTAG = "SimpleMadlib";
    private final String[] words = new String[WORDLIST_LENGTH];

    public static String getSentence(List<Integer> numlist) {
        ListIterator<Integer> liter = numlist.listIterator();
        String stc = "";
        while (liter.hasNext()) {
            stc += liter.next() + " ";
        }
        return stc.trim();
    }

    public String getWord(int pos) {
        return words[pos];
    }

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
