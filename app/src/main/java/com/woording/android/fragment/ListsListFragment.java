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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.woording.android.components.MyFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


public class ListsListFragment extends MyFragment implements SearchView.OnQueryTextListener {

    // Account stuff
    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String mAuthToken;

    public static String sCurrentUsername;

    private List[] mLists = new List[]{};

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ListsViewAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Menu mMenu;

    public ListsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_lists_list, container, false);

        // Setup the recyclerView
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.lists_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new ListsViewAdapter(getActivity(), new ArrayList<>(Arrays.asList(new List[0])));
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getLists(true);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAuthToken = null;
        mAuthPreferences = new AuthPreferences(getActivity());
        mAccountManager = AccountManager.get(getActivity());

        sCurrentUsername = mAuthPreferences.getAccountName();

        Account[] accounts = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE);
        Account currentAccount = null;
        if (accounts.length != 0) {
            String username = mAuthPreferences.getAccountName();
            for (Account account : accounts) {
                if (account.name.equals(username)) currentAccount = account;
            }
        }
        if (currentAccount != null) {
            // Ask for an auth token
            mAccountManager.getAuthToken(currentAccount, AccountUtils.AUTH_TOKEN_TYPE, null,
                    getActivity(), new GetAuthTokenCallback(0), null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity.sLastDeletedList = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getActivity().getMenuInflater().inflate(R.menu.menu_lists_view, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
    }

    public String getCurrentUsername() {
        return sCurrentUsername;
    }

    /**
     * Change the current user
     * @param username The username of the user you want to go to
     */
    public void changeUser(String username) {
        sCurrentUsername = username;
        mAdapter.clearList();
        getLists();

        // Collapse searchView
        if (mMenu != null) {
            MenuItemCompat.collapseActionView(mMenu.findItem(R.id.action_search));
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            if (!username.equals(mAuthPreferences.getAccountName())) {
                if (username.endsWith("s")) actionBar.setTitle(getString(R.string.others_list_alt, username));
                else actionBar.setTitle(getString(R.string.others_lists, username));
            } else actionBar.setTitle(R.string.my_lists);
        } else throw new RuntimeException("actionBar should not be null");
    }

    public void getLists() {
        getLists(false);
    }

    public void getLists(final boolean skipCache) {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            // Create the data that is sent
            JSONObject data = new JSONObject();
            data.put("token", mAuthPreferences.getAuthToken());
            final String username = sCurrentUsername != null ? sCurrentUsername : mAuthPreferences.getAccountName();
            // Create the request
            JsonObjectRequest request = new JsonObjectRequest
                    (Request.Method.POST, App.API_LOCATION + "/" + username,
                            data, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                // Check for errors
                                if (response.has("error")) {
                                    if (response.getString("error").equals("User not found")) {
                                        // If from deep link, close the app and send a Toast
                                        if (getActivity().getIntent().getBooleanExtra("fromDeepLink", false)) {
                                            // First display toast
                                            Toast.makeText(getActivity(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                                            // Then finish the app
                                            finishApp();
                                        } else {
                                            // Show Snackbar
                                            Snackbar.make(
                                                    MainActivity.mCoordinatorLayout, R.string.error_user_not_found, Snackbar.LENGTH_SHORT
                                            ).show();
                                            // Go to own lists
                                            ((MainActivity) getActivity()).gotoUser(mAuthPreferences.getAccountName());
                                        }
                                    }
                                } else {
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
                                    if (mAdapter.getItemCount() != 0) {
                                        mAdapter.updateList(mLists);
                                    } else mAdapter.setList(mLists);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse networkResponse = error.networkResponse;
                            if (networkResponse != null && networkResponse.statusCode == 401) {
                                // HTTP Status Code: 401 Unauthorized
                                getNewAuthToken(skipCache ? 2 : 0);
                            } else if (networkResponse != null && networkResponse.statusCode >= 500 && networkResponse.statusCode <= 599) {
                                // Server error
                                Snackbar.make(MainActivity.mCoordinatorLayout, R.string.error_server, Snackbar.LENGTH_SHORT).show();
                            } else {
                                error.printStackTrace();
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
            request.setSkipCache(skipCache);
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(request);
        } catch (JSONException e) {
            mSwipeRefreshLayout.setRefreshing(false);
            e.printStackTrace();
        }
    }

    public void saveList(final List list) {
        try {
            final JSONObject data = new JSONObject()
                    .put("token", mAuthPreferences.getAuthToken())
                    .put("username", mAuthPreferences.getAccountName())
                    .put("list_data", list.toJSON());
            // Create Volley request
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
                    mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    final String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);

                    // Save session username & auth token
                    mAuthPreferences.setAuthToken(mAuthToken);
                    mAuthPreferences.setUsername(accountName);
                    // Run task
                    switch (taskToRun) {
                        case 0:
                            getLists();
                            break;
                        case 1:
                            saveList(MainActivity.sLastDeletedList);
                            break;
                        case 2:
                            getLists(true);
                            break;
                    }

                    // If the logged account didn't exist, we need to create it on the device
                    Account account = AccountUtils.getAccount(getActivity(), accountName);
                    if (null == account) {
                        account = new Account(accountName, AccountUtils.ACCOUNT_TYPE);
                        mAccountManager.addAccountExplicitly(account, bundle.getString(LoginActivity.PARAM_USER_PASSWORD), null);
                        mAccountManager.setAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, mAuthToken);
                    }
                }
            } catch(OperationCanceledException e) {
                // If sign up was cancelled, force activity termination
                getActivity().finish();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * OnQueryTextListener stuff
    * To filter the lists
    */
    @Override
    public boolean onQueryTextChange(String query) {
        // Here is where we are going to implement our filter logic
        mRecyclerView.scrollToPosition(0);
        mAdapter.getFilter().filter(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}
