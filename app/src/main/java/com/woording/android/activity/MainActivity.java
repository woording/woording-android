/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.woording.android.CacheHandler;
import com.woording.android.List;
import com.woording.android.ListsViewAdapter;
import com.woording.android.NetworkCaller;
import com.woording.android.R;
import com.woording.android.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static String username;

    private List[] mLists = new List[]{};
    public static List lastDeletedList = null;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CoordinatorLayout mCoordinatorLayout;

    private static ListsViewAdapter mListsViewAdapter;

    public static Context mContext;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load cache
        try {
            mLists = CacheHandler.readLists(this);
        } catch (IOException e) {
            Log.d("Cache", "Something went wrong with the IO: " + e);
        } catch (JSONException e) {
            Log.d("Cache", "Something went wrong with the JSON: " + e);
        }

        // Setup toolbar
        Toolbar mToolbar;
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Setup RecyclerView
        RecyclerView mRecyclerView;
        mRecyclerView = (RecyclerView) findViewById(R.id.lists_view);
        // Setup LinearLayoutManager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // Setup RecyclerView Adapter
        mListsViewAdapter = new ListsViewAdapter(new ArrayList<>(Arrays.asList(mLists)));
        mRecyclerView.setAdapter(mListsViewAdapter);

        // Setup SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getLists();
            }
        });

        // Setup Floating Action Button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, EditListActivity.class);
                startActivity(intent);
            }
        });

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.root_view);

        // Load saved data
        SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
        NetworkCaller.mToken = sharedPreferences.getString("token", null);

        mContext = this;

        int requestCode = getIntent().getIntExtra("requestCode", 0);
        if (requestCode != 0) {
            switch (requestCode) {
                case ListViewActivity.NO_WORDS_DATA:
                    Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG)
                            .show();
                    break;

                case ListViewActivity.DELETED_LIST:
                    Snackbar.make(mCoordinatorLayout, getString(R.string.list_deleted), Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Log.d(TAG, "onClick: Undo delete");
                                    if (lastDeletedList != null) {
                                        saveList(lastDeletedList);
                                        getLists();
                                    }
                                }
                            }).show();
                    break;
            }
        }

        // TODO: Needs better logic
        if (NetworkCaller.mToken == null || username == null) {
            openLoginActivity(this);
        } else {
            if (isNetworkAvailable(this)) getLists();
            else Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        restoreLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreLists();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        lastDeletedList = null;

        if (mLists.length > 0) {
            String[] lists = new String[mLists.length];
            for (int i = 0; i < mLists.length; i++) {
                lists[i] = mLists[i].toString();
            }

            SharedPreferences sharedPreferences = getSharedPreferences("nl.philipdb.woording_MainActivity", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet("Lists", new HashSet<>(Arrays.asList(lists)));
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // int id = item.getItemId();



        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
           finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Snackbar.make(mCoordinatorLayout, R.string.press_BACK_again_to_exit, Snackbar.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    protected void restoreLists() {
        if (mLists.length == 0) {
            // First try to get from sharedPrefs
            SharedPreferences sharedPreferences = getSharedPreferences("nl.philipdb.woording_MainActivity", MODE_PRIVATE);
            Set listsSet = sharedPreferences.getStringSet("lists", new HashSet<String>(0));
            String[] stringLists = (String[]) listsSet.toArray(new String[listsSet.size()]);
            mLists = new List[stringLists.length];
            for (int i = 0; i < stringLists.length; i++) {
                try {
                    mLists[i] = List.fromString(stringLists[i]);
                } catch (JSONException e) {
                    Log.d(TAG, "onStart: Error while converting string to list");
                }
            }
            mListsViewAdapter.updateList(mLists);

            // Then try to update or to read from cache
            if (NetworkCaller.mToken != null && isNetworkAvailable(this)) getLists();
            else {
                // Load cache
                try {
                    mLists = CacheHandler.readLists(this);
                } catch (IOException e) {
                    Log.d("Cache", "Something went wrong with the IO: " + e);
                } catch (JSONException e) {
                    Log.d("Cache", "Something went wrong with the JSON: " + e);
                }
                mListsViewAdapter.updateList(mLists);
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void openLoginActivity(Context context) {
        Intent loginIntent = new Intent(context, LoginActivity.class);
        context.startActivity(loginIntent);
    }

    private void getLists() {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            // Create the data that is sent
            JSONObject data = new JSONObject();
            data.put("token", NetworkCaller.mToken);
            // Create the request
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.POST, NetworkCaller.API_LOCATION + "/" + username, data, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                // Check for errors
                                if (response.getString("username") != null && response.getString("username").contains("ERROR")) {
                                    MainActivity.openLoginActivity(MainActivity.mContext);
                                    return;
                                }

                                // Handle the response
                                JSONArray jsonArray = response.getJSONArray("lists");
                                JSONObject listObject;
                                List[] lists = new List[jsonArray.length()];
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    listObject = jsonArray.getJSONObject(i);
                                    List tmp = new List(listObject.getString("listname"), listObject.getString("language_1_tag"),
                                            listObject.getString("language_2_tag"), listObject.getString("shared_with"));
                                    lists[i] = tmp;
                                }
                                mLists = lists;
                                mListsViewAdapter.updateList(mLists);
                                // Write lists to cache
                                try {
                                    CacheHandler.writeLists(MainActivity.mContext, mLists);
                                } catch (IOException e) {
                                    Log.d("IO", "Something bad with the IO while writing cache: " + e);
                                } catch (JSONException e) {
                                    Log.d("JSON", "Something bad with the JSON while writing cache: " + e);
                                }
                            } catch (JSONException e) {
                                Log.d("JSONException", "The JSON fails");
                            }
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            Log.e("VolleyError", error.toString());
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });

            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
        } catch (JSONException e) {
            mSwipeRefreshLayout.setRefreshing(false);
            Log.d("JSONException", "The JSON fails");
        }
    }

    public void saveList(final List list) {
        try {
            JSONObject data = new JSONObject();
            data.put("token", NetworkCaller.mToken);
            data.put("username", username);
            data.put("list_data", list.toJSON());

            // Create the request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, NetworkCaller.API_LOCATION + "/savelist",
                    data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Write lists to cache
                    try {
                        CacheHandler.writeList(MainActivity.mContext, list);
                    } catch (IOException e) {
                        Log.d("IO", "Something bad with the IO: " + e);
                    } catch (JSONException e) {
                        Log.d("JSON", "Something bad with the JSON: " + e);
                    }
                    Snackbar.make(mCoordinatorLayout, R.string.restored, Snackbar.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Log.e("VolleyError", error.getMessage());
                }
            });
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(this).addToRequestQueue(request);
        } catch (JSONException e) {
            Log.d("JSONException", "The JSON fails");
        }
    }
}
