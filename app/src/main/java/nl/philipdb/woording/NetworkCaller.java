/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.woording;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public abstract class NetworkCaller extends AsyncTask<Void, Void, Boolean> {
    public static final String API_LOCATION = "http://api.woording.com";
    public static String mToken = null;

    public HttpURLConnection setupConnection(String location, boolean doOutput) throws IOException {
        // Setup connection
        URL url = new URL(API_LOCATION + location);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setReadTimeout(15000);
        urlConnection.setConnectTimeout(15000);
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(doOutput);
        urlConnection.setUseCaches(true);
        urlConnection.setInstanceFollowRedirects(false);
        // Set the content-type as json --> Important
        urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");

        return urlConnection;
    }

    public boolean authorize(String username, String password)  throws IOException, JSONException {
        // Setup connection
        HttpURLConnection urlConnection = setupConnection("/authenticate", true);
        // Now create the JSONObject
        JSONObject data = new JSONObject();
        data.put("username", username);
        data.put("password", password);
        // And send the data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
        writer.write(data.toString());
        writer.flush();
        writer.close();
        output.close();
        // And now connect to the server
        urlConnection.connect();

        // Now check the response code
        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder json = new StringBuilder();
            String inputLine = "";

            while ((inputLine = bufferedReader.readLine()) != null) {
                json.append(inputLine);
            }

            JSONObject response = new JSONObject(json.toString());
            MainActivity.username = username;
            mToken = response.getString("token");

            inputStream.close();

            // Save the token
            SharedPreferences prefs = MainActivity.mContext.getSharedPreferences("data", Context.MODE_APPEND);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("token", NetworkCaller.mToken);
            editor.putString("username", MainActivity.username);
            editor.apply();

            urlConnection .disconnect();
            return true;
        }
        urlConnection .disconnect();
        return false;
    }

    public List[] getLists(String username) throws IOException, JSONException {
        // Initialize connection
        HttpURLConnection urlConnection = setupConnection("/" + username, true);
        // Add content
        JSONObject data = new JSONObject();
        data.put("token", NetworkCaller.mToken);
        // And send the data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
        writer.write(data.toString());
        writer.flush();
        writer.close();
        output.close();
        // And connect
        urlConnection.connect();

        // Check for the response from the server
        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder json = new StringBuilder();
            String inputLine = "";

            while ((inputLine = bufferedReader.readLine()) != null) {
                json.append(inputLine);
            }

            JSONObject response = new JSONObject(json.toString());

            inputStream.close();

            // Check for errors
            if (response.getString("username") != null && response.getString("username").contains("ERROR")) {
                MainActivity.openLoginActivity(MainActivity.mContext);
                return null;
            }

            // Handle the response
            JSONArray jsonArray = response.getJSONArray("lists");
            JSONObject listObject;
            List[] mLists = new List[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i ++) {
                listObject = jsonArray.getJSONObject(i);
                List tmp = new List(listObject.getString("listname"), listObject.getString("language_1_tag"),
                        listObject.getString("language_2_tag"), listObject.getString("shared_with"));
                mLists[i] = tmp;
            }
            return mLists;
        }
        urlConnection.disconnect();
        return null;
    }

    public List getList(String username, String listName) throws IOException, JSONException {
        // Initialize connection
        HttpURLConnection urlConnection = setupConnection("/" + username + "/" + listName.replace(" ", "%20"), true);
        // Add content
        JSONObject data = new JSONObject();
        data.put("token", NetworkCaller.mToken);
        // And send the data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
        writer.write(data.toString());
        writer.flush();
        writer.close();
        output.close();
        // And connect
        urlConnection.connect();

        // Check for the response from the server
        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder json = new StringBuilder();
            String inputLine = "";

            while ((inputLine = bufferedReader.readLine()) != null) {
                json.append(inputLine);
            }

            JSONObject response = new JSONObject(json.toString());

            inputStream.close();

            // Check for errors
            try {
                if (response.getString("username") != null) {
                    MainActivity.openLoginActivity(MainActivity.mContext);
                    return null;
                }
            } catch (JSONException e) {
                List mList = new List(response.getString("listname"), response.getString("language_1_tag"),
                        response.getString("language_2_tag"), response.getString("shared_with"));
                JSONArray JSONWords = response.getJSONArray("words");
                ArrayList<String> language1Words = new ArrayList<>();
                ArrayList<String> language2Words = new ArrayList<>();
                for (int i = 0; i < JSONWords.length(); i++) {
                    JSONObject object = JSONWords.getJSONObject(i);
                    language1Words.add(object.getString("language_1_text"));
                    language2Words.add(object.getString("language_2_text"));
                }
                mList.setWords(language1Words, language2Words);
                return mList;
            }
        }
        urlConnection.disconnect();
        return null;
    }

    public void deleteList(String username, String listName) throws IOException, JSONException {
        // Initialize connection
        HttpURLConnection urlConnection = setupConnection("/deleteList", false);
        // Add content
        JSONObject data = new JSONObject();
        data.put("username", username);
        data.put("listname", listName);
        data.put("token", NetworkCaller.mToken);
        // And send the data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
        writer.write(data.toString());
        writer.flush();
        writer.close();
        output.close();
        // And connect
        urlConnection.connect();

        Log.d("NetworkCaller", "deleteList: " + urlConnection.getResponseCode());
        Log.d("NetworkCaller", "deleteList: list deleted");

        urlConnection.disconnect();
    }

    public void saveList(String username, List list) throws IOException, JSONException {
        // Initialize connection
        HttpURLConnection urlConnection = setupConnection("/savelist", false);
        // Add content
        JSONObject data = new JSONObject();
        data.put("username", username);
        data.put("list_data", list.toJSON());
        data.put("token", NetworkCaller.mToken);
        // And send the data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
        writer.write(data.toString());
        writer.flush();
        writer.close();
        output.close();
        // And connect
        urlConnection.connect();

        Log.d("NetworkCaller", "saveList: " + urlConnection.getResponseCode());
        Log.d("NetworkCaller", "saveList: list saved");

        urlConnection.disconnect();
    }

}
