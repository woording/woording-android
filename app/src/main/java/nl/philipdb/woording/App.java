/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.woording;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}
