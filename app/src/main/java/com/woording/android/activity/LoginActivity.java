/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.credentials.Credential;
import com.woording.android.R;
import com.woording.android.account.AccountUtils;
import com.woording.android.adapter.ViewPagerAdapter;
import com.woording.android.components.AccountAuthenticatorAppCompatActivity;
import com.woording.android.components.NoSwipeViewPager;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AccountAuthenticatorAppCompatActivity {

    private static final String TAG = "LoginActivity";

    public static final String ARG_ACCOUNT_TYPE = "accountType";
    public static final String ARG_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "isAddingNewAccount";
    public static final String ARG_IS_SIGNING_IN = "isSigningIn";
    public static final String PARAM_USER_PASSWORD = "password";

    private AccountManager mAccountManager;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private UserRegisterTask mRegisterTask = null;

    private boolean mLoggingIn = true;

    // UI references.
    private NoSwipeViewPager mPager;

    private TextInputEditText mUsernameViewLogin;
    private TextInputEditText mPasswordViewLogin;
    private TextInputEditText mUsernameViewRegister;
    private TextInputEditText mPasswordViewRegister;
    private TextInputEditText mRepeatPasswordView;
    private TextInputEditText mEmailView;

    private TextInputLayout mUsernameLayoutLogin;
    private TextInputLayout mPasswordLayoutLogin;
    private TextInputLayout mUsernameLayoutRegister;
    private TextInputLayout mPasswordLayoutRegister;
    private TextInputLayout mRepeatPasswordLayout;
    private TextInputLayout mEmailLayout;

    private View mLoginProgressView;
    private View mRegisterProgressView;
    private View mLoginFormView;
    private View mRegisterFormView;

//    private final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        else throw new RuntimeException("getSupportActionBar() should not be null");

        mAccountManager = AccountManager.get(this);

        // Setup ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter();
        mPager = (NoSwipeViewPager) findViewById(R.id.view_pager);
        if (mPager == null) throw new RuntimeException("pager should not be null");
        mPager.setAdapter(adapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: Changed page to " + position);
                mLoggingIn = position == 0;
                if (mToolbar != null) {
                    switch (position) {
                        case 0:
                            mToolbar.setTitle(R.string.action_sign_in);
                            break;
                        case 1:
                            mToolbar.setTitle(R.string.action_register);
                            break;
                    }
                } else throw new RuntimeException("mToolbar should not be null");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Set up the login form.
        mUsernameViewLogin = (TextInputEditText) findViewById(R.id.username);
        mUsernameViewRegister = (TextInputEditText) findViewById(R.id.username_register);

        mPasswordViewRegister = (TextInputEditText) findViewById(R.id.password_register);
        mPasswordViewLogin = (TextInputEditText) findViewById(R.id.password);
        mPasswordViewLogin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mRepeatPasswordView = (TextInputEditText) findViewById(R.id.password_repeat);

        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.register || id == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "onEditorAction: Attempt register from emailView");
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        if (signInButton == null) throw new RuntimeException("signButton should not be null");
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button registerButton = (Button) findViewById(R.id.register_button);
        if (registerButton == null) throw new RuntimeException("registerButton should not be null");
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Attempt register from button");
                attemptRegister();
            }
        });

        // Set TextInputLayouts
        mUsernameLayoutLogin = (TextInputLayout) findViewById(R.id.username_layout);
        mPasswordLayoutLogin = (TextInputLayout) findViewById(R.id.password_layout);
        mUsernameLayoutRegister = (TextInputLayout) findViewById(R.id.username_register_layout);
        mPasswordLayoutRegister = (TextInputLayout) findViewById(R.id.password_register_layout);
        mRepeatPasswordLayout = (TextInputLayout) findViewById(R.id.password_repeat_layout);
        mEmailLayout = (TextInputLayout) findViewById(R.id.email_layout);

        mLoginFormView = findViewById(R.id.login_form);
        mRegisterFormView = findViewById(R.id.register_form);
        mLoginProgressView = findViewById(R.id.login_progress);
        mRegisterProgressView = findViewById(R.id.register_progress);

        createPageLinks();

        if (getIntent().getBooleanExtra(ARG_IS_SIGNING_IN, false)) {
            Intent intent = getIntent();
            mUsernameViewLogin.setText(intent.getStringExtra("username"));
            mPasswordViewLogin.setText(intent.getStringExtra("password"));
            attemptLogin();
        }
    }

    private void createPageLinks() {
        TextView noAccount = (TextView) findViewById(R.id.no_account);
        String noAccountText = getString(R.string.no_account, getString(R.string.action_register));
        SpannableString noAccountString = new SpannableString(noAccountText);
        int registerLength = getString(R.string.action_register).length();
        ClickableSpan noAccountClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                mPager.setCurrentItem(1);
            }
        };

        noAccountString.setSpan(
                noAccountClickableSpan,
                noAccountText.length() - registerLength,
                noAccountText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        noAccountString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent)),
                noAccountText.length() - registerLength,
                noAccountText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        noAccountString.setSpan(
                new UnderlineSpan(),
                noAccountText.length() - registerLength,
                noAccountText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (noAccount != null) {
            noAccount.setText(noAccountString);
            noAccount.setMovementMethod(LinkMovementMethod.getInstance());
            noAccount.setHighlightColor(Color.TRANSPARENT);
        } else throw new RuntimeException("noAccount should not be null");

        TextView registered = (TextView) findViewById(R.id.registered);
        String registeredText = getString(R.string.already_registered, getString(R.string.action_sign_in));
        SpannableString registeredString = new SpannableString(registeredText);
        int signInLength = getString(R.string.action_sign_in).length();
        ClickableSpan registeredClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                mPager.setCurrentItem(0);
            }
        };

        registeredString.setSpan(
                registeredClickableSpan,
                registeredText.length() - signInLength,
                registeredText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        registeredString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent)),
                registeredText.length() - signInLength,
                registeredText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        registeredString.setSpan(
                new UnderlineSpan(),
                registeredText.length() - signInLength,
                registeredText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (registered != null) {
            registered.setText(registeredString);
            registered.setMovementMethod(LinkMovementMethod.getInstance());
            registered.setHighlightColor(Color.TRANSPARENT);
        } else throw new RuntimeException("registered should not be null");
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameLayoutLogin.setError(null);
        mPasswordLayoutLogin.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameViewLogin.getText().toString();
        String password = mPasswordViewLogin.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordLayoutLogin.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordViewLogin;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameLayoutLogin.setError(getString(R.string.error_field_required));
            focusView = mUsernameViewLogin;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        // Reset errors
        mPasswordLayoutRegister.setError(null);
        mPasswordLayoutRegister.setError(null);
        mRepeatPasswordLayout.setError(null);
        mEmailLayout.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameViewRegister.getText().toString();
        String password = mPasswordViewRegister.getText().toString();
        String repeatPassword = mRepeatPasswordView.getText().toString();
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for valid email
        if (TextUtils.isEmpty(email)) {
            mEmailLayout.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailLayout.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordLayoutRegister.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordViewRegister;
            cancel = true;
        }

        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(repeatPassword) && !password.equals(repeatPassword)) {
            mRepeatPasswordLayout.setError(getString(R.string.error_invalid_repeat_password));
            focusView = mRepeatPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameLayoutRegister.setError(getString(R.string.error_field_required));
            focusView = mUsernameViewRegister;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mRegisterTask = new UserRegisterTask(this, username, password, email);
            mRegisterTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        // TODO: Replace this with better logic
        return password.length() > 0;
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        final int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        final View view = mLoggingIn ? mLoginFormView : mRegisterFormView;
        final View progressView = mLoggingIn ? mLoginProgressView : mRegisterProgressView;

        view.setVisibility(show ? View.GONE : View.VISIBLE);
        view.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        mPager.setSwipeable(!show);
    }


    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Intent> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Intent doInBackground(Void... params) {
            String authToken = AccountUtils.mServerAuthenticator.signIn(mUsername, mPassword);

            final Intent res = new Intent();
            res.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountUtils.ACCOUNT_TYPE);
            res.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
            res.putExtra(PARAM_USER_PASSWORD, mPassword);
            return res;
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            mAuthTask = null;
            showProgress(false);

            if (intent.getStringExtra(AccountManager.KEY_AUTHTOKEN) == null) {
                mUsernameLayoutLogin.setError(getString(R.string.error_incorrect_password_or_username));
                mPasswordLayoutLogin.setError(getString(R.string.error_incorrect_password_or_username));
                mPasswordViewLogin.requestFocus();
            } else {
                finishLogin(intent);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private void finishLogin(Intent intent) {
            final String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            final String accountPassword = intent.getStringExtra(PARAM_USER_PASSWORD);
            final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
            String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false) || getIntent().getBooleanExtra(ARG_IS_SIGNING_IN, false)) {
                // Creating the account on the device and setting the auth token we got
                // (Not setting the auth token will cause another call to the server to authenticate the user)
                mAccountManager.addAccountExplicitly(account, accountPassword, null);
                mAccountManager.setAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, authToken);

                MainActivity.sAccountAdded = true;
            } else {
                mAccountManager.setPassword(account, accountPassword);
            }
            MainActivity.sCredentialToSave = new Credential.Builder(accountName)
                    .setName(accountName)
                    .setPassword(accountPassword)
                    .build();

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(AccountAuthenticatorActivity.RESULT_OK, intent);

            finish();
        }
    }

    public class UserRegisterTask extends AsyncTask<Void, Void, String> {
        private final Context context;

        private final String username;
        private final String password;
        private final String email;

        public UserRegisterTask(Context context, String username, String password, String email) {
            this.context = context;
            this.username = username;
            this.password = password;
            this.email = email;
        }

        @Override
        protected String doInBackground(Void... params) {
            return AccountUtils.mServerAuthenticator.signUp(email, username, password);
        }

        @Override
        protected void onPostExecute(final String success) {
            mRegisterTask = null;
            showProgress(false);

            if (success == null) {
                // Username or email does already exist
                mUsernameLayoutRegister.setError(getString(R.string.error_used_username_email));
                mEmailLayout.setError(getString(R.string.error_used_username_email));
                mUsernameLayoutRegister.requestFocus();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setMessage(R.string.register_success)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPager.setCurrentItem(0);
                            }
                        });
                builder.create().show();
            }
        }

        @Override
        protected void onCancelled() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}

