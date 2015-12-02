/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.wording;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static String username;

    private List[] mLists = new List[]{};
    private GetListsTask mGetListsTask;

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private static ListsViewAdapter mListsViewAdapter;

    protected static Context mContext;

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
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Setup RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.lists_view);
        // Setup LinearLayoutManager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // Setup RecyclerView Adapter
        mListsViewAdapter = new ListsViewAdapter(new ArrayList<>(Arrays.asList(mLists)));
        mRecyclerView.setAdapter(mListsViewAdapter);

        // Setup SwipeRefresLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getLists();
            }
        });

        // Load saved data
        SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
        NetworkCaller.mToken = sharedPreferences.getString("token", null);

        mContext = this;

        // TODO: Needs better logic
        if (NetworkCaller.mToken == null || username == null) {
            openLoginActivity(this);
        } else {
            if (isNetworkAvailable(this)) getLists();
            else Snackbar.make(mSwipeRefreshLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (NetworkCaller.mToken != null) getLists();
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
        if (mGetListsTask != null) {
            return;
        }
        mSwipeRefreshLayout.setRefreshing(true);

        mGetListsTask = new GetListsTask();
        mGetListsTask.execute((Void) null);
    }

    public class GetListsTask extends NetworkCaller {

        GetListsTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mLists = getLists(username);
                return mLists != null;
            } catch (IOException e) {
                Log.d("IOException", "Something bad happened on the IO");
            } catch (JSONException e) {
                Log.d("JSONException", "The JSON fails");
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mGetListsTask = null;
            mSwipeRefreshLayout.setRefreshing(false);

            if (success) {
                mListsViewAdapter.updateList(mLists);
                // Write lists to cache
                try {
                    CacheHandler.writeLists(MainActivity.mContext, mLists);
                } catch (IOException e) {
                    Log.d("IO", "Something bad with the IO: " + e);
                } catch (JSONException e) {
                    Log.d("JSON", "Something bad with the JSON: " + e);
                }
            }
        }

        @Override
        protected void onCancelled() {
            mGetListsTask = null;
        }
    }
}
