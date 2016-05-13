/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.account;

import com.woording.android.App;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyServerAuthenticator implements IServerAuthenticator {

    @Override
    public String signUp(String email, String username, String password) {
        // TODO: register new user on the server and return its auth token
        String success = null;
        try {
            URL url = new URL(App.API_LOCATION + "/register");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(15000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(true);
            urlConnection.setInstanceFollowRedirects(false);
            // Set the content-type as json --> Important
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");

            // Now create the JSONObject
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);
            data.put("email", email);
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
                if (!response.getString("response").contains("ERROR")) {
                    // Successfully registered
                    success = "success";
                }

                inputStream.close();
            }
            urlConnection .disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return success;
    }

    @Override
    public String signIn(String username, String password) {
        String authToken = null;
        try {
            // Setup connection
            URL url = new URL(App.API_LOCATION + "/authenticate");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(15000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(true);
            urlConnection.setInstanceFollowRedirects(false);
            // Set the content-type as json --> Important
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");

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
                authToken = response.getString("token");

                inputStream.close();
            }
            urlConnection .disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return authToken;
    }
}
