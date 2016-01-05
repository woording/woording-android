/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.woording.android.App;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.VolleySingleton;
import com.woording.android.account.AccountUtils;
import com.woording.android.account.AuthPreferences;
import com.woording.android.activity.LoginActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.adapter.ListsViewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


public class ListsListFragment extends Fragment {

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String authToken;

    public static String currentUsername;

    private List[] mLists = new List[]{};

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ListsViewAdapter mAdapter;

    public ListsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_lists_list, container, false);

        // Setup the recyclerView
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.lists_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new ListsViewAdapter(new ArrayList<>(Arrays.asList(new List[0])));
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getLists(false);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        authToken = null;
        mAuthPreferences = new AuthPreferences(getActivity());
        mAccountManager = AccountManager.get(getActivity());

        currentUsername = mAuthPreferences.getAccountName();

        Account account = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[App.selectedAccount];
        // Ask for an auth token
        mAccountManager.getAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, null, getActivity(), new GetAuthTokenCallback(0), null);
//        mAccountManager.getAuthTokenByFeatures(AccountUtils.ACCOUNT_TYPE, AccountUtils.AUTH_TOKEN_TYPE,
//                null, this, null, null, new GetAuthTokenCallback(0), null);
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity.lastDeletedList = null;
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
    }

    public void changeUser(String username) {
        currentUsername = username;
        getLists(true);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (!username.equals(mAuthPreferences.getAccountName())) {
            actionBar.setTitle(getString(R.string.others_lists, username));
        } else actionBar.setTitle(R.string.my_lists);
    }

    public void getLists(final boolean useCached) {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            // Create the data that is sent
            JSONObject data = new JSONObject();
            data.put("token", mAuthPreferences.getAuthToken());
            final String username = currentUsername != null ? currentUsername : mAuthPreferences.getAccountName();
            // Create the request
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.POST, App.API_LOCATION + "/" + username,
                            data, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
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
                                mAdapter.updateList(mLists);
                            } catch (JSONException e) {
                                Log.d("JSONException", "The JSON fails");
                            }
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse networkResponse = error.networkResponse;
                            if (networkResponse != null && networkResponse.statusCode == 401) {
                                // HTTP Status Code: 401 Unauthorized
                                if (useCached) getNewAuthToken(0);
                                else getNewAuthToken(2);
                            } else {
                                error.printStackTrace();
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
            if (!useCached) jsObjRequest.setShouldCache(false);
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(jsObjRequest);
        } catch (JSONException e) {
            mSwipeRefreshLayout.setRefreshing(false);
            e.printStackTrace();
        }
    }

    public void saveList(final List list) {
        try {
            JSONObject data = new JSONObject();
            data.put("token", mAuthPreferences.getAuthToken());
            data.put("username", mAuthPreferences.getAccountName());
            data.put("list_data", list.toJSON());

            // Create the request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, App.API_LOCATION + "/savelist",
                    data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Snackbar.make(MainActivity.mCoordinatorLayout, R.string.restored, Snackbar.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.statusCode == 401) {
                        // HTTP Status Code: 401 Unauthorized
                        getNewAuthToken(1);
                    } else {
                        error.printStackTrace();
                    }
                }
            });
            request.setShouldCache(false);
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        private final int taskToRun;

        public GetAuthTokenCallback(int taskToRun) {
            this.taskToRun = taskToRun;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;

            try {
                bundle = result.getResult();

                final Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (null != intent) {
                    startActivityForResult(intent, MainActivity.REQ_SIGNUP);
                } else {
                    authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    final String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);

                    // Save session username & auth token
                    mAuthPreferences.setAuthToken(authToken);
                    mAuthPreferences.setUsername(accountName);
                    // Run task
                    switch (taskToRun) {
                        case 0:
                            getLists(true);
                            break;
                        case 1:
                            saveList(MainActivity.lastDeletedList);
                            break;
                        case 2:
                            getLists(false);
                            break;
                    }

                    // If the logged account didn't exist, we need to create it on the device
                    Account account = AccountUtils.getAccount(getActivity(), accountName);
                    if (null == account) {
                        account = new Account(accountName, AccountUtils.ACCOUNT_TYPE);
                        mAccountManager.addAccountExplicitly(account, bundle.getString(LoginActivity.PARAM_USER_PASSWORD), null);
                        mAccountManager.setAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, authToken);
                    }
                }
            } catch(OperationCanceledException e) {
                // If signup was cancelled, force activity termination
                getActivity().finish();
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

    }
}
