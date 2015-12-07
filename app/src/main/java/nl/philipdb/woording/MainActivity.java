/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.woording;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
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

import org.json.JSONException;

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

    private GetListsTask mGetListsTask;
    private SaveListTask mSaveListTask;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CoordinatorLayout mCoordinatorLayout;

    private static ListsViewAdapter mListsViewAdapter;

    protected static Context mContext;
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
                                        mSaveListTask = new SaveListTask(lastDeletedList, username);
                                        mSaveListTask.execute();
                                        getLists();
                                    }
                                }
                            })
                            .show();
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

    public class SaveListTask extends NetworkCaller {

        private final List mList;
        private final String mUsername;

        SaveListTask(List list, String username) {
            mList = list;
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                saveList(mUsername, mList);
                return true;
            } catch (IOException e) {
                Log.d("IOException", "Something bad happened on the IO");
            } catch (JSONException e) {
                Log.d("JSONException", "The JSON fails");
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSaveListTask = null;

            if (success) {
                // Write lists to cache
                try {
                    CacheHandler.writeList(MainActivity.mContext, mList);
                } catch (IOException e) {
                    Log.d("IO", "Something bad with the IO: " + e);
                } catch (JSONException e) {
                    Log.d("JSON", "Something bad with the JSON: " + e);
                }
                Snackbar.make(mCoordinatorLayout, R.string.restored, Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mSaveListTask = null;
        }
    }
}
