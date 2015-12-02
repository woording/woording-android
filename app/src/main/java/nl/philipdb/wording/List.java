/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.wording;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class List implements Serializable {
    public String mName;
    public String mLanguage1;
    public String mLanguage2;
    public String mSharedWith;

    public ArrayList<String> mLanguage1Words = new ArrayList<>();
    public ArrayList<String> mLanguage2Words = new ArrayList<>();

    public static HashMap<String, String> mLanguageCodes = null;

    public List(String n, String l1, String l2, String sw) {
        mName = n;
        mLanguage1 = l1;
        mLanguage2 = l2;
        mSharedWith = sw;
    }

    public void setWords(ArrayList<String> language1, ArrayList<String> language2) {
        if (language1.size() == language2.size()) {
            mLanguage1Words = language1;
            mLanguage2Words = language2;
        }
    }

//    public String getTranslation(String word) {
//        if (mLanguage1Words.contains(word)) return mLanguage2Words.get(mLanguage1Words.indexOf(word));
//        else if (mLanguage2Words.contains(word)) return mLanguage1Words.get(mLanguage1Words.indexOf(word));
//        else return null;
//    }

    public static String getLanguageName(Context context, String languageCode) {
        if (mLanguageCodes == null) {
            mLanguageCodes = new HashMap<>();
            mLanguageCodes.put("eng", context.getString(R.string.english));
            mLanguageCodes.put("dut", context.getString(R.string.dutch));
            mLanguageCodes.put("ger", context.getString(R.string.german));
            mLanguageCodes.put("fre", context.getString(R.string.french));
            mLanguageCodes.put("lat", context.getString(R.string.latin));
            mLanguageCodes.put("gre", context.getString(R.string.greek));
            mLanguageCodes.put("spa", context.getString(R.string.spanish));
            mLanguageCodes.put("por", context.getString(R.string.portuguese));
            mLanguageCodes.put("ita", context.getString(R.string.italian));
        }
        return mLanguageCodes.get(languageCode);
    }

    public int getTotalWords() {
        return mLanguage1Words.size();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("listname", mName);
        json.put("language_1_tag", mLanguage1);
        json.put("language_2_tag", mLanguage2);
        json.put("shared_with", mSharedWith);
        if (getTotalWords() > 0) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < mLanguage1Words.size(); i++) {
                JSONObject temp = new JSONObject();
                temp.put("language_1_text", mLanguage1Words.get(i));
                temp.put("language_2_text", mLanguage2Words.get(i));
                array.put(temp);
            }
            json.put("words", array);
        }
        return json;
    }
}
