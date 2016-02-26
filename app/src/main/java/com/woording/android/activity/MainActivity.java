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
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
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
import com.woording.android.fragment.ListViewFragment;
import com.woording.android.fragment.ListsListFragment;
import com.woording.android.util.LetterTileDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQ_SIGNUP = 1;

    // MaterialDrawer identifier
    private static final int PROFILE_SETTING = 1;

    public static List lastDeletedList = null;
    public static boolean accountAdded = false;

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

    private String setSelectionFor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_SHORT)
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
                                        mListsListFragment.getLists(true);
                                    }
                                }
                            }).show();
                    break;

                case ListViewActivity.LIST_NOT_FOUND:
                    Snackbar.make(mCoordinatorLayout, R.string.error_list_not_found, Snackbar.LENGTH_SHORT).show();
                    break;
                case ListViewActivity.USER_NOT_FOUND:
                    Snackbar.make(mCoordinatorLayout, R.string.error_user_not_found, Snackbar.LENGTH_SHORT).show();
                    break;
            }
        }

        if (!isNetworkAvailable(this))
            Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();

        authToken = null;
        mAuthPreferences = new AuthPreferences(this);
        mAccountManager = AccountManager.get(this);

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
            mAccountManager.getAuthToken(currentAccount, AccountUtils.AUTH_TOKEN_TYPE, null, this, new GetAuthTokenCallback(0), null);
//        mAccountManager.getAuthTokenByFeatures(AccountUtils.ACCOUNT_TYPE, AccountUtils.AUTH_TOKEN_TYPE,
//                null, this, null, null, new GetAuthTokenCallback(0), null);
        } else {
            // Add new account
            Intent addAccountIntent = new Intent(this, LoginActivity.class);
            addAccountIntent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
            startActivity(addAccountIntent);
        }

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
                        if (profile instanceof ProfileSettingDrawerItem && profile.getIdentifier() == PROFILE_SETTING) {
                            Intent addAccountIntent = new Intent(mContext, LoginActivity.class);
                            addAccountIntent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
                            startActivity(addAccountIntent);
                            return true;
                        } else if (profile instanceof  ProfileDrawerItem) {
                            // Save selected position
                            int position = -1;
                            for (int i = 0; i < headerResult.getProfiles().size(); i++) {
                                if (headerResult.getProfiles().get(i) == profile) {
                                    position = i;
                                    break;
                                }
                            }

                            if (position > -1) {
                                Account account = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[position];
                                // Ask for an auth token
                                mAccountManager.getAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE,
                                        null, (Activity) mContext, new GetAuthTokenCallback(2), null);
                            }
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        addAccounts();

        // Setup material navigation drawer
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.my_lists).withIcon(GoogleMaterial.Icon.gmd_list),
                        new SectionDrawerItem().withName(R.string.friends),
                        new SecondaryDrawerItem().withName(R.string.add_friend).withIcon(GoogleMaterial.Icon.gmd_add)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem instanceof SecondaryDrawerItem && position == drawer.getDrawerItems().size()) {
                            // Inflate AlertDialog layout
                            View layoutView = getLayoutInflater().inflate(R.layout.content_friend_request_dialog, null);
                            final EditText friendNameInput = (EditText) layoutView.findViewById(R.id.friend_request_input);
                            // First create dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppTheme_AlertDialog)
                                    .setTitle(R.string.send_request_title).setView(layoutView);

                            builder.setPositiveButton(R.string.send_request, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendFriendRequest(friendNameInput.getText().toString());
                                    // Hide keyboard
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(friendNameInput.getWindowToken(), 0);
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hide keyboard
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(friendNameInput.getWindowToken(), 0);
                                }
                            });

                            // Show Dialog
                            final AlertDialog dialog = builder.create();
                            // Set action on pressing enter
                            friendNameInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                @Override
                                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                    if (actionId == EditorInfo.IME_ACTION_GO) {
                                        // Send request
                                        sendFriendRequest(v.getText().toString());
                                        // Hide keyboard
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(friendNameInput.getWindowToken(), 0);
                                        // Dismiss dialog
                                        dialog.dismiss();
                                    }
                                    return false;
                                }
                            });
                            // Show dialog
                            dialog.show();
                            // Show input
                            friendNameInput.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        } else if (drawerItem instanceof SecondaryDrawerItem) {
                            String friendName = ((SecondaryDrawerItem) drawerItem).getName().getText();
                            Log.d(TAG, "onItemClick: Go to the list of " + friendName);
                            gotoUser(friendName);
                        } else if (position == 1) {
                            Log.d(TAG, "onItemClick: Go to own list");
                            gotoUser(mAuthPreferences.getAccountName());
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        getSupportActionBar().setTitle(R.string.my_lists);
        getFriends(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check for extras in the Intent
        String gotoUsername = getIntent().getStringExtra("username");
        if (gotoUsername != null) {
            // Load user
            gotoUser(gotoUsername);
        }

        if (App.mDualPane && getIntent().getStringExtra("listname") != null) {
            // Load list
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.second_pane);
            if (fragment instanceof ListViewFragment) {
                ((ListViewFragment) fragment).setList(new List(getIntent().getStringExtra("listname"), "", "", ""));
            } else {
                // Switch to fragment
                ListViewFragment listViewFragment = ListViewFragment.newInstance(
                        new List(getIntent().getStringExtra("listname"), "", "", ""), ListsListFragment.currentUsername);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.second_pane, listViewFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null).commit();
                // Change the FAB
                MainActivity.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white_24dp));
                MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        newList();
                    }
                });
            }
        } else if (App.mDualPane && getIntent().getSerializableExtra("list") != null) {
            // Load list
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.second_pane);
            if (fragment instanceof ListViewFragment) {
                ((ListViewFragment) fragment).setList((List) getIntent().getSerializableExtra("list"));
            } else {
                // Switch to fragment
                ListViewFragment listViewFragment = ListViewFragment.newInstance(
                        (List) getIntent().getSerializableExtra("list"), ListsListFragment.currentUsername);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.second_pane, listViewFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null).commit();
                // Change the FAB
                MainActivity.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white_24dp));
                MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        newList();
                    }
                });
            }
        }
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
    
    @Override
    public void onResume() {
        super.onResume();

        if (accountAdded) {
            // Switch to new account
            Account[] accounts = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE);
            Account addedAccount = accounts[accounts.length - 1];
            // Ask for an auth token
            mAccountManager.getAuthToken(addedAccount, AccountUtils.AUTH_TOKEN_TYPE,
                    null, this, new GetAuthTokenCallback(2), null);

            removeAccounts();
            addAccounts();
        }
    }

    private void removeAccounts() {
        for (int i = headerResult.getProfiles().size() - 2; i > -1; i--) {
            headerResult.removeProfile(i);
        }
    }

    private void addAccounts() {
        // Add every account found
        for (int i = 0; i < mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE).length; i++) {
            String userName = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[i].name;
            LetterTileDrawable icon = new LetterTileDrawable(this);
            icon.setContactDetails(userName, userName);
            headerResult.addProfile(
                    new ProfileDrawerItem().withName(userName).withIcon(icon), headerResult.getProfiles().size() - 1
            );

            if (userName.equals(mAuthPreferences.getAccountName())) headerResult.setActiveProfile(i);
        }

        if (headerResult.getActiveProfile() instanceof ProfileSettingDrawerItem) headerResult.setActiveProfile(0);
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void changeUser(String username) {
        mListsListFragment.changeUser(username);
        getFriends(true);

        removeFragmentsFromSecondPane();
    }

    public void gotoUser(String username) {
        if (!mListsListFragment.getCurrentUsername().equals(username)) {
            // Change user
            mListsListFragment.changeUser(username);
            removeFragmentsFromSecondPane();

            /*
             * Default selection is at 'My Lists' --> Position 1
             * So only check when this is selected and the username is not your username
             */
            if (drawer.getCurrentSelectedPosition() == 1 && !username.equals(mAuthPreferences.getAccountName())) {
                int selectPosition = -1;
                if (drawer.getDrawerItems().size() > 3) selectPosition = getFriendPosition(username);
                else setSelectionFor = username;

                // Select nothing if friends are not loaded
                drawer.setSelectionAtPosition(selectPosition, false);
            }
        }
    }

    public void removeFragmentsFromSecondPane() {
        if (App.mDualPane) {
            // Remove fragment on second pane
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.second_pane);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(fragment).commit();
        }
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
            fab.setImageResource(R.drawable.ic_save_white_24dp);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.saveList();
                }
            });
        }
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
    }

    private void removeFriendsFromDrawer() {
        for (int i = drawer.getDrawerItems().size() - 1; i > 2; i--) {
            drawer.removeItemByPosition(i);
        }
    }

    private int getFriendPosition(String username) {
        ArrayList<IDrawerItem> friends = new ArrayList<>(drawer.getDrawerItems());
        int friendPosition = -1; // Select nothing by default
        // remove all non-friend items
        friends.remove(friends.size() - 1);
        friends.remove(1);
        friends.remove(0);

        for (int i = 0; i < friends.size(); i++) {
            IDrawerItem currentFriend = friends.get(i);
            if (currentFriend instanceof BaseDrawerItem
                    && username.equals(((BaseDrawerItem) currentFriend).getName().getText())) {
                friendPosition = i + 3;
                break;
            }
        }

        return friendPosition;
    }

    /**
     * This function helps you getting friends from our API server. ;)
     */
    private void getFriends(final boolean skipCache) {
        try {
            final JSONObject data = new JSONObject()
                    .put("username", mAuthPreferences.getAccountName())
                    .put("token", mAuthPreferences.getAuthToken());
            // Send the request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, App.API_LOCATION + "/getFriends",
                    data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Remove all friend items when needed
                    if (drawer.getDrawerItems().size() > 3) removeFriendsFromDrawer();
                    // Try to load data
                    try {
                        JSONArray array = response.getJSONArray("friends");
                        for (int i = 0; i < array.length(); i++) {
                            try {
                                JSONObject friend = array.getJSONObject(i);
                                drawer.addItemAtPosition(new SecondaryDrawerItem()
                                        .withName(friend.getString("username"))
                                        .withIcon(GoogleMaterial.Icon.gmd_person),
                                        drawer.getDrawerItems().size());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Check if need to set selection
                    if (setSelectionFor != null) {
                        drawer.setSelectionAtPosition(getFriendPosition(setSelectionFor), false);
                        setSelectionFor = null;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.statusCode == 401) {
                        // HTTP Status Code: 401 Unauthorized
                        getNewAuthToken(skipCache ? 3 : 1);
                    } else {
                        error.printStackTrace();
                    }
                }
            });
            request.setSkipCache(skipCache);
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(this).addToRequestQueue(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendFriendRequest(final String friendName) {
        try {
            final JSONObject data = new JSONObject()
                    .put("username", mAuthPreferences.getAccountName())
                    .put("friendname", friendName);
            // Create Volley request
            StringRequest request = new StringRequest(Request.Method.POST, App.API_LOCATION + "/friendRequest",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (response.contains("ERROR")) {
                                Snackbar.make(mCoordinatorLayout, R.string.error_already_friends, Snackbar.LENGTH_SHORT).show();
                            } else {
                                Snackbar.make(mCoordinatorLayout, R.string.friend_request_sent, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
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
                    mAuthPreferences.setAuthToken(authToken);
                    mAuthPreferences.setUsername(accountName);
                    // Run task
                    switch (taskToRun) {
                        case 1:
                            getFriends(false);
                            break;
                        case 2:
                            changeUser(accountName);
                            break;
                        case 3:
                            getFriends(true);
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
