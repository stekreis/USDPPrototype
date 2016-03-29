package de.tu_darmstadt.seemoo.usdpprototype.authentication;

import java.util.List;
import java.util.ListIterator;

/**
 * Created by kenny on 29.03.16.
 */
public class SimpleMadlib {

    private static final String[] words = {
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
    };

    public static String getWord(byte pos){
        return words[pos];
    }

    public static String getSentence(List<Integer> numlist){
        ListIterator<Integer> liter = numlist.listIterator();
        String stc = "";
        while(liter.hasNext()){
            stc+=liter.next() + " ";
        }
        return stc.trim();
    }

}
