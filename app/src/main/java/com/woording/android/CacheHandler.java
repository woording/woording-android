/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class CacheHandler {

    public static void writeLists(Context context, List[] lists) throws IOException, JSONException {
        for (List list : lists) {
            writeList(context, list);
        }
    }

    public static void writeList(Context context, List list) throws IOException, JSONException {
        FileOutputStream outputStream;
        File file = new File(context.getCacheDir(), "lists/" + list.mName);
        if (!file.exists()) {
            file.mkdirs();
            file.createNewFile();
        } else if (list.getTotalWords() > 0) {
            file.delete();
            file.createNewFile();
        } else return;

        outputStream = new FileOutputStream(file);
        outputStream.write(list.toJSON().toString().getBytes());
        outputStream.close();

    }

    public static List[] readLists(Context context) throws IOException, JSONException {
        File[] files = new File(context.getCacheDir(), "lists").listFiles();
        if (files == null) return new List[]{};
        List[] lists = new List[files.length];
        for (int i = 0; i < files.length; i++) {
            lists[i] = readList(files[i]);
        }

        // Remove all null objects
        ArrayList<List> tmp = new ArrayList<>(Arrays.asList(lists));
        for (int i = 0; i < tmp.size(); i++) {
            try {
                if (tmp.get(i) == null) tmp.remove(i);
            } catch (IndexOutOfBoundsException e) {
                tmp.add(i, new List("Dummy", "Dummy", "Dummy", "Dummy"));
                tmp.remove(i);
            }
        }
        return tmp.toArray(new List[tmp.size()]);
    }

    public static List readList(Context context, String listname) throws IOException, JSONException {
        File file = new File(context.getCacheDir(), "lists/" + listname);
        return readList(file);
    }

    @Nullable
    public static List readList(File file) throws IOException, JSONException {
        if (file.isDirectory()) {
            file.delete();
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder json = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            json.append(line);
        }

        JSONObject jsonObject = new JSONObject(json.toString());

        // Translate json to List
        List list = new List(jsonObject.getString("listname"), jsonObject.getString("language_1_tag"),
                jsonObject.getString("language_2_tag"), jsonObject.getString("shared_with"));
        // Check for words
        try {
            JSONArray wordsArray = jsonObject.getJSONArray("words");
            ArrayList<String> language1Words = new ArrayList<>();
            ArrayList<String> language2Words = new ArrayList<>();
            for (int j = 0; j < wordsArray.length(); j++) {
                JSONObject object = wordsArray.getJSONObject(j);
                language1Words.add(object.getString("language_1_text"));
                language2Words.add(object.getString("language_2_text"));
            }
            list.setWords(language1Words, language2Words);
        } catch (JSONException e) {
            Log.d("JSON", "No words");
        }
        return list;
    }

    public static boolean deleteList(Context context, String listName) throws IOException {
        File listFile = new File(context.getCacheDir() + "lists/" + listName);
        return listFile.delete();
    }
}
