/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.woording.android.CacheHandler;
import com.woording.android.List;
import com.woording.android.MySingleton;
import com.woording.android.NetworkCaller;
import com.woording.android.R;
import com.woording.android.TableListViewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ListViewActivity extends AppCompatActivity {

    public static final int NO_WORDS_DATA = 1;
    public static final int DELETED_LIST = 2;

    private List mList;

    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private TableListViewAdapter recyclerViewAdapter;

    public int askedLanguage = 1;
    public boolean caseSensitive = true;
    public boolean cancelled = false;
    protected final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressBar = (ProgressBar) findViewById(R.id.get_list_progress);
        mRecyclerView = (RecyclerView) findViewById(R.id.words_list);
        // Setup LinearLayoutManager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // Setup adapter
        recyclerViewAdapter = new TableListViewAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(recyclerViewAdapter);

        // Load List from Intent
        mList = (List) getIntent().getSerializableExtra("list");
        if (MainActivity.isNetworkAvailable(this)) getList();
        else {
            // Try to read from cache
            try {
                mList = CacheHandler.readList(this, mList.mName);
            } catch (IOException e) {
                Log.d("Cache", "Something went wrong with the IO: " + e);
            } catch (JSONException e) {
                Log.d("Cache", "Something went wrong with the JSON: " + e);
            }
            if (mList.getTotalWords() == 0) {
                finishActivity(NO_WORDS_DATA);
            }
            else setWordsTable();
        }




        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

       switch (item.getItemId()) {
           case R.id.action_practice:
                // Create custom AlertDialog
                View view = getLayoutInflater().inflate(R.layout.content_practice_options, null);
                ((TextView) view.findViewById(R.id.ask_language_1)).setText(List.getLanguageName(this, mList.mLanguage1));
                ((TextView) view.findViewById(R.id.ask_language_2)).setText(List.getLanguageName(this, mList.mLanguage2));
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog).setTitle(getString(R.string.practice_options))
                        .setCancelable(true).setView(view);
                // Set option buttons
                final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group_asked_language);
                final CheckBox checkBox = (CheckBox) view.findViewById(R.id.case_sensitive_check_box);
                // Setup start and cancel buttons
                builder.setPositiveButton(R.string.start_practice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get user inputs
                        switch (radioGroup.getCheckedRadioButtonId()) {
                            case R.id.ask_language_1:
                                askedLanguage = 1;
                                break;
                            case R.id.ask_language_2:
                                askedLanguage = 2;
                                break;
                            case R.id.ask_both:
                                askedLanguage = 0;
                                break;
                        }
                        caseSensitive = checkBox.isChecked();

                        // Create and launch new intent
                        Intent newIntent = new Intent(mContext, PracticeActivity.class);
                        newIntent.putExtra("list", mList);
                        newIntent.putExtra("askedLanguage", askedLanguage);
                        newIntent.putExtra("caseSensitive", caseSensitive);
                        startActivity(newIntent);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelled = true;
                        dialog.cancel();
                    }
                });
                // Create and show dialog
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return !cancelled;
           case R.id.action_delete:
               deleteList();
               break;
           case R.id.action_edit:
               // TODO
               break;
       }

        return super.onOptionsItemSelected(item);
    }

    private void setWordsTable() {
        // Set title and languages
        getSupportActionBar().setTitle(mList.mName);

        ((TextView) findViewById(R.id.head_1)).setText(List.getLanguageName(this, mList.mLanguage1));
        ((TextView) findViewById(R.id.head_2)).setText(List.getLanguageName(this, mList.mLanguage2));
        recyclerViewAdapter.addItems(mList.mLanguage1Words, mList.mLanguage2Words);
    }

    private void getList() {
        showProgress(true);

        try {
            JSONObject data = new JSONObject();
            data.put("token", NetworkCaller.mToken);
            // Create request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    NetworkCaller.API_LOCATION + "/" + MainActivity.username + "/" + mList.mName.replace(" ", "%20"), data,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Check for errors
                            try {
                                if (response.getString("username") != null) {
                                    MainActivity.openLoginActivity(MainActivity.mContext);
                                }
                            } catch (JSONException e) {
                                try {
                                    List list = new List(response.getString("listname"), response.getString("language_1_tag"),
                                            response.getString("language_2_tag"), response.getString("shared_with"));
                                    JSONArray JSONWords = response.getJSONArray("words");
                                    ArrayList<String> language1Words = new ArrayList<>();
                                    ArrayList<String> language2Words = new ArrayList<>();
                                    for (int i = 0; i < JSONWords.length(); i++) {
                                        JSONObject object = JSONWords.getJSONObject(i);
                                        language1Words.add(object.getString("language_1_text"));
                                        language2Words.add(object.getString("language_2_text"));
                                    }
                                    list.setWords(language1Words, language2Words);
                                    mList = list;
                                    // Display the list
                                    setWordsTable();
                                    // Write list to cache
                                    try {
                                        CacheHandler.writeList(mContext, mList);
                                    } catch (IOException exception) {
                                        Log.d("IO", "Something bad with the IO: " + e);
                                    } catch (JSONException exception) {
                                        Log.d("JSON", "Something bad with the JSON: " + e);
                                    }
                                } catch (JSONException ex) {
                                    Log.d("JSONException", "The JSON fails");
                                }
                            }
                            // Stop displaying loading screen
                            showProgress(false);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Log.e("VolleyError", error.toString());
                    // Stop displaying loading screen
                    showProgress(false);
                    // Try to read from cache
                    try {
                        mList = CacheHandler.readList(mContext, mList.mName);
                    } catch (IOException e) {
                        Log.d("Cache", "Something went wrong with the IO: " + e);
                    } catch (JSONException e) {
                        Log.d("Cache", "Something went wrong with the JSON: " + e);
                    }
                    if (mList.getTotalWords() == 0) {
                        finishActivity(NO_WORDS_DATA);
                    }
                    else setWordsTable();
                }
            });
            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(request);
        } catch (JSONException e) {
            Log.d("JSONException", "The JSON fails");
        }
    }

    private void deleteList() {
        try {
            final JSONObject data = new JSONObject()
                    .put("token", NetworkCaller.mToken)
                    .put("username", MainActivity.username)
                    .put("listname", mList.mName);
            // Create request
            StringRequest request = new StringRequest(Request.Method.POST, NetworkCaller.API_LOCATION + "/deleteList",
                    new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Delete list from cache
                    try {
                        CacheHandler.deleteList(mContext, mList.mName);
                    } catch (IOException e) {
                        Log.d("IO", "Something bad with the IO: " + e);
                    }
                    MainActivity.lastDeletedList = mList;
                    finishActivity(DELETED_LIST);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Log.e("VolleyError", error.toString());
                }
            }) {
                // This needs to be done to send data with a StringRequest
                // Get the data body
                @Override
                public byte[] getBody() throws AuthFailureError {
                    return data.toString().getBytes();
                }
                // Get the content type
                @Override
                public String getBodyContentType() {
                    return "application/json";
                }
            };
            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(request);
        } catch (JSONException e) {
            Log.d("JSONException", "The JSON fails");
        }
    }

    public void finishActivity(int requestCode) {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.putExtra("requestCode", requestCode);
        startActivity(upIntent);
        finish();
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        mRecyclerView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}
