package com.woording.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.woording.android.App;
import com.woording.android.EditTextListAdapter;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.VolleySingleton;
import com.woording.android.account.AccountUtils;
import com.woording.android.account.AuthPreferences;
import com.woording.android.activity.LoginActivity;
import com.woording.android.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditListFragment extends Fragment {

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String authToken;

    public boolean isModifiedSinceLastSave = false;

    private EditTextListAdapter mEditTextListAdapter;

    // UI Elements
    private Spinner mLanguage1Spinner;
    private Spinner mLanguage2Spinner;
    private EditText mListName;

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
            // TODO load in list
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.languages, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mLanguage1Spinner.setAdapter(adapter);
        mLanguage2Spinner.setAdapter(adapter);

        // Setup RecyclerView
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.edit_words_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEditTextListAdapter = new EditTextListAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(mEditTextListAdapter);

        mListName = (EditText) rootView.findViewById(R.id.list_title);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        authToken = null;
        mAuthPreferences = new AuthPreferences(getActivity());
        mAccountManager = AccountManager.get(getActivity());

        // Ask for an auth token
        mAccountManager.getAuthTokenByFeatures(AccountUtils.ACCOUNT_TYPE, AccountUtils.AUTH_TOKEN_TYPE,
                null, getActivity(), null, null, new GetAuthTokenCallback(0), null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && isModifiedSinceLastSave) {
            // TODO: 12-14-2015 Ask user to save or discard any changes made
        }

        return super.onOptionsItemSelected(item);
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
    }

    private String getLanguage1() {
        return getResources().getStringArray(R.array.language_codes)[mLanguage1Spinner.getSelectedItemPosition()];
    }

    private String getLanguage2() {
        return getResources().getStringArray(R.array.language_codes)[mLanguage2Spinner.getSelectedItemPosition()];
    }

    public List getListData() {
        List list = new List(mListName.getText().toString(),
                getLanguage1(), getLanguage2(), "0");
        list.setWords(mEditTextListAdapter.mLanguage1Words, mEditTextListAdapter.mLanguage2Words);
        return list;
    }

    public void saveList() {
        try {
            // Create data
            final JSONObject data = new JSONObject()
                    .put("username", mAuthPreferences.getAccountName())
                    .put("token", mAuthPreferences.getAuthToken())
                    .put("list_data", getListData().toJSON());
            // Create Volley request
            StringRequest request = new StringRequest(Request.Method.POST, App.API_LOCATION + "/savelist",
                    new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    isModifiedSinceLastSave = false;
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
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        private int taskToRun;

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
