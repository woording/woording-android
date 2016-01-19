package com.woording.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.woording.android.App;
import com.woording.android.List;

public class ParseDeepLinkActivity extends AppCompatActivity {

    private static final String TAG = ParseDeepLinkActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Extrapolates the deeplink data
        Intent intent = getIntent();
        Uri deeplink = intent.getData();

        // Parse the deeplink and take the adequate action
        if (deeplink != null) {
            parseDeepLink(deeplink);
        }
    }


    private void parseDeepLink(Uri deeplink) {
        // The path of the deep link, e.g. '/<username>/<list>'
        String path;
        if (deeplink.getFragment() != null) path = deeplink.getFragment();
        else path = deeplink.getPath();
        // Delete first slash and (if available) last slash
        StringBuilder builder = new StringBuilder(path);
        // Remove beginning slashes
        if (path.startsWith("!/")) {
            builder.delete(0, 2);
        } else builder.deleteCharAt(0);
        // Remove ending slash
        if (path.endsWith("/")) {
            builder.deleteCharAt(path.length() - 2);
        }
        path = builder.toString();

        String[] data = path.split("/");

        Log.d(TAG, "parseDeepLink: path: " + path);
        Log.d(TAG, "parseDeepLink: data length: " + data.length);
        for (int i = 0; i < data.length; i++) {
            Log.d(TAG, "parseDeepLink: data[" + i + "]: " + data[i]);
        }

        switch (data.length) {
            case 2:
                if (App.mDualPane) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("username", data[0]);
                    intent.putExtra("listname", data[1]);
                    intent.putExtra("fromDeepLink", true);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, ListViewActivity.class);
                    intent.putExtra("username", data[0]);
                    intent.putExtra("list", new List(data[1], "", "", ""));
                    intent.putExtra("fromDeepLink", true);
                    startActivity(intent);
                }
                break;
            case 1:
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", data[0]);
                intent.putExtra("fromDeepLink", true);
                startActivity(intent);
                break;
            default:
                startActivity(new Intent(this, MainActivity.class));
        }
    }
}
