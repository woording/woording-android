package com.woording.android.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.woording.android.App;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.TableListViewAdapter;
import com.woording.android.VolleySingleton;
import com.woording.android.account.AccountUtils;
import com.woording.android.account.AuthPreferences;
import com.woording.android.activity.ListViewActivity;
import com.woording.android.activity.LoginActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.activity.PracticeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ListViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ListViewFragment extends Fragment {

    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private List mList;

    private AccountManager mAccountManager;
    private AuthPreferences mAuthPreferences;
    private String authToken;

    private TableListViewAdapter recyclerViewAdapter;

    public int askedLanguage = 1;
    public boolean caseSensitive = true;
    public boolean cancelled = false;

//    private OnFragmentInteractionListener mListener;

    public ListViewFragment() {
        // Required empty public constructor
    }

    public static ListViewFragment newInstance(List list) {
        ListViewFragment f = new ListViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("list", list);
        f.setArguments(args);
        return f;
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
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
        authToken = null;
        mAuthPreferences = new AuthPreferences(getActivity());
        mAccountManager = AccountManager.get(getActivity());

        // Ask for an auth token
        mAccountManager.getAuthTokenByFeatures(AccountUtils.ACCOUNT_TYPE, AccountUtils.AUTH_TOKEN_TYPE,
                null, getActivity(), null, null, new GetAuthTokenCallback(0), null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_list_view, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_practice:
                // Create custom AlertDialog
                View view = getActivity().getLayoutInflater().inflate(R.layout.content_practice_options, null);
                ((TextView) view.findViewById(R.id.ask_language_1)).setText(List.getLanguageName(getActivity(), mList.mLanguage1));
                ((TextView) view.findViewById(R.id.ask_language_2)).setText(List.getLanguageName(getActivity(), mList.mLanguage2));
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog).setTitle(getString(R.string.practice_options))
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
                        Intent newIntent = new Intent(getActivity(), PracticeActivity.class);
                        newIntent.putExtra("list", mList);
                        newIntent.putExtra("askedLanguage", askedLanguage);
                        newIntent.putExtra("caseSensitive", caseSensitive);
                        startActivity(newIntent);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    public void setList(List list) {
        mList = list;
    }

    private void setWordsTable() {
        // Set title and languages
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mList.mName);

        ((TextView) getActivity().findViewById(R.id.head_1)).setText(List.getLanguageName(getActivity(), mList.mLanguage1));
        ((TextView) getActivity().findViewById(R.id.head_2)).setText(List.getLanguageName(getActivity(), mList.mLanguage2));
        recyclerViewAdapter.addItems(mList.mLanguage1Words, mList.mLanguage2Words);
    }

    private void getNewAuthToken(int taskToRun) {
        // Invalidate the old token
        mAccountManager.invalidateAuthToken(AccountUtils.ACCOUNT_TYPE, mAuthPreferences.getAuthToken());
        // Now get a new one
        mAccountManager.getAuthToken(mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE)[0],
                AccountUtils.AUTH_TOKEN_TYPE, null, false, new GetAuthTokenCallback(taskToRun), null);
    }

    public void showProgress(final boolean show) {
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
            // Create request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    App.API_LOCATION + "/" + mAuthPreferences.getAccountName() + "/" + mList.mName.replace(" ", "%20"),
                    data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Check for errors
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
                    } catch (JSONException ex) {
                        Log.d("JSONException", "The JSON fails");
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
                    .put("listname", mList.mName);
            // Create request
            StringRequest request = new StringRequest(Request.Method.POST, App.API_LOCATION + "/deleteList",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            MainActivity.lastDeletedList = mList;
                            getActivity().finishActivity(ListViewActivity.DELETED_LIST);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

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
