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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import com.woording.android.activity.EditListActivity;
import com.woording.android.activity.ListViewActivity;
import com.woording.android.activity.LoginActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.activity.PracticeActivity;
import com.woording.android.adapter.TableListViewAdapter;
import com.woording.android.components.MyFragment;
import com.woording.android.util.ConvertLanguage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ListViewFragment extends MyFragment {

    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private List mList;

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String mAuthToken;

    private TableListViewAdapter recyclerViewAdapter;

    private boolean mCancelled = false;
    private String mUsername = null;

    public AlertDialog mDialog = null;

    public ListViewFragment() {
        // Required empty public constructor
    }

    public static ListViewFragment newInstance(List list, String username) {
        ListViewFragment f = new ListViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("list", list);
        args.putString("username", username);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mList = (List) getArguments().getSerializable("list");
            mUsername = getArguments().getString("username", null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_list_view, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.get_list_progress);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.words_list);
        // Setup LinearLayoutManager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // Setup adapter
        recyclerViewAdapter = new TableListViewAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(recyclerViewAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAuthToken = null;
        mAuthPreferences = new AuthPreferences(getActivity());
        mAccountManager = AccountManager.get(getActivity());

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
            if (mUsername == null) {
                mAccountManager.getAuthToken(currentAccount, AccountUtils.AUTH_TOKEN_TYPE, null,
                        getActivity(), new GetAuthTokenCallback(-1), null);
            } else {
                mAccountManager.getAuthToken(currentAccount, AccountUtils.AUTH_TOKEN_TYPE, null,
                        getActivity(), new GetAuthTokenCallback(0), null);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_list_view, menu);

        if (mList.getSharedWith().equals("1") || mList.getSharedWith().equals("2")) {
            menu.findItem(R.id.action_share).setVisible(true);
        }
        // Remove delete list button when not own list
        if (mUsername != null && !mUsername.equals(mAuthPreferences.getAccountName())) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                ((ListViewActivity) getActivity()).goUp(mUsername);
                return true;
            case R.id.action_practice:
                // Create custom AlertDialog
                View view = getActivity().getLayoutInflater().inflate(R.layout.content_practice_options, null);
                ((TextView) view.findViewById(R.id.ask_language_1)).setText(ConvertLanguage.toLang(mList.getLanguage1()));
                ((TextView) view.findViewById(R.id.ask_language_2)).setText(ConvertLanguage.toLang(mList.getLanguage2()));
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog)
                        .setTitle(getString(R.string.practice_options)).setView(view);
                // Set option buttons
                final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group_asked_language);
                final CheckBox checkBox1 = (CheckBox) view.findViewById(R.id.case_sensitive_check_box);
                final CheckBox checkBox2 = (CheckBox) view.findViewById(R.id.ignore_accents_check_box);
                // Setup start and cancel buttons
                builder.setPositiveButton(R.string.start_practice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PracticeActivity.AskedLanguage askedLanguage = PracticeActivity.AskedLanguage.LANGUAGE_1;
                        // Get user inputs
                        switch (radioGroup.getCheckedRadioButtonId()) {
                            case R.id.ask_language_1:
                                askedLanguage = PracticeActivity.AskedLanguage.LANGUAGE_1;
                                break;
                            case R.id.ask_language_2:
                                askedLanguage = PracticeActivity.AskedLanguage.LANGUAGE_2;
                                break;
                            case R.id.ask_both:
                                askedLanguage = PracticeActivity.AskedLanguage.BOTH;
                                break;
                        }
                        boolean caseSensitive = checkBox1.isChecked();
                        boolean ignoreAccents = checkBox2.isChecked();

                        // Create and launch new intent
                        Intent newIntent = new Intent(getActivity(), PracticeActivity.class);
                        newIntent.putExtra("list", mList);
                        newIntent.putExtra("username", mUsername);
                        newIntent.putExtra("askedLanguage", askedLanguage);
                        newIntent.putExtra("caseSensitive", caseSensitive);
                        newIntent.putExtra("ignoreAccents", ignoreAccents);
                        startActivity(newIntent);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCancelled = true;
                        dialog.cancel();
                    }
                });
                // Create and show dialog
                mDialog = builder.create();
                mDialog.show();

                return !mCancelled;
            case R.id.action_delete:
                deleteList();
                break;
            case R.id.action_edit:
                if (!App.mDualPane) {
                    if (mUsername == null) mUsername = mAuthPreferences.getAccountName();
                    Intent intent = new Intent(getActivity(), EditListActivity.class)
                            .putExtra("list", mList)
                            .putExtra("username", mUsername);
                    startActivity(intent);
                } else {
                    // Load on second pane
                    final EditListFragment fragment = EditListFragment.newInstance(mList);
                    FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.second_pane, fragment)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null).commit();
                    // Change the FAB
                    MainActivity.sFab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save_white_24dp));
                    MainActivity.sFab.setContentDescription(getString(R.string.content_desc_save_list));
                    MainActivity.sFab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fragment.saveList();
                        }
                    });
                }
                break;
            case R.id.action_share:
                if (mList.getSharedWith().equals("1")) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog);
                    alertDialogBuilder.setMessage(R.string.share_text_dialog);
                    alertDialogBuilder.setCancelable(true);
                    alertDialogBuilder.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shareList();
                            dialog.dismiss();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    mDialog = alertDialogBuilder.create();
                    mDialog.show();
                } else shareList();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setList(List list) {
        mList = list;
        if (mList != null && mUsername != null) getList();
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    @Nullable
    public String getUsername() {
        return mUsername;
    }

    private void shareList() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain")
                    .setText(getString(
                            R.string.share_text, mList.getName(), mUsername, mList.getName().replace(" ", "%20")))
                    .getIntent();
            if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(shareIntent);
            }
        } else {
            // Use Android M+ Share dialog which has direct share.
            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(
                            Intent.EXTRA_TEXT,
                            getString(R.string.share_text, mList.getName(), mUsername, mList.getName().replace(" ", "%20"))
                    );
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title, mList.getName())));
        }
    }

    private void setWordsTable() {
        // Set title and languages
        if (!App.mDualPane) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.setTitle(mList.getName());
            else throw new RuntimeException("getSupportActionBar() should not be null");
        }

        ((TextView) getActivity().findViewById(R.id.head_1)).setText(List.getLanguageName(getActivity(), mList.getLanguage1()));
        ((TextView) getActivity().findViewById(R.id.head_2)).setText(List.getLanguageName(getActivity(), mList.getLanguage2()));
        recyclerViewAdapter.setItems(mList.getLanguage1Words(), mList.getLanguage2Words());
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
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

    private void getList() {
        showProgress(true);

        try {
            JSONObject data = new JSONObject();
            data.put("token", mAuthPreferences.getAuthToken());
            if (mUsername == null) mUsername = mAuthPreferences.getAccountName();
            // Create request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    App.API_LOCATION + "/" + mUsername + "/" + mList.getName().replace(" ", "%20"),
                    data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Check for errors
                    try {
                        // Check for errors
                        if (response.has("error")) {
                            String error = response.getString("error");
                            switch (error) {
                                case "List not found":
                                    // First check for deep linking
                                    if (getActivity().getIntent().getBooleanExtra("fromDeepLink", false)) {
                                        // First display toast
                                        Toast.makeText(getActivity(), R.string.error_list_not_found, Toast.LENGTH_SHORT).show();
                                        // Then finish the app
                                        finishApp();
                                    } else {
                                        if (!App.mDualPane) {
                                            // Finish and go back to MainActivity
                                            ((ListViewActivity) getActivity()).goUp(ListViewActivity.LIST_NOT_FOUND, mUsername);
                                        } else {
                                            Snackbar.make(
                                                    MainActivity.mCoordinatorLayout, R.string.error_list_not_found, Snackbar.LENGTH_SHORT
                                            ).show();
                                            // Remove from pane
                                            ((MainActivity) getActivity()).removeFragmentsFromSecondPane();
                                        }
                                    }
                                    break;
                                case "User not found":
                                    // First check for deep linking
                                    if (getActivity().getIntent().getBooleanExtra("fromDeepLink", false)) {
                                        // First display toast
                                        Toast.makeText(getActivity(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                                        // Then finish the app
                                        finishApp();
                                    } else {
                                        if (!App.mDualPane) {
                                            // Finish and go back to MainActivity
                                            ((ListViewActivity) getActivity()).goUp(ListViewActivity.USER_NOT_FOUND,
                                                    mAuthPreferences.getAccountName()); // Can't go back to that user, so go to own account
                                        } else {
                                            Snackbar.make(
                                                    MainActivity.mCoordinatorLayout, R.string.error_user_not_found, Snackbar.LENGTH_SHORT
                                            ).show();
                                            // Go to own lists
                                            ((MainActivity) getActivity()).gotoUser(mAuthPreferences.getAccountName());
                                        }
                                    }
                                    break;
                            }
                        } else {
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
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                    // Stop displaying loading screen
                    showProgress(false);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.statusCode == 401) {
                        // HTTP Status Code: 401 Unauthorized
                        getNewAuthToken(0);
                    } else if (networkResponse != null
                            && networkResponse.statusCode >= 500 && networkResponse.statusCode <= 599) {
                        // Some kind of server error
                        if (getActivity().getIntent().getBooleanExtra("fromDeepLink", false)) {
                            // First display toast
                            Toast.makeText(getActivity(), R.string.error_server, Toast.LENGTH_SHORT).show();
                            // Then finish the app
                            finishApp();
                        } else {
                            if (!App.mDualPane) {
                                // Finish and go back to MainActivity
                                ((ListViewActivity) getActivity()).goUp(ListViewActivity.SERVER_ERROR, mUsername);
                            } else {
                                Snackbar.make(
                                        MainActivity.mCoordinatorLayout, R.string.error_server, Snackbar.LENGTH_SHORT
                                ).show();
                                // Remove from pane
                                ((MainActivity) getActivity()).removeFragmentsFromSecondPane();
                            }
                        }
                    } else {
                        error.printStackTrace();
                        // Stop displaying loading screen
                        showProgress(false);
                    }
                }
            });
            // Access the RequestQueue through your singleton class.
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteList() {
        try {
            final JSONObject data = new JSONObject()
                    .put("token", mAuthPreferences.getAuthToken())
                    .put("username", mAuthPreferences.getAccountName())
                    .put("listname", mList.getName());
            // Create request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, App.API_LOCATION + "/deleteList",
                    data, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            MainActivity.sLastDeletedList = mList;
                            ((ListViewActivity) getActivity()).goUp(ListViewActivity.DELETED_LIST, mUsername);
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
                            getList();
                            break;
                        case 1:
                            deleteList();
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
                // If signup was cancelled, force activity termination
                getActivity().finish();
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

    }
}
