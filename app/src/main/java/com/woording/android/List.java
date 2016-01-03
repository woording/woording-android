/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    private static HashMap<String, String> mLanguageCodes = null;
    private static HashMap<String, String> mLocales = null;

    public List(String name, String language1, String language2, String sharedWith) {
        this.mName = name;
        this.mLanguage1 = language1;
        this.mLanguage2 = language2;
        this.mSharedWith = sharedWith;
    }

    public void setWords(ArrayList<String> language1, ArrayList<String> language2) {
        if (language1.size() == language2.size()) {
            this.mLanguage1Words = language1;
            this.mLanguage2Words = language2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List)) {
            return false;
        }

        List list = (List) o;
        return this.mName.equals(list.mName)
                && this.mLanguage1.equals(list.mLanguage1)
                && this.mLanguage2.equals(list.mLanguage2)
                && this.mSharedWith.equals(list.mSharedWith)
                && this.mLanguage1Words.equals(list.mLanguage1Words)
                && this.mLanguage2Words.equals(list.mLanguage2Words);
    }

    public List deepClone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (List) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public static String getLocale(String languageCode) {
        if (mLocales == null) {
            mLocales = new HashMap<>();
            mLocales.put("eng", "en");
            mLocales.put("dut", "nl");
            mLocales.put("ger", "de");
            mLocales.put("fre", "fr");
            mLocales.put("gre", "el");
            mLocales.put("spa", "es");
            mLocales.put("por", "pt");
            mLocales.put("ita", "it");
        }
        return mLocales.get(languageCode);
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

    @Override
    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            Log.d("JSONException", "toString: Error in JSON");
            return null;
        }
    }

    public static List fromString(String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        List list = new List(json.getString("listname"), json.getString("language_1_tag"),
                json.getString("language_2_tag"), json.getString("shared_with"));
        if (json.getJSONArray("words").length() > 0) {
            // If words are saved, add them to the list object
            JSONArray jsonWords = json.getJSONArray("words");
            ArrayList<String> language1 = new ArrayList<>();
            ArrayList<String> language2 = new ArrayList<>();
            for (int i = 0; i < jsonWords.length(); i++) {
                JSONObject temp = jsonWords.getJSONObject(i);
                language1.add(temp.getString("language_1_text"));
                language2.add(temp.getString("language_2_text"));
            }
            list.setWords(language1, language2);
        }
        return list;
    }
}
