/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

public class App extends Application {

    private static Context context;

    public static final String API_LOCATION = "https://api.woording.com";

    public static boolean mDualPane;

    @Override
    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();

        mDualPane = getResources().getBoolean(R.bool.is_dual_pane);

        if (BuildConfig.DEBUG) {
            // Enable StrictMode
            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );
        }
    }

    public static Context getAppContext() {
        return App.context;
    }
}
