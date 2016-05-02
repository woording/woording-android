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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

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
import com.woording.android.activity.LoginActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.adapter.EditTextListAdapter;
import com.woording.android.adapter.LanguageAdapter;
import com.woording.android.components.MyFragment;
import com.woording.android.util.ConvertLanguage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditListFragment extends MyFragment {

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String authToken;

    public boolean isModifiedSinceLastSave = false;
    public boolean isNewList = true;

    private EditTextListAdapter mEditTextListAdapter;
    private ArrayList<String> mListNames = new ArrayList<>();
    private List mList = null;
    private List lastSavedList = null;
    private boolean listNameExists = false;

    // UI Elements
    private Spinner mLanguage1Spinner;
    private Spinner mLanguage2Spinner;
    private Spinner mSharedWith;
    private EditText mListName;
    private TextInputLayout mListNameInputLayout;

    public EditListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param list the list to edit
     * @return A new instance of fragment EditListFragment.
     */
    public static EditListFragment newInstance(List list) {
        EditListFragment fragment = new EditListFragment();
        Bundle args = new Bundle();
        args.putSerializable("list", list);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mList = (List) getArguments().getSerializable("list");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_list, container, false);

        // Setup the Spinners
        mLanguage1Spinner = (Spinner) rootView.findViewById(R.id.spinner_language_1);
        mLanguage2Spinner = (Spinner) rootView.findViewById(R.id.spinner_language_2);

        // Create an ArrayAdapter using the string array and a default spinner layout
        LanguageAdapter  adapter = new LanguageAdapter(getActivity(), android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mLanguage1Spinner.setAdapter(adapter);
        mLanguage2Spinner.setAdapter(adapter);

        mSharedWith = (Spinner) rootView.findViewById(R.id.spinner_shared_with);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> sharedWithAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.shared_with, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        sharedWithAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSharedWith.setAdapter(sharedWithAdapter);

        // Setup RecyclerView
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.edit_words_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEditTextListAdapter = new EditTextListAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(mEditTextListAdapter);

        mListName = (EditText) rootView.findViewById(R.id.list_title);
        mListNameInputLayout = (TextInputLayout) rootView.findViewById(R.id.title_text_input_layout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        authToken = null;
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
            mAccountManager.getAuthToken(currentAccount, AccountUtils.AUTH_TOKEN_TYPE, null,
                    getActivity(), new GetAuthTokenCallback(0), null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mList != null) loadList(mList);

        // Setup characters filter
        mListName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().matches("^[0-9a-zA-Z\\-_\\s]*$")) {
                    mListNameInputLayout.setErrorEnabled(true);
                    mListNameInputLayout.setError(getString(R.string.error_invalid_chars));
                } else if (mListNameInputLayout.getError() != null) {
                    mListNameInputLayout.setErrorEnabled(false);
                    mListNameInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Check if list name exists
                checkIfListNameExists(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // First check if changes are made
            areChangesMade();
            if (isModifiedSinceLastSave && !isNewList) {
                // Build alertDialog
                createAlertDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // Go intent up
                        navigateUp();
                    }
                }).create().show();
                return true;
            } else if (isModifiedSinceLastSave) {
                createAlertDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // Navigate up
                        NavUtils.navigateUpFromSameTask(getActivity());
                    }
                }).create().show();
                return true;
            } else if (!isNewList) {
                navigateUp();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private int getElementThatContains(String[] ips, String key) {
        for (int i = 0; i < ips.length; i++) {
            if (ips[i].contains(key)) {
                return i;
            }
        }
        return -1;
    }

    public AlertDialog.Builder createAlertDialog(DialogInterface.OnClickListener negativeButtonOnClick,
                                                 DialogInterface.OnClickListener positiveButtonOnClick) {
        // Build alertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog)
                .setMessage(R.string.discard_dialog_text);
        // Add buttons
        builder.setNegativeButton(android.R.string.cancel, negativeButtonOnClick);
        builder.setPositiveButton(R.string.discard, positiveButtonOnClick);
        return builder;
    }

    public void navigateUp() {
        Intent upIntent = NavUtils.getParentActivityIntent(getActivity());
        upIntent.putExtra("list", mList)
                .putExtra("username", getActivity().getIntent().getStringExtra("username"));
        if (NavUtils.shouldUpRecreateTask(getActivity(), upIntent)) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(getActivity())
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(getActivity(), upIntent);
        }
    }

    public void loadList(List list) {
        mList = list.deepClone();
        lastSavedList = list.deepClone();
        isNewList = false;
        // Set list name
        mListName.setText(list.getName());
        // Set shared with
        mSharedWith.setSelection(Integer.parseInt(list.getSharedWith()));
        // Set languages
        ArrayList<Locale> locales = ((LanguageAdapter) mLanguage1Spinner.getAdapter()).getLocales();
        String[] languageCodes = new String[locales.size()];
        for (int i = 0; i < locales.size(); i++) {
            languageCodes[i] = ConvertLanguage.getISO3(locales.get(i));
        }
        mLanguage1Spinner.setSelection(getElementThatContains(languageCodes, list.getLanguage1()));
        mLanguage2Spinner.setSelection(getElementThatContains(languageCodes, list.getLanguage2()));
        // Set words
        mEditTextListAdapter.setWords(list.getLanguage1Words(), list.getLanguage2Words());
    }

    private void checkIfListNameExists(boolean forceCheck) {
        if (isNewList) {
            if (mListNames.size() != 0 || forceCheck) {
                String listName = mListName.getText().toString();
                if (mListNames.contains(listName)) {
                    mListNameInputLayout.setErrorEnabled(true);
                    mListNameInputLayout.setError(getString(R.string.error_list_name_exist));
                    listNameExists = true;
                } else {
                    mListNameInputLayout.setError(null);
                    mListNameInputLayout.setErrorEnabled(false);
                    listNameExists = false;
                }
            } else {
                getListNames();
            }
        }
    }

    private void getNewAuthToken(int task) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(task), null);
    }

    private String getLanguage1() {
        return mLanguage1Spinner.getSelectedItem().toString();
    }

    private String getLanguage2() {
        return mLanguage2Spinner.getSelectedItem().toString();
    }

    private List getListData() {
        // TODO: 3-15-2016 Check if list name already exists 
        
        List list = new List(mListName.getText().toString(),
                getLanguage1(), getLanguage2(), mSharedWith.getSelectedItemPosition() + "");
        list.setWords(mEditTextListAdapter.mLanguage1Words, mEditTextListAdapter.mLanguage2Words);
        return list;
    }

    public void areChangesMade() {
        mList = getListData().deepClone();
        if (lastSavedList == null) lastSavedList = new List("", "", "", "0");
        isModifiedSinceLastSave = !mList.equals(lastSavedList);
    }

    public void saveList() {
        if (!listNameExists || !isNewList) {
            try {
                // Create data
                final JSONObject data = new JSONObject()
                        .put("username", mAuthPreferences.getAccountName())
                        .put("token", mAuthPreferences.getAuthToken())
                        .put("list_data", getListData().toJSON());
                // Create Volley request
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, App.API_LOCATION + "/savelist",
                        data, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        isModifiedSinceLastSave = false;
                        lastSavedList = getListData().deepClone();

                        // Display SnackBar
                        CoordinatorLayout coordinatorLayout;
                        // Decide whether to load CoordinatorLayout from MainActivity or EditListActivity
                        coordinatorLayout = App.mDualPane ? MainActivity.mCoordinatorLayout : EditListActivity.mCoordinatorLayout;
                        // Show SnackBar
                        Snackbar.make(coordinatorLayout, R.string.list_saved, Snackbar.LENGTH_SHORT).show();
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
        } else {
            // List name does already exist so can't save
            // Display SnackBar
            CoordinatorLayout coordinatorLayout;
            // Decide whether to load CoordinatorLayout from MainActivity or EditListActivity
            coordinatorLayout = App.mDualPane ? MainActivity.mCoordinatorLayout : EditListActivity.mCoordinatorLayout;
            // Show SnackBar
            Snackbar.make(coordinatorLayout, R.string.error_cant_save_list_name_exits, Snackbar.LENGTH_LONG).show();
        }
    }

    private void getListNames() {
        try {
            // Create the data that is sent
            JSONObject data = new JSONObject()
                    .put("token", mAuthPreferences.getAuthToken());
            // Create the request
            JsonObjectRequest request = new JsonObjectRequest
                    (Request.Method.POST, App.API_LOCATION + "/" + mAuthPreferences.getAccountName(),
                            data, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                // Check for errors
                                if (!response.has("error")) {
                                    // TODO: 4-29-2016 Save list names
                                    JSONArray lists = response.getJSONArray("lists");
                                    JSONObject list;
                                    for (int i = 0; i < lists.length(); i++) {
                                        list = lists.getJSONObject(i);
                                        mListNames.add(list.getString("listname"));
                                    }
                                    // Check if list name exists
                                    checkIfListNameExists(true);
                                } else {
                                    // TODO: 4-29-2016 Handle error
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse networkResponse = error.networkResponse;
                            if (networkResponse != null && networkResponse.statusCode == 401) {
                                // HTTP Status Code: 401 Unauthorized
                                getNewAuthToken(2);
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
                    authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    final String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);

                    // Save session username & auth token
                    mAuthPreferences.setAuthToken(authToken);
                    mAuthPreferences.setUsername(accountName);
                    // Run task
                    switch (taskToRun) {
                        case 1:
                            saveList();
                            break;
                        case 2:
                            getListNames();
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
