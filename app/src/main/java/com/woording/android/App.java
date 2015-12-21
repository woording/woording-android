/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Context context;

    public static final String API_LOCATION = "http://api.woording.com";

    @Override
    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}
