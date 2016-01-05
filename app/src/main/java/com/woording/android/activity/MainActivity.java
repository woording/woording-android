/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.woording.android.App;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.VolleySingleton;
import com.woording.android.account.AccountUtils;
import com.woording.android.account.AuthPreferences;
import com.woording.android.fragment.EditListFragment;
import com.woording.android.fragment.ListsListFragment;
import com.woording.android.util.CustomJsonArrayRequest;
import com.woording.android.util.LetterTileDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQ_SIGNUP = 1;
    private static final int PROFILE_SETTING = 1;

    public static List lastDeletedList = null;

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String authToken;

    public static CoordinatorLayout mCoordinatorLayout;
    public static FloatingActionButton fab;
    private ListsListFragment mListsListFragment;

    private AccountHeader headerResult;
    private Drawer drawer;

    public static Context mContext;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App.mDualPane = getResources().getBoolean(R.bool.is_dual_pane);

        // Setup toolbar
        Toolbar mToolbar;
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mListsListFragment = (ListsListFragment) getSupportFragmentManager().findFragmentById(R.id.lists_view_fragment);

        // Setup Floating Action Button
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newList();
            }
        });

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.root_view);

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
                                        mListsListFragment.saveList(lastDeletedList);
                                        mListsListFragment.getLists(false);
                                    }
                                }
                            }).show();
                    break;
            }
        }

        if (!isNetworkAvailable(this)) Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();

        authToken = null;
        mAuthPreferences = new AuthPreferences(this);
        mAccountManager = AccountManager.get(this);

        App.selectedAccount = mAuthPreferences.getSelectedAccount();

        Account account = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[App.selectedAccount];
        // Ask for an auth token
        mAccountManager.getAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, null, this, new GetAuthTokenCallback(0), null);
//        mAccountManager.getAuthTokenByFeatures(AccountUtils.ACCOUNT_TYPE, AccountUtils.AUTH_TOKEN_TYPE,
//                null, this, null, null, new GetAuthTokenCallback(0), null);

        // Build the accountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .addProfiles(
                        new ProfileSettingDrawerItem().withName(getString(R.string.add_account))
                                .withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_add).actionBar()
                                        .paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(PROFILE_SETTING)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_SETTING) {
                            Intent addAccountIntent = new Intent(mContext, LoginActivity.class);
                            addAccountIntent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
                            startActivity(addAccountIntent);
                            return true;
                        } else if (profile instanceof  IDrawerItem) {
                            // TODO: 1-5-2016 Change user
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        // For every account found
        for (int i = 0; i < mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE).length; i++) {
            String userName = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[i].name;
            LetterTileDrawable icon = new LetterTileDrawable(this);
            icon.setContactDetails(userName, userName);
            headerResult.addProfile(new ProfileDrawerItem().withName(userName).withIcon(icon), headerResult.getProfiles().size() - 1);
        }
        headerResult.setActiveProfile(App.selectedAccount);

        // Setup material navigation drawer
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.my_lists).withIcon(GoogleMaterial.Icon.gmd_list),
                        new SectionDrawerItem().withName(R.string.friends)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        getSupportActionBar().setTitle(R.string.my_lists);
        getFriends();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            if (doubleBackToExitPressedOnce) {
                finish();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Snackbar.make(mCoordinatorLayout, R.string.press_BACK_again_to_exit, Snackbar.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void newList() {
        if (!App.mDualPane) {
            Intent intent = new Intent(mContext, EditListActivity.class);
            mContext.startActivity(intent);
        } else {
            final EditListFragment fragment = new EditListFragment();
            FragmentTransaction ft = ((AppCompatActivity) mContext).getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.second_pane, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null).commit();
            // Change the FAB
            fab.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_save_white_24dp));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.saveList();
                }
            });
        }
    }

    private void getNewAuthToken() {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken(App.selectedAccount));
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(1), null);
    }

    private void getFriends() {
        try {
            JSONObject data = new JSONObject()
                    .put("username", mAuthPreferences.getAccountName(App.selectedAccount))
                    .put("token", mAuthPreferences.getAuthToken(App.selectedAccount));
            // Send the request
            CustomJsonArrayRequest request = new CustomJsonArrayRequest(Request.Method.POST, App.API_LOCATION + "/getFriends",
                    data, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject friend = response.getJSONObject(i);
                            drawer.addItem(new SecondaryDrawerItem()
                                    .withName(friend.getString("username"))
                                    .withIcon(GoogleMaterial.Icon.gmd_person));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.statusCode == 401) {
                        // HTTP Status Code: 401 Unauthorized
                        getNewAuthToken();
                    } else {
                        error.printStackTrace();
                    }
                }
            });
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(this).addToRequestQueue(request);
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
                    mAuthPreferences.setAuthToken(authToken, App.selectedAccount);
                    mAuthPreferences.setUsername(accountName, App.selectedAccount);
                    // Run task
                    switch (taskToRun) {
                        case 1:
                            getFriends();
                            break;
                    }

                    // If the logged account didn't exist, we need to create it on the device
                    Account account = AccountUtils.getAccount(mContext, accountName);
                    if (null == account) {
                        account = new Account(accountName, AccountUtils.ACCOUNT_TYPE);
                        mAccountManager.addAccountExplicitly(account, bundle.getString(LoginActivity.PARAM_USER_PASSWORD), null);
                        mAccountManager.setAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, authToken);
                    }
                }
            } catch(OperationCanceledException e) {
                // If signup was cancelled, force activity termination
                finish();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
